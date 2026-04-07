# Read-only: print Subnets, Security groups, RDS endpoints, and Secrets Manager names/ARNs
# so you can copy values into provision-ecs-fargate.ps1.
#
# Usage:
#   cd infra
#   .\show-aws-values-for-ecs.ps1 -Region ap-south-1

param(
    [string] $Region = "ap-south-1"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Account ===" -ForegroundColor Cyan
aws sts get-caller-identity --output table

Write-Host "`n=== RDS instances (use Endpoint.Address for -RdsEndpoint) ===" -ForegroundColor Cyan
aws rds describe-db-instances --region $Region --query "DBInstances[].{Id:DBInstanceIdentifier,Endpoint:Endpoint.Address,Port:Endpoint.Port,Vpc:VpcId,SG:VpcSecurityGroups[0].VpcSecurityGroupId}" --output table 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "(none or no permission)" -ForegroundColor Yellow }

Write-Host "`n=== Subnets (pick 2+ in the SAME VPC as RDS; Fargate needs them) ===" -ForegroundColor Cyan
aws ec2 describe-subnets --region $Region --query "Subnets[].{SubnetId:SubnetId,VpcId:VpcId,AZ:AvailabilityZone,CIDR:CidrBlock,Name:Tags[?Key=='Name'].Value|[0]}" --output table

Write-Host "`n=== Security groups (pick one for ECS tasks; allow outbound HTTPS + TCP 5432 to RDS) ===" -ForegroundColor Cyan
aws ec2 describe-security-groups --region $Region --query "SecurityGroups[].{GroupId:GroupId,VpcId:VpcId,Name:GroupName}" --output table

Write-Host "`n=== Secrets Manager (full ARN for -RdsPasswordSecretArn and -JwtSecretArn) ===" -ForegroundColor Cyan
aws secretsmanager list-secrets --region $Region --query "SecretList[].{Name:Name,ARN:ARN}" --output table 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "(none or no permission)" -ForegroundColor Yellow }

Write-Host "`n=== ECR repository URI (image used by task definition) ===" -ForegroundColor Cyan
$account = (aws sts get-caller-identity --query Account --output text).Trim()
Write-Host "${account}.dkr.ecr.${Region}.amazonaws.com/connto-backend:latest"

Write-Host "`n--- Next: copy SubnetIds (comma-separated), one SecurityGroupId, RDS endpoint host, two secret ARNs ---" -ForegroundColor Green
Write-Host "Example provision line (replace placeholders):" -ForegroundColor DarkGray
Write-Host '  .\provision-ecs-fargate.ps1 `'
Write-Host '    -SubnetIds "subnet-xxx,subnet-yyy" `'
Write-Host '    -SecurityGroupIds "sg-zzz" `'
Write-Host '    -RdsEndpoint "<from RDS table above>" `'
Write-Host '    -RdsPasswordSecretArn "<full ARN from Secrets table>" `'
Write-Host '    -JwtSecretArn "<full ARN from Secrets table>"'
