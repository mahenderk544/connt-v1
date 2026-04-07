# Provision Connto backend on ECS Fargate: register task definition + create or update service.
# Requires: AWS CLI v2, logged in (aws sts get-caller-identity works).
#
# Fill the parameters below (or pass on the command line). Subnets and security groups must be in your VPC.
#
# Example:
#   cd infra
#   .\provision-ecs-fargate.ps1 `
#     -SubnetIds "subnet-aaa,subnet-bbb" `
#     -SecurityGroupIds "sg-ccc" `
#     -RdsEndpoint "connto-db.xxxxx.ap-south-1.rds.amazonaws.com" `
#     -RdsPasswordSecretArn "arn:aws:secretsmanager:ap-south-1:798335328181:secret:connto/rds-xxx" `
#     -JwtSecretArn "arn:aws:secretsmanager:ap-south-1:798335328181:secret:connto/jwt-xxx"
#
# Public IP: use -AssignPublicIp ENABLED if tasks are in public subnets (quick test). Use DISABLED with NAT/private + RDS.
#
# If you see "ecsTaskExecutionRole cannot be found", run first: .\create-ecs-task-execution-role.ps1

param(
    [string] $Region = "ap-south-1",
    [string] $ClusterName = "connto-backend-app",
    [string] $ServiceName = "connto-backend-svc",
    [string] $TaskFamily = "connto-backend",
    [string] $EcrRepositoryName = "connto-backend",
    [string] $ImageTag = "latest",
    [string] $ContainerName = "backend",
    [string] $ExecutionRoleName = "ecsTaskExecutionRole",
    [string] $TaskRoleName = "",
    [string] $Cpu = "512",
    [string] $Memory = "1024",
    [Parameter(Mandatory = $true)]
    [string] $SubnetIds,
    [Parameter(Mandatory = $true)]
    [string] $SecurityGroupIds,
    [Parameter(Mandatory = $true)]
    [string] $RdsEndpoint,
    [string] $DbName = "connto",
    [string] $DbUsername = "connto",
    [Parameter(Mandatory = $true)]
    [string] $RdsPasswordSecretArn,
    [Parameter(Mandatory = $true)]
    [string] $JwtSecretArn,
    [int] $DesiredCount = 1,
    [ValidateSet("ENABLED", "DISABLED")]
    [string] $AssignPublicIp = "ENABLED",
    [string] $LogGroup = "/ecs/connto-backend"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$templatePath = Join-Path $PSScriptRoot "ecs\fargate-task-definition.template.json"
if (-not (Test-Path $templatePath)) {
    throw "Template not found: $templatePath"
}

$account = (aws sts get-caller-identity --query Account --output text).Trim()
if (-not $account) { throw "AWS CLI not configured." }

$executionRoleArn = aws iam get-role --role-name $ExecutionRoleName --query Role.Arn --output text 2>$null
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($executionRoleArn)) {
    throw @"
IAM role not found: $ExecutionRoleName

Create the standard Fargate execution role, then run this script again:
  cd infra
  .\create-ecs-task-execution-role.ps1

Or use a different role name: -ExecutionRoleName YourRoleName
"@
}
$executionRoleArn = $executionRoleArn.Trim()

$image = "${account}.dkr.ecr.${Region}.amazonaws.com/${EcrRepositoryName}:${ImageTag}"
$jdbcUrl = "jdbc:postgresql://${RdsEndpoint}:5432/${DbName}?sslmode=require"

Write-Host "Account $account  Region $Region  Image $image" -ForegroundColor Cyan

$td = Get-Content -LiteralPath $templatePath -Raw -Encoding UTF8 | ConvertFrom-Json
$td.family = $TaskFamily
$td.cpu = $Cpu
$td.memory = $Memory
$td.executionRoleArn = $executionRoleArn
if ($TaskRoleName) {
    $taskRoleArn = aws iam get-role --role-name $TaskRoleName --query Role.Arn --output text 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($taskRoleArn)) { throw "IAM role not found: $TaskRoleName" }
    $taskRoleArn = $taskRoleArn.Trim()
    $td.taskRoleArn = $taskRoleArn
} else {
    $null = $td.PSObject.Properties.Remove("taskRoleArn")
}

$td.containerDefinitions[0].name = $ContainerName
$td.containerDefinitions[0].image = $image
$td.containerDefinitions[0].environment = @(
    @{ name = "SPRING_PROFILES_ACTIVE"; value = "prod" },
    @{ name = "SPRING_DATASOURCE_URL"; value = $jdbcUrl },
    @{ name = "SPRING_DATASOURCE_USERNAME"; value = $DbUsername }
)
$td.containerDefinitions[0].secrets = @(
    @{ name = "SPRING_DATASOURCE_PASSWORD"; valueFrom = $RdsPasswordSecretArn },
    @{ name = "CONNTO_JWT_SECRET"; valueFrom = $JwtSecretArn }
)
$td.containerDefinitions[0].logConfiguration.options."awslogs-group" = $LogGroup
$td.containerDefinitions[0].logConfiguration.options."awslogs-region" = $Region

# Windows AWS CLI does not support file://- (stdin); use a file under infra + resolved long path.
$tmp = Join-Path $PSScriptRoot (".connto-taskdef-" + [Guid]::NewGuid().ToString("n") + ".json")
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($tmp, ($td | ConvertTo-Json -Depth 20), $utf8NoBom)

try {
    Write-Host "Registering task definition family $TaskFamily ..."
    $fullPath = (Resolve-Path -LiteralPath $tmp).Path
    $fileUri = "file:///" + ($fullPath -replace "\\", "/")
    $taskDefArn = aws ecs register-task-definition --cli-input-json $fileUri --region $Region --query taskDefinition.taskDefinitionArn --output text
    if ($LASTEXITCODE -ne 0) { throw "register-task-definition failed" }
    Write-Host "Registered: $taskDefArn" -ForegroundColor Green
}
finally {
    Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
}

$subnets = ($SubnetIds.Split(",") | ForEach-Object { $_.Trim() }) -join ","
$sgs = ($SecurityGroupIds.Split(",") | ForEach-Object { $_.Trim() }) -join ","
$netCfg = "awsvpcConfiguration={subnets=[$subnets],securityGroups=[$sgs],assignPublicIp=$AssignPublicIp}"

$svcDesc = aws ecs describe-services --cluster $ClusterName --services $ServiceName --region $Region --output json | ConvertFrom-Json
$active = ($svcDesc.services.Count -gt 0 -and $svcDesc.services[0].status -eq "ACTIVE")
if ($active) {
    Write-Host "Updating existing service $ServiceName ..."
    aws ecs update-service `
        --cluster $ClusterName `
        --service $ServiceName `
        --task-definition $taskDefArn `
        --force-new-deployment `
        --region $Region | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "update-service failed" }
} else {
    Write-Host "Creating service $ServiceName ..."
    aws ecs create-service `
        --cluster $ClusterName `
        --service-name $ServiceName `
        --task-definition $taskDefArn `
        --desired-count $DesiredCount `
        --launch-type FARGATE `
        --network-configuration $netCfg `
        --region $Region | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "create-service failed" }
}

Write-Host ""
Write-Host "Done. Set these GitHub Actions Variables to match:" -ForegroundColor Yellow
Write-Host "  ECS_CLUSTER        = $ClusterName"
Write-Host "  ECS_SERVICE        = $ServiceName"
Write-Host "  ECS_TASK_DEFINITION = $TaskFamily"
Write-Host "  ECR_REPOSITORY     = $EcrRepositoryName"
Write-Host "  AWS_REGION         = $Region"
