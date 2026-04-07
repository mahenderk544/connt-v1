#Requires -Version 5.1
<#
.SYNOPSIS
  Creates AWS RDS PostgreSQL for Connto (subnet group, security group, DB instance).

.PREREQUISITES
  - AWS CLI v2 installed and configured:  aws configure   OR   aws login
  - IAM permissions: ec2:* (describe/create SG, subnets), rds:* (subnet group, create instance)
  - Default VPC must exist in the target region (many accounts have this).

.PARAMETER Region
  AWS region (default: ap-south-1 Mumbai).

.PARAMETER MasterPassword
  RDS master password (meets RDS complexity: upper, lower, number, 8+ chars).

.PARAMETER MyIpCidr
  Your public IP in CIDR form, e.g. 203.0.113.10/32. If omitted, fetched from checkip.amazonaws.com.

.EXAMPLE
  .\create-rds-connto.ps1 -MasterPassword 'YourStr0ng!Pass'
#>
param(
    [string] $Region = "ap-south-1",
    [string] $DbInstanceIdentifier = "connto-db",
    [string] $DbName = "connto",
    [string] $MasterUsername = "connto",
    [Parameter(Mandatory = $true)]
    [string] $MasterPassword,
    [string] $MyIpCidr = "",
    [string] $DbInstanceClass = "db.t4g.micro",
    [int] $AllocatedStorageGb = 20,
    [string] $EngineVersion = ""
)

$ErrorActionPreference = "Stop"

function Invoke-AwsCli {
    param([string[]] $CliArgs)
    $out = & aws @CliArgs 2>&1
    if ($LASTEXITCODE -ne 0) { throw "AWS CLI failed: $out" }
    return $out
}

Write-Host "Using region: $Region" -ForegroundColor Cyan
Invoke-AwsCli @("sts", "get-caller-identity", "--region", $Region) | Out-Null
Write-Host "AWS credentials OK." -ForegroundColor Green

if (-not $MyIpCidr) {
    $ip = (Invoke-RestMethod -Uri "https://checkip.amazonaws.com" -TimeoutSec 10).Trim()
    $MyIpCidr = "$ip/32"
    Write-Host "Using your current public IP for SG ingress: $MyIpCidr" -ForegroundColor Cyan
}

$vpcId = Invoke-AwsCli @("ec2", "describe-vpcs", "--region", $Region, "--filters", "Name=isDefault,Values=true", "--query", "Vpcs[0].VpcId", "--output", "text")
if (-not $vpcId -or $vpcId -eq "None") {
    throw "No default VPC in $Region. Create a VPC + subnets or use the RDS console."
}

$subnetJson = Invoke-AwsCli @(
    "ec2", "describe-subnets", "--region", $Region,
    "--filters", "Name=vpc-id,Values=$vpcId",
    "--query", "Subnets[*].[SubnetId,AvailabilityZone]", "--output", "json"
) | ConvertFrom-Json

$byAz = @{}
foreach ($row in $subnetJson) {
    $sid = $row[0]
    $az = $row[1]
    if (-not $byAz.ContainsKey($az)) { $byAz[$az] = $sid }
}
$uniqueAzs = @($byAz.Keys)
if ($uniqueAzs.Count -lt 2) {
    throw "Need at least 2 subnets in different AZs in default VPC; found AZs: $($uniqueAzs -join ', ')"
}
$subnetIds = @($byAz[$uniqueAzs[0]], $byAz[$uniqueAzs[1]])
Write-Host "Subnet group subnets: $($subnetIds -join ', ')" -ForegroundColor Cyan

$subnetGroupName = "connto-db-subnets"
aws rds describe-db-subnet-groups --db-subnet-group-name $subnetGroupName --region $Region 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Invoke-AwsCli @(
        "rds", "create-db-subnet-group",
        "--region", $Region,
        "--db-subnet-group-name", $subnetGroupName,
        "--db-subnet-group-description", "Connto app",
        "--subnet-ids", $subnetIds[0], $subnetIds[1]
    ) | Out-Null
    Write-Host "Created DB subnet group: $subnetGroupName" -ForegroundColor Green
} else {
    Write-Host "DB subnet group already exists: $subnetGroupName" -ForegroundColor Yellow
}

$sgName = "connto-rds-postgres-sg"
$sgId = aws ec2 describe-security-groups --region $Region --filters "Name=vpc-id,Values=$vpcId" "Name=group-name,Values=$sgName" --query "SecurityGroups[0].GroupId" --output text 2>$null
if (-not $sgId -or $sgId -eq "None") {
    $sgId = Invoke-AwsCli @(
        "ec2", "create-security-group",
        "--region", $Region,
        "--group-name", $sgName,
        "--description", "Connto RDS PostgreSQL",
        "--vpc-id", $vpcId,
        "--query", "GroupId", "--output", "text"
    )
    Write-Host "Created security group: $sgId" -ForegroundColor Green
} else {
    Write-Host "Reusing security group: $sgId" -ForegroundColor Yellow
}

# Add your IP to 5432 (duplicate rule returns error - ignored)
aws ec2 authorize-security-group-ingress --region $Region --group-id $sgId --protocol tcp --port 5432 --cidr $MyIpCidr 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Added inbound TCP 5432 from $MyIpCidr" -ForegroundColor Green
} else {
    Write-Host "Ingress rule may already exist (OK)." -ForegroundColor Yellow
}

$exists = aws rds describe-db-instances --db-instance-identifier $DbInstanceIdentifier --region $Region 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "RDS instance '$DbInstanceIdentifier' already exists. Showing endpoint:" -ForegroundColor Yellow
    Invoke-AwsCli @("rds", "describe-db-instances", "--region", $Region, "--db-instance-identifier", $DbInstanceIdentifier, "--query", "DBInstances[0].Endpoint.Address", "--output", "text")
    exit 0
}

Write-Host "Creating RDS instance (can take 10-20 minutes)..." -ForegroundColor Cyan
$createArgs = @(
    "rds", "create-db-instance",
    "--region", $Region,
    "--db-instance-identifier", $DbInstanceIdentifier,
    "--db-instance-class", $DbInstanceClass,
    "--engine", "postgres",
    "--master-username", $MasterUsername,
    "--master-user-password", $MasterPassword,
    "--allocated-storage", "$AllocatedStorageGb",
    "--db-name", $DbName,
    "--vpc-security-group-ids", $sgId,
    "--db-subnet-group-name", $subnetGroupName,
    "--backup-retention-period", "0",
    "--no-deletion-protection",
    "--no-storage-encrypted",
    "--publicly-accessible"
)
if ($EngineVersion) {
    $createArgs += @("--engine-version", $EngineVersion)
}
Invoke-AwsCli $createArgs | Out-Null

Write-Host "Waiting for instance to become available..." -ForegroundColor Cyan
aws rds wait db-instance-available --region $Region --db-instance-identifier $DbInstanceIdentifier

$endpoint = Invoke-AwsCli @(
    "rds", "describe-db-instances", "--region", $Region,
    "--db-instance-identifier", $DbInstanceIdentifier,
    "--query", "DBInstances[0].Endpoint.Address", "--output", "text"
)

Write-Host ""
Write-Host "=== RDS ready ===" -ForegroundColor Green
Write-Host "Endpoint: $endpoint"
Write-Host ""
Write-Host "PowerShell env for Connto backend:" -ForegroundColor Cyan
Write-Host ('$env:DB_HOST = "' + $endpoint + '"')
Write-Host '$env:DB_PORT = "5432"'
Write-Host ('$env:DB_NAME = "' + $DbName + '"')
Write-Host ('$env:DB_USER = "' + $MasterUsername + '"')
Write-Host ('$env:DB_PASSWORD = "{0}"' -f '<the password you passed to this script>')
Write-Host '$env:DB_JDBC_PARAMS = "?sslmode=require"'
Write-Host ('$env:JWT_SECRET = "{0}"' -f '<generate a long random string>')
