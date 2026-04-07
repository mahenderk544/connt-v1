# Create IAM role ecsTaskExecutionRole (Fargate pulls ECR, writes logs, reads Secrets Manager for task secrets).
# Idempotent: skips if role already exists.
# Requires: AWS CLI, iam:CreateRole, iam:AttachRolePolicy on your user.
#
# Usage:
#   cd infra
#   .\create-ecs-task-execution-role.ps1
#
# Then re-run .\provision-ecs-fargate.ps1

param(
    [string] $RoleName = "ecsTaskExecutionRole"
)

$ErrorActionPreference = "Stop"

$trust = @'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "Service": "ecs-tasks.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }
  ]
}
'@

$tmp = Join-Path $env:TEMP "ecs-trust-$([Guid]::NewGuid().ToString('n')).json"
$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($tmp, $trust.Trim(), $utf8)

try {
    aws iam get-role --role-name $RoleName --output json 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Role $RoleName already exists." -ForegroundColor Green
        aws iam get-role --role-name $RoleName --query Role.Arn --output text
        exit 0
    }

    Write-Host "Creating IAM role $RoleName ..."
    $trustUri = "file:///" + (($tmp -replace "\\", "/") -replace "^([A-Za-z]):/", '$1:/')
    aws iam create-role --role-name $RoleName --assume-role-policy-document $trustUri | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "create-role failed" }

    aws iam attach-role-policy `
        --role-name $RoleName `
        --policy-arn "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "attach-role-policy failed" }

    Write-Host "Attached AmazonECSTaskExecutionRolePolicy (ECR, logs, Secrets for task definitions)." -ForegroundColor Green
    Write-Host "Role ARN:" -ForegroundColor Cyan
    aws iam get-role --role-name $RoleName --query Role.Arn --output text
}
finally {
    Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
}
