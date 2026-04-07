# Attach AWS managed policy AmazonECSTaskExecutionRolePolicy (ECR auth + pull + CloudWatch Logs + Secrets).
# Use this if tasks fail with AccessDenied on ecr:GetAuthorizationToken.
# Safe to run multiple times (idempotent attach).
#
#   cd infra
#   .\ensure-ecs-execution-managed-policy.ps1

param([string] $RoleName = "ecsTaskExecutionRole")

$ErrorActionPreference = "Stop"
Write-Host "Attaching AmazonECSTaskExecutionRolePolicy to $RoleName ..." -ForegroundColor Cyan
aws iam attach-role-policy `
    --role-name $RoleName `
    --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
if ($LASTEXITCODE -ne 0) { throw "attach-role-policy failed (role missing or no permission?)" }
Write-Host "Done. Force new ECS deployment after ~10s." -ForegroundColor Green
