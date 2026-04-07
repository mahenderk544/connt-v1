# Create IAM role ecsTaskExecutionRole (Fargate pulls ECR, writes logs, reads Secrets Manager for task secrets).
# Idempotent: skips if role already exists.
#
# Trust policy is written to a short-lived file under this folder (UTF-8 no BOM) and passed with file://
# using the resolved long path — avoids PowerShell breaking inline JSON and avoids %TEMP% 8.3 paths.

param(
    [string] $RoleName = "ecsTaskExecutionRole"
)

$ErrorActionPreference = "Stop"

$trustPolicy = @'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
'@

$trustPolicy = $trustPolicy.Trim()

# IAM get-role writes NoSuchEntity to stderr when missing
$prevEa = $ErrorActionPreference
$ErrorActionPreference = "SilentlyContinue"
aws iam get-role --role-name $RoleName --output json *> $null
$getRoleExit = $LASTEXITCODE
$ErrorActionPreference = $prevEa

if ($getRoleExit -eq 0) {
    Write-Host "Role $RoleName already exists." -ForegroundColor Green
    aws iam get-role --role-name $RoleName --query Role.Arn --output text
    exit 0
}

$docFile = Join-Path $PSScriptRoot (".ecs-trust-policy-" + [Guid]::NewGuid().ToString("n") + ".json")
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

try {
    [System.IO.File]::WriteAllText($docFile, $trustPolicy, $utf8NoBom)
    $fullPath = (Resolve-Path -LiteralPath $docFile).Path
    $fileUri = "file:///" + ($fullPath -replace "\\", "/")

    Write-Host "Creating IAM role $RoleName ..."
    aws iam create-role --role-name $RoleName --assume-role-policy-document $fileUri | Out-Null
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
    Remove-Item -LiteralPath $docFile -Force -ErrorAction SilentlyContinue
}
