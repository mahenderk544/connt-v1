# Attach an internet-facing Network Load Balancer with Elastic IPs to an existing ECS Fargate service.
# Fargate tasks cannot use Elastic IPs directly; NLB + EIP per subnet is the supported pattern.
#
# Prerequisites:
#   - Service exists (e.g. connto-backend-svc), awsvpc, container name/port match parameters below.
#   - Subnets: internet-facing NLB needs public subnets (route to IGW). Use one subnet per AZ you want.
#   - Task security group must allow TCP containerPort from the VPC CIDR (NLB -> task traffic). This script adds that rule.
#
# After success, use:  http://<ElasticIP>:<ListenerPort>/actuator/health
# (Default listener TCP 80 -> targets :8080; each AZ EIP answers on port 80.)
#
# Optional: -DisableTaskPublicIp $true only if tasks are in private subnets with NAT (otherwise ECR pull may fail).
#
# Usage:
#   cd infra
#   .\attach-nlb-elastic-ip-ecs.ps1 `
#     -SubnetIds "subnet-aaa,subnet-bbb" `
#     -TaskSecurityGroupId "sg-task"
#
# Idempotency: uses -NlbName / -TargetGroupName; delete NLB in console if you need to re-run with same names.

param(
    [string] $Region = "ap-south-1",
    [string] $ClusterName = "connto-backend-app",
    [string] $ServiceName = "connto-backend-svc",
    [string] $ContainerName = "backend",
    [int] $ContainerPort = 8080,
    [int] $ListenerPort = 80,
    [string] $NlbName = "connto-backend-nlb",
    [string] $TargetGroupName = "connto-backend-nlb-tg",
    [Parameter(Mandatory = $true)]
    [string] $SubnetIds,
    [Parameter(Mandatory = $true)]
    [string] $TaskSecurityGroupId,
    [switch] $DisableTaskPublicIp
)

$ErrorActionPreference = "Stop"

function Aws-Json([string]$JsonText) {
    $JsonText | ConvertFrom-Json
}

$subnetList = @($SubnetIds.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ })
if ($subnetList.Count -lt 1) { throw "Provide at least one -SubnetIds (public subnets recommended)." }

Write-Host "Describing subnets..." -ForegroundColor Cyan
$snJson = aws ec2 describe-subnets --subnet-ids $subnetList --region $Region --output json
$subs = (Aws-Json $snJson).Subnets
if ($subs.Count -ne $subnetList.Count) { throw "One or more subnet IDs were not found." }

$vpcId = $subs[0].VpcId
foreach ($s in $subs) {
    if ($s.VpcId -ne $vpcId) { throw "All subnets must be in the same VPC." }
}

$azSeen = @{}
foreach ($s in $subs) {
    if ($azSeen.ContainsKey($s.AvailabilityZone)) {
        throw "Use at most one subnet per Availability Zone for NLB subnet mappings (duplicate AZ: $($s.AvailabilityZone))."
    }
    $azSeen[$s.AvailabilityZone] = $true
}

$vpcJson = aws ec2 describe-vpcs --vpc-ids $vpcId --region $Region --output json
$cidr = (Aws-Json $vpcJson).Vpcs[0].CidrBlock
Write-Host "VPC $vpcId CIDR $cidr" -ForegroundColor DarkGray

# Allow NLB (in-VPC) -> task:8080
Write-Host "Ensuring task SG $TaskSecurityGroupId allows TCP $ContainerPort from $cidr ..." -ForegroundColor Cyan
$existing = aws ec2 describe-security-groups --group-ids $TaskSecurityGroupId --region $Region --output json
$sg = (Aws-Json $existing).SecurityGroups[0]
$hasRule = $false
foreach ($p in $sg.IpPermissions) {
    if ($p.IpProtocol -ne "tcp") { continue }
    if ($p.FromPort -ne $ContainerPort -or $p.ToPort -ne $ContainerPort) { continue }
    foreach ($r in $p.IpRanges) {
        if ($r.CidrIp -eq $cidr) { $hasRule = $true; break }
    }
}
if (-not $hasRule) {
    aws ec2 authorize-security-group-ingress `
        --group-id $TaskSecurityGroupId `
        --protocol tcp `
        --port $ContainerPort `
        --cidr $cidr `
        --region $Region | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "authorize-security-group-ingress failed" }
    Write-Host "Added inbound rule." -ForegroundColor Green
} else {
    Write-Host "Inbound rule already present." -ForegroundColor DarkGray
}

# Allocate one EIP per subnet mapping
$eipAllocs = @()
$i = 0
foreach ($sid in $subnetList) {
    $i++
    Write-Host "Allocating Elastic IP #$i for subnet $sid ..." -ForegroundColor Cyan
    $allocJson = aws ec2 allocate-address --domain vpc --region $Region --output json
    $allocId = (Aws-Json $allocJson).AllocationId
    $pub = (Aws-Json $allocJson).PublicIp
    Write-Host "  $pub  ($allocId)" -ForegroundColor Green
    $eipAllocs += [PSCustomObject]@{ SubnetId = $sid; AllocationId = $allocId; PublicIp = $pub }
}

# Build subnet-mappings for CLI (space-separated SubnetId=,AllocationId=)
$mapArgs = @()
foreach ($e in $eipAllocs) {
    $mapArgs += "SubnetId=$($e.SubnetId),AllocationId=$($e.AllocationId)"
}

Write-Host "Creating Network Load Balancer $NlbName ..." -ForegroundColor Cyan
# With Elastic IPs you must pass --subnet-mappings only (do not combine with --subnets).
$createNlbArgs = @(
    "elbv2", "create-load-balancer",
    "--name", $NlbName,
    "--type", "network",
    "--scheme", "internet-facing",
    "--region", $Region,
    "--output", "json"
)
foreach ($m in $mapArgs) {
    $createNlbArgs += "--subnet-mappings"
    $createNlbArgs += $m
}
$nlbOut = & aws @createNlbArgs
if ($LASTEXITCODE -ne 0) { throw "create-load-balancer failed (use AWS CLI v2; NLB + EIP requires subnet-mappings only)." }

$nlbArn = (Aws-Json $nlbOut).LoadBalancers[0].LoadBalancerArn
aws elbv2 wait load-balancer-available --load-balancer-arns $nlbArn --region $Region

# Without this, each EIP (per-AZ) only sends to targets in that AZ; a single Fargate task in one AZ
# would make the other AZ's Elastic IP time out. Cross-zone sends traffic to any healthy target.
Write-Host "Enabling cross-zone load balancing on NLB ..." -ForegroundColor Cyan
aws elbv2 modify-load-balancer-attributes `
    --load-balancer-arn $nlbArn `
    --region $Region `
    --attributes Key=load_balancing.cross_zone.enabled,Value=true | Out-Null
if ($LASTEXITCODE -ne 0) { throw "modify-load-balancer-attributes (cross-zone) failed" }

# create-load-balancer sometimes leaves an EIP unassociated (NLB shows fewer AZs); those IPs time out.
Write-Host "Verifying NLB has all subnet/EIP mappings ..." -ForegroundColor Cyan
$nlbDescJson = aws elbv2 describe-load-balancers --load-balancer-arns $nlbArn --region $Region --output json
$azCount = @((Aws-Json $nlbDescJson).LoadBalancers[0].AvailabilityZones).Count
if ($azCount -ne $subnetList.Count) {
    Write-Host "NLB reports $azCount AZ(s), expected $($subnetList.Count) - running set-subnets to attach orphaned EIPs ..." -ForegroundColor Yellow
    $setSubArgs = @("elbv2", "set-subnets", "--load-balancer-arn", $nlbArn, "--region", $Region)
    foreach ($m in $mapArgs) {
        $setSubArgs += "--subnet-mappings"
        $setSubArgs += $m
    }
    & aws @setSubArgs | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "set-subnets failed" }
    aws elbv2 wait load-balancer-available --load-balancer-arns $nlbArn --region $Region
    aws elbv2 modify-load-balancer-attributes `
        --load-balancer-arn $nlbArn `
        --region $Region `
        --attributes Key=load_balancing.cross_zone.enabled,Value=true | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "modify-load-balancer-attributes after set-subnets failed" }
}

Write-Host "Creating target group $TargetGroupName (IP targets, TCP $ContainerPort) ..." -ForegroundColor Cyan
$tgOut = aws elbv2 create-target-group `
    --name $TargetGroupName `
    --protocol TCP `
    --port $ContainerPort `
    --vpc-id $vpcId `
    --target-type ip `
    --health-check-enabled `
    --health-check-protocol HTTP `
    --health-check-path "/actuator/health" `
    --health-check-port "traffic-port" `
    --region $Region `
    --output json
if ($LASTEXITCODE -ne 0) { throw "create-target-group failed" }

$tgArn = (Aws-Json $tgOut).TargetGroups[0].TargetGroupArn

Write-Host "Creating listener TCP $ListenerPort -> target group ..." -ForegroundColor Cyan
aws elbv2 create-listener `
    --load-balancer-arn $nlbArn `
    --protocol TCP `
    --port $ListenerPort `
    --default-actions "Type=forward,TargetGroupArn=$tgArn" `
    --region $Region | Out-Null
if ($LASTEXITCODE -ne 0) { throw "create-listener failed" }

# ECS service: attach LB + optionally turn off task public IP
Write-Host "Reading service network configuration ..." -ForegroundColor Cyan
$svcJson = aws ecs describe-services --cluster $ClusterName --services $ServiceName --region $Region --output json
$svc = (Aws-Json $svcJson).services[0]
if (-not $svc) { throw "Service not found: $ServiceName in cluster $ClusterName" }

$awsVpc = $svc.networkConfiguration.awsvpcConfiguration
$subnetsStr = ($awsVpc.subnets -join ",")
$sgsStr = ($awsVpc.securityGroups -join ",")
$assign = if ($DisableTaskPublicIp) { "DISABLED" } else { $awsVpc.assignPublicIp }
if ($DisableTaskPublicIp) {
    Write-Host "Using assignPublicIp=DISABLED (ensure NAT for ECR/RDS if tasks are private)." -ForegroundColor Yellow
}
$netCfg = "awsvpcConfiguration={subnets=[$subnetsStr],securityGroups=[$sgsStr],assignPublicIp=$assign}"

$lbSpec = "targetGroupArn=$tgArn,containerName=$ContainerName,containerPort=$ContainerPort"
$existingLbs = $svc.loadBalancers
$updateSvcArgs = @(
    "ecs", "update-service",
    "--cluster", $ClusterName,
    "--service", $ServiceName,
    "--region", $Region,
    "--network-configuration", $netCfg,
    "--force-new-deployment"
)
if ($existingLbs -and $existingLbs.Count -gt 0) {
    Write-Host "Service already has load balancer(s). Replacing with full list including new NLB target group." -ForegroundColor Yellow
    foreach ($lb in $existingLbs) {
        $updateSvcArgs += "--load-balancers"
        $updateSvcArgs += "targetGroupArn=$($lb.targetGroupArn),containerName=$($lb.containerName),containerPort=$($lb.containerPort)"
    }
}
$updateSvcArgs += "--load-balancers"
$updateSvcArgs += $lbSpec
& aws @updateSvcArgs | Out-Null
if ($LASTEXITCODE -ne 0) { throw "ecs update-service failed" }

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Green
Write-Host "Stable public IP(s) (Elastic IP) - use any one in browser/Postman:" -ForegroundColor Cyan
foreach ($e in $eipAllocs) {
    Write-Host "  http://$($e.PublicIp):$ListenerPort/actuator/health"
    Write-Host "  http://$($e.PublicIp):$ListenerPort/api/v1/..."
}
Write-Host ""
Write-Host "NLB ARN: $nlbArn"
Write-Host "Target group ARN: $tgArn"
Write-Host "ReleaseElasticIps: release-address + delete-load-balancer when tearing down (EIPs cost if idle)."
