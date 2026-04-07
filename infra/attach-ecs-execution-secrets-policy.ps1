# Attach inline IAM policy so ecsTaskExecutionRole can read Secrets Manager values used in the task definition.
# Fixes: AccessDeniedException on secretsmanager:GetSecretValue for assumed-role/ecsTaskExecutionRole/...
#
# Usage (from infra):
#   .\attach-ecs-execution-secrets-policy.ps1
#
# Requires: iam:PutRolePolicy on your IAM user. Edit iam/ecs-execution-connto-secrets-policy.json if ARNs differ.

param(
    [string] $RoleName = "ecsTaskExecutionRole",
    [string] $PolicyName = "ConntoTaskSecretsRead"
)

$ErrorActionPreference = "Stop"
$policyPath = Join-Path $PSScriptRoot "iam\ecs-execution-connto-secrets-policy.json"
if (-not (Test-Path $policyPath)) { throw "Missing $policyPath" }

Write-Host "Attaching inline policy $PolicyName to role $RoleName ..." -ForegroundColor Cyan
$abs = (Resolve-Path -LiteralPath $policyPath).Path
$docUri = "file://" + ($abs -replace "\\", "/")
aws iam put-role-policy `
    --role-name $RoleName `
    --policy-name $PolicyName `
    --policy-document $docUri

if ($LASTEXITCODE -ne 0) { throw "put-role-policy failed" }

Write-Host "Done. Wait ~30s, then ECS will retry or use: cluster -> service -> Update -> Force new deployment." -ForegroundColor Green
