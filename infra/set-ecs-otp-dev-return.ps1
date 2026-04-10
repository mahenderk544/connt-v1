# Register new ECS task definition revision with CONNTO_OTP_DEV_RETURN_CODE=true and roll the service.
# Requires: AWS CLI, Python 3 on PATH.
#
# Usage (from infra folder):
#   .\set-ecs-otp-dev-return.ps1
#   .\set-ecs-otp-dev-return.ps1 -Region eu-west-1 -TaskFamily connto-backend -Cluster my-cluster -Service my-svc

param(
    [string] $Region = "ap-south-1",
    [string] $TaskFamily = "connto-backend",
    [string] $Cluster = "connto-backend-app",
    [string] $Service = "connto-backend-svc"
)

$ErrorActionPreference = "Stop"
$scriptDir = $PSScriptRoot
$patchScript = Join-Path $scriptDir "patch-ecs-taskdef-otp-dev.py"
# Write next to script (avoids TEMP 8.3 short paths) — removed in finally
$tmp = Join-Path $scriptDir ("connto-ecs-td-{0}.json" -f [guid]::NewGuid().ToString("N"))

try {
    $describe = aws ecs describe-task-definition --task-definition $TaskFamily --region $Region --query taskDefinition --output json
    if ($LASTEXITCODE -ne 0) { throw "describe-task-definition failed" }

    $patched = $describe | python $patchScript
    if ($LASTEXITCODE -ne 0) { throw "Python patch failed" }

    [System.IO.File]::WriteAllText($tmp, $patched, [System.Text.UTF8Encoding]::new($false))

    # Windows AWS CLI: use file://C:/path (two slashes after file:), not file:///C:/...
    $fullPath = (Resolve-Path -LiteralPath $tmp).Path
    $fileUri = "file://" + ($fullPath.Replace("\", "/"))
    $newArn = aws ecs register-task-definition --cli-input-json $fileUri --region $Region --query taskDefinition.taskDefinitionArn --output text
    if ($LASTEXITCODE -ne 0) { throw "register-task-definition failed" }

    Write-Host "Registered: $newArn"

    aws ecs update-service --cluster $Cluster --service $Service --task-definition $newArn --force-new-deployment --region $Region | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "update-service failed" }

    Write-Host "Service $Service updated; new deployment started."
}
finally {
    if (Test-Path $tmp) { Remove-Item $tmp -Force -ErrorAction SilentlyContinue }
}
