# 📋 Sentinel AWS EC2 Deployment Plan

> **Generated**: 2025-09-23
> **Target Cost**: $3-15/month (AWS Free Tier Optimized)
> **Timeline**: 7 days to production

## 🏗️ Project Overview

**Architecture**: Spring Boot 3.5.5 + Next.js 14 monorepo
**Deployment Strategy**: Single EC2 instance with Docker containers
**Infrastructure**: Terraform-managed AWS resources

### Current Technology Stack
- **Backend**: Spring Boot 3.5.5, PostgreSQL, Redis, JWT auth, Kakao OAuth
- **Frontend**: Next.js 14, TypeScript, Tailwind CSS, Radix UI, Zustand
- **Infrastructure**: Terraform, Docker, GitHub Actions CI/CD
- **External APIs**: Alpha Vantage, Finnhub, Yahoo Finance
- **Testing**: JUnit, Playwright E2E, Jest (Frontend)

## 📊 Infrastructure Status Assessment

### ✅ Production-Ready Components
- **Terraform Infrastructure**: Complete VPC, security groups, RDS, EC2 modules
- **Docker Configuration**: Multi-stage builds with health checks
- **CI/CD Pipeline**: Quality checks, security scanning, automated deployment
- **Application Configuration**: Environment-specific YAML configurations
- **Database Setup**: PostgreSQL + Redis with proper connection pooling
- **Security**: Security groups, IAM roles, encrypted storage

### ⚠️ Configuration Requirements
- AWS credentials & EC2 key pair creation
- Environment variables for external APIs
- SSL certificate acquisition (optional)
- GitHub repository secrets configuration
- Database migration scripts verification

## 🚀 Detailed Deployment Plan

### Phase 1: AWS Infrastructure Setup (Days 1-2)

#### Prerequisites Checklist
```bash
# 1. AWS Account Configuration
- Create AWS account with billing alerts ($20 threshold)
- Enable free tier notifications
- Set up IAM user with AdministratorAccess
- Generate and securely store access keys
- Create EC2 key pair in target region

# 2. Local Environment Setup
aws configure set aws_access_key_id YOUR_KEY
aws configure set aws_secret_access_key YOUR_SECRET
aws configure set default.region us-east-1
aws configure set default.output json
```

#### Terraform Infrastructure Deployment
```bash
# 1. Infrastructure Repository Setup
cd Sentinel/infrastructure
cp terraform.tfvars.example terraform.tfvars

# 2. Configure terraform.tfvars (Critical Settings)
aws_region = "us-east-1"
environment = "prod"
project_name = "sentinel"
instance_type = "t2.micro"  # Free tier eligible
db_instance_class = "db.t3.micro"  # Free tier eligible
db_allocated_storage = 20  # Free tier limit
key_name = "your-aws-key-pair-name"
db_password = "SECURE_PASSWORD_HERE"

# 3. Deploy Infrastructure
terraform init
terraform validate
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars" -auto-approve

# 4. Capture Critical Outputs
terraform output > ../deployment_outputs.txt
```

### Phase 2: Application Deployment Setup (Days 3-4)

#### GitHub Repository Secrets Configuration
```yaml
# AWS Infrastructure
AWS_ACCESS_KEY_ID: "AKIA..."
AWS_SECRET_ACCESS_KEY: "..."
AWS_REGION: "us-east-1"

# EC2 Instance IDs (from Terraform output)
STAGING_INSTANCE_ID: "i-xxxxxxxxx"
PRODUCTION_INSTANCE_ID: "i-xxxxxxxxx"

# ECR Repositories
ECR_REPOSITORY_BACKEND: "sentinel-backend"
ECR_REPOSITORY_FRONTEND: "sentinel-frontend"

# Database Configuration
DB_PASSWORD: "your-secure-password"
DATABASE_URL: "jdbc:postgresql://RDS_ENDPOINT:5432/sentinel"

# External API Keys
KAKAO_CLIENT_ID: "your-kakao-client-id"
KAKAO_CLIENT_SECRET: "your-kakao-client-secret"
ALPHA_VANTAGE_API_KEY: "your-alphavantage-key"
FINNHUB_API_KEY: "your-finnhub-key"
GEMINI_API_KEY: "your-gemini-key"

# Application URLs
STAGING_URL: "http://STAGING_IP"
PRODUCTION_URL: "http://PRODUCTION_IP"

# Optional Integrations
SNYK_TOKEN: "your-snyk-token"
SLACK_WEBHOOK_URL: "your-slack-webhook"
```

#### CI/CD Pipeline Activation
```bash
# 1. Deploy to Staging Environment
git checkout develop
git add .
git commit -m "feat: activate CI/CD pipeline"
git push origin develop

# 2. Monitor GitHub Actions
- Quality Checks: Lint, type check, tests, security scan
- Build Phase: Docker images pushed to ECR
- Staging Deployment: SSM commands to EC2 instance
- Health Verification: Automated health checks

# 3. Production Deployment
git checkout main
git merge develop
git push origin main
```

### Phase 3: Optimization & Monitoring (Days 5-7)

#### Cost Optimization Strategies
```yaml
# Infrastructure Cost Savings
ALB_DISABLED: true  # Saves $16/month - direct EC2 access
RDS_INSTANCE: "db.t3.micro"  # Free tier eligible
LOG_RETENTION: "7 days"  # Minimize CloudWatch costs
ECR_LIFECYCLE: "10 images max"  # Automated cleanup
AUTO_SCALING: false  # Disable to avoid costs

# Application Optimizations
SPRING_PROFILES: "prod"  # Production-optimized settings
DOCKER_MULTI_STAGE: true  # Minimize image sizes
HEALTH_CHECKS: "enabled"  # Automated monitoring
```

#### Monitoring & Alerting Setup
```bash
# CloudWatch Alarms Configuration
aws cloudwatch put-metric-alarm \
  --alarm-name "Sentinel-HighCPU" \
  --alarm-description "CPU usage above 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2

# Application Health Monitoring
- Backend: /api/actuator/health
- Frontend: /api/health
- Database: Connection pool monitoring
- Redis: Cache hit rate monitoring
```

## 🎯 Access Configuration

### Development Environment
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Database Console**: http://localhost:8080/h2-console (dev only)

### Production Environment
- **Application**: http://EC2_PUBLIC_IP
- **API Endpoints**: http://EC2_PUBLIC_IP/api
- **Health Checks**: http://EC2_PUBLIC_IP/api/actuator/health
- **Admin Panel**: Restricted access via security groups

## 💰 Detailed Cost Analysis (Monthly)

| AWS Service | Configuration | Monthly Cost |
|-------------|---------------|--------------|
| EC2 t2.micro | 1 instance, 24/7 | $0 (Free Tier) |
| RDS db.t3.micro | 20GB storage | $0 (Free Tier) |
| EBS Storage | 30GB GP2 | $3.00 |
| Data Transfer | 1GB outbound | $0 (Free Tier) |
| ECR Storage | <500MB | $0.00 |
| CloudWatch | Basic metrics | $0.00 |
| Route53 (Optional) | Hosted zone | $12.00 |
| **Total (No Domain)** | | **$3.00/month** |
| **Total (With Domain)** | | **$15.00/month** |

## 🔄 CI/CD Workflow Details

### Branch Strategy
```yaml
develop:
  - Triggers staging deployment
  - Runs integration tests
  - Automated quality checks

main:
  - Triggers production deployment
  - Requires successful staging tests
  - Manual approval gates
```

### Pipeline Stages
1. **Quality Gates**: ESLint, TypeScript, JUnit, Security scanning
2. **Build Process**: Multi-stage Docker builds, ECR push
3. **Deployment**: AWS SSM commands, container orchestration
4. **Validation**: Health checks, E2E tests, performance monitoring
5. **Notification**: Slack alerts, deployment status updates

## 🚨 Security Considerations

### Infrastructure Security
- VPC with public/private subnet isolation
- Security groups with minimal required access
- RDS encryption at rest and in transit
- IAM roles with least privilege principles
- S3 bucket encryption and access controls

### Application Security
- JWT token-based authentication
- Kakao OAuth2 integration
- Input validation and sanitization
- CORS configuration for frontend
- Rate limiting on API endpoints

## 📋 Deployment Checklist

### Pre-Deployment
- [ ] AWS account configured with billing alerts
- [ ] IAM user created with necessary permissions
- [ ] EC2 key pair generated and stored securely
- [ ] External API keys obtained and validated
- [ ] Domain name purchased (optional)

### Infrastructure Deployment
- [ ] Terraform configuration validated
- [ ] Infrastructure deployed successfully
- [ ] RDS database accessible
- [ ] EC2 instance accessible via SSH
- [ ] Security groups configured correctly

### Application Deployment
- [ ] GitHub secrets configured
- [ ] CI/CD pipeline activated
- [ ] Docker images built and pushed
- [ ] Application deployed to staging
- [ ] Integration tests passing
- [ ] Production deployment successful

### Post-Deployment
- [ ] Health checks passing
- [ ] Monitoring and alerting active
- [ ] Backup procedures implemented
- [ ] Documentation updated
- [ ] Team access configured

## 🔧 Troubleshooting Guide

### Common Issues & Solutions
```yaml
# Infrastructure Issues
terraform_state_lock: "Delete DynamoDB lock entry manually"
rds_connection_timeout: "Check security group inbound rules"
ec2_ssh_access: "Verify key pair and security group port 22"

# Application Issues
docker_build_failure: "Check Dockerfile syntax and base images"
health_check_failure: "Verify application.yml configuration"
oauth_callback_error: "Update Kakao console redirect URIs"

# Performance Issues
high_cpu_usage: "Enable detailed monitoring, check memory leaks"
database_slow_queries: "Enable RDS Performance Insights"
frontend_load_time: "Optimize bundle size, enable compression"
```

## 📈 Success Metrics

### Technical KPIs
- **Deployment Time**: <10 minutes
- **Application Startup**: <60 seconds
- **API Response Time**: <200ms average
- **Uptime Target**: 99.9%
- **Cost Target**: <$15/month

### Monitoring Dashboard
- CPU/Memory utilization trends
- API response time percentiles
- Database connection pool metrics
- Error rate and alert frequency
- Cost tracking and optimization opportunities

---

**Next Immediate Actions**:
1. Configure AWS credentials locally
2. Create EC2 key pair in AWS Console
3. Update terraform.tfvars with actual values
4. Execute Terraform deployment
5. Configure GitHub repository secrets
6. Trigger first deployment via git push

**Estimated Total Setup Time**: 6-8 hours over 3-4 days
**Target Go-Live Date**: Within 1 week of starting Phase 1