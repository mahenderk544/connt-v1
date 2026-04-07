param(
    [string] $Region = "ap-south-1",
    [string] $RepositoryName = "connto-backend",
    [string] $Cluster = "",
    [string] $Service = "",
    [string] $TaskDefinitionFamily = "",
    [string] $ContainerName = "backend",
    [switch] $SkipEcsUpdate
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot "push-backend-ecr.ps1") `
    -Region $Region `
    -RepositoryName $RepositoryName

$account = (aws sts get-caller-identity --query Account --output text).Trim()
$registry = "$account.dkr.ecr.$Region.amazonaws.com"

$tag = (git -C $root rev-parse --short HEAD 2>$null).Trim()
if (-not $tag) {
    $tag = "manual-$(Get-Date -Format 'yyyyMMddHHmm')"
}

$imageUri = "${registry}/${RepositoryName}:$tag"

if ($SkipEcsUpdate) {
    Write-Host "SkipEcsUpdate: pushed $imageUri — update ECS manually or via CI."
    exit 0
}

if (-not $Cluster -or -not $Service -or -not $TaskDefinitionFamily) {
    throw "Provide -Cluster, -Service, and -TaskDefinitionFamily, or use -SkipEcsUpdate."
}

Write-Host "Registering new task definition revision with image $imageUri ..."

$tdJson = aws ecs describe-task-definition `
    --task-definition $TaskDefinitionFamily `
    --region $Region `
    --query taskDefinition `
    --output json

if ($LASTEXITCODE -ne 0) {
    throw "describe-task-definition failed."
}

$td = $tdJson | ConvertFrom-Json

$remove = @(
    "taskDefinitionArn","revision","status","requiresAttributes",
    "compatibilities","registeredAt","registeredBy",
    "deregisteredAt","deregisteredBy"
)

foreach ($p in $remove) {
    $prop = $td.PSObject.Properties[$p]
    if ($prop) { $td.PSObject.Properties.Remove($p) }
}

$found = $false
foreach ($c in $td.containerDefinitions) {
    if ($c.name -eq $ContainerName) {
        $c.image = $imageUri
        $found = $true
        break
    }
}

if (-not $found) {
    Write-Warning "Container '$ContainerName' not found; updating first container."
    $td.containerDefinitions[0].image = $imageUri
}

$newJson = $td | ConvertTo-Json -Depth 100

$tmp = Join-Path $env:TEMP "ecs-td-$([Guid]::NewGuid().ToString('n')).json"

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($tmp, $newJson, $utf8NoBom)

try {
    $newArn = [System.IO.File]::ReadAllText($tmp) | aws ecs register-task-definition `
        --cli-input-json "file://-" `
        --region $Region `
        --query taskDefinition.taskDefinitionArn `
        --output text

    if ($LASTEXITCODE -ne 0) {
        throw "register-task-definition failed."
    }

    aws ecs update-service `
        --cluster $Cluster `
        --service $Service `
        --task-definition $newArn `
        --force-new-deployment `
        --region $Region | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "update-service failed."
    }

    Write-Host "Deployment successful"
    Write-Host "Task Definition: $newArn"
}
finally {
    Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
}
