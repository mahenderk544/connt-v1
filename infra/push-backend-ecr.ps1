# Push backend Docker image to Amazon ECR (one-off or before first ECS deploy).
# Requires: AWS CLI v2, Docker Desktop, and IAM permission for ecr:GetAuthorizationToken + ecr:* on the repo.
#
# Usage:
#   cd infra
#   .\push-backend-ecr.ps1 -Region ap-south-1 -RepositoryName connto-backend
#
# Then create/update your ECS task definition to use:
#   <account>.dkr.ecr.<region>.amazonaws.com/<RepositoryName>:latest
# Or tag with a Git SHA for immutable deploys (same as GitHub Actions).

param(
    [string] $Region = "ap-south-1",
    [string] $RepositoryName = "connto-backend"
)

$ErrorActionPreference = "Stop"

$account = (aws sts get-caller-identity --query Account --output text).Trim()
if (-not $account) { throw "AWS CLI not configured or no caller identity." }

$registry = "${account}.dkr.ecr.${Region}.amazonaws.com"
$uri = "${registry}/${RepositoryName}"

aws ecr describe-repositories --repository-names $RepositoryName --region $Region 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating ECR repository $RepositoryName..."
    aws ecr create-repository --repository-name $RepositoryName --region $Region | Out-Null
}

Write-Host "Logging in to $registry..."
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin $registry

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
if (-not (Test-Path (Join-Path $backend "Dockerfile"))) { throw "Dockerfile not found at $backend" }

$tag = git -C $root rev-parse --short HEAD 2>$null
if (-not $tag) { $tag = "manual-$(Get-Date -Format 'yyyyMMddHHmm')" }

Write-Host "Building from $backend..."
docker build -t "${uri}:latest" -t "${uri}:${tag}" $backend

docker push "${uri}:latest"
docker push "${uri}:${tag}"

Write-Host ""
Write-Host "Pushed: ${uri}:latest and ${uri}:${tag}"
Write-Host "ECS task image URI for this build: ${uri}:${tag}"
Write-Host "Full deploy (register task + update service): .\deploy-backend-ecs.ps1 ... (see README-ECS-DOCKER.txt)"
