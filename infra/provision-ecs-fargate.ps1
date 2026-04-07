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

function Escape-JsonString([string]$Value) {
    if ($null -eq $Value) { return "" }
    return $Value.Replace('\', '\\').Replace('"', '\"')
}

$taskRoleLine = ""
if ($TaskRoleName) {
    $taskRoleArn = aws iam get-role --role-name $TaskRoleName --query Role.Arn --output text 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($taskRoleArn)) { throw "IAM role not found: $TaskRoleName" }
    $taskRoleArn = $taskRoleArn.Trim()
    $taskRoleLine = "  ""taskRoleArn"": ""$(Escape-JsonString $taskRoleArn)"","
}

# PowerShell ConvertTo-Json breaks ECS API (wrong property names / shapes). Build JSON from template text.
$templateRaw = Get-Content -LiteralPath $templatePath -Raw -Encoding UTF8
$taskDefJson = $templateRaw.Replace("__TASK_FAMILY__", (Escape-JsonString $TaskFamily))
$taskDefJson = $taskDefJson.Replace("__CPU__", (Escape-JsonString $Cpu))
$taskDefJson = $taskDefJson.Replace("__MEMORY__", (Escape-JsonString $Memory))
$taskDefJson = $taskDefJson.Replace("__EXECUTION_ROLE_ARN__", (Escape-JsonString $executionRoleArn))
$taskDefJson = $taskDefJson.Replace("__TASK_ROLE_LINE__", $taskRoleLine)
$taskDefJson = $taskDefJson.Replace("__CONTAINER_NAME__", (Escape-JsonString $ContainerName))
$taskDefJson = $taskDefJson.Replace("__IMAGE_URI__", (Escape-JsonString $image))
$taskDefJson = $taskDefJson.Replace("__JDBC_URL__", (Escape-JsonString $jdbcUrl))
$taskDefJson = $taskDefJson.Replace("__DB_USERNAME__", (Escape-JsonString $DbUsername))
$taskDefJson = $taskDefJson.Replace("__RDS_PASSWORD_SECRET_ARN__", (Escape-JsonString $RdsPasswordSecretArn))
$taskDefJson = $taskDefJson.Replace("__JWT_SECRET_ARN__", (Escape-JsonString $JwtSecretArn))
$taskDefJson = $taskDefJson.Replace("__LOG_GROUP__", (Escape-JsonString $LogGroup))
$taskDefJson = $taskDefJson.Replace("__AWS_REGION__", (Escape-JsonString $Region))

try {
    $null = $taskDefJson | ConvertFrom-Json
} catch {
    throw "Built invalid task-definition JSON (template substitution). $_"
}

Write-Host "Registering task definition family $TaskFamily ..."
# Pass JSON as argv. Template + string replace avoids PowerShell ConvertTo-Json (invalid for ECS API).
$taskDefArn = & aws @(
    "ecs", "register-task-definition",
    "--cli-input-json", $taskDefJson,
    "--region", $Region,
    "--query", "taskDefinition.taskDefinitionArn",
    "--output", "text"
)
if ($LASTEXITCODE -ne 0) { throw "register-task-definition failed" }
Write-Host "Registered: $taskDefArn" -ForegroundColor Green

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
