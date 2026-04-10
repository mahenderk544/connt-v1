Connto backend on AWS ECS using Docker
======================================

1) Docker image
   - backend/Dockerfile — multi-stage Maven build + JRE 17, port 8080, prod profile default.
   - Local test: from repo root
       docker build -t connto-backend:local backend
       docker run --rm -p 8080:8080 ^
         -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/connto ^
         -e SPRING_DATASOURCE_USERNAME=... -e SPRING_DATASOURCE_PASSWORD=... ^
         -e CONNTO_JWT_SECRET=... connto-backend:local

2) Push image to ECR (manual)
   cd infra
   .\push-backend-ecr.ps1 -Region ap-south-1 -RepositoryName connto-backend

3) First-time ECS task definition + service (Fargate) — PowerShell
   cd infra
   .\provision-ecs-fargate.ps1 -SubnetIds "subnet-xxx,subnet-yyy" -SecurityGroupIds "sg-zzz" `
     -RdsEndpoint "your-rds.region.rds.amazonaws.com" `
     -RdsPasswordSecretArn "arn:aws:secretsmanager:..." -JwtSecretArn "arn:aws:secretsmanager:..."
   (Requires AWS CLI; image tag defaults to latest — push an image to ECR first, or run GitHub Actions once to populate latest.)

3b) First-time ECS task definition (Fargate) — manual
   - Copy infra/ecs/fargate-task-definition.template.json → fargate-task-definition.json
   - Replace placeholders __TASK_FAMILY__, __EXECUTION_ROLE_ARN__, __IMAGE_URI__, __JDBC_URL__,
     __RDS_PASSWORD_SECRET_ARN__, __JWT_SECRET_ARN__, etc. (or run provision-ecs-fargate.ps1)
   - Create log group /ecs/connto-backend in CloudWatch (optional; template uses awslogs-create-group)
   - aws ecs register-task-definition --cli-input-json file://fargate-task-definition.json --region YOUR_REGION

4) ECS service
   - Cluster: Fargate, awsvpc, subnets + security group (allow 8080 from ALB; outbound to RDS 5432)
   - Service: desired count ≥ 1, load balancer optional (ALB health: GET /actuator/health on port 8080)

4b) Stable public IP (Elastic IP) — Fargate tasks cannot have an EIP; use a Network Load Balancer
   - cd infra
   - .\attach-nlb-elastic-ip-ecs.ps1 -SubnetIds "subnet-a,subnet-b" -TaskSecurityGroupId "sg-your-ecs-task-sg"
   - Use public subnets (one per AZ). Script allocates an EIP per subnet, creates NLB + TCP :80 -> tasks :8080,
     attaches target group to the ECS service. Base URL: http://<EIP>:80/...
   - Do not use -DisableTaskPublicIp unless tasks are in private subnets with NAT (ECR pull needs outbound internet).
   - Script enables NLB cross-zone load balancing so both Elastic IPs work when tasks run in only one AZ.

5) Deploy updates (build + push + new revision + rolling deploy)
   cd infra
   .\deploy-backend-ecs.ps1 -Region ap-south-1 -RepositoryName connto-backend `
     -Cluster YOUR_CLUSTER -Service YOUR_SERVICE -TaskDefinitionFamily connto-backend

6) CI (GitHub) — no Docker on laptop
   - .github/workflows/backend-ecs-deploy.yml — push to main when backend/** changes
   - Step-by-step OIDC + IAM: infra/SETUP-GITHUB-OIDC.txt and infra/iam/*.template.json
   - Or access keys: leave AWS_USE_OIDC unset; secrets AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
   - First deploy: create ECS task definition once (see workflow header) or describe-task-definition fails with a fix guide in the job summary

Secrets: prefer Secrets Manager (or SSM) for SPRING_DATASOURCE_PASSWORD and CONNTO_JWT_SECRET;
execution role must allow secretsmanager:GetSecretValue on those ARNs.
