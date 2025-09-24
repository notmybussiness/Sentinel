# Sentinel Infrastructure Configuration
# Copy this file to terraform.tfvars and customize the values

# Basic Configuration
aws_region   = "ap-northeast-2"
environment  = "prod"
project_name = "sentinel"

# Network Configuration
vpc_cidr = "10.0.0.0/16"

# Compute Configuration (Free Tier)
instance_type = "t2.micro"  # Free tier eligible
key_name     = "TODO(human)"  # TODO(human): Enter your desired EC2 key pair name

# Database Configuration (Free Tier)
db_instance_class     = "db.t3.micro"  # Free tier eligible
db_allocated_storage  = 20              # Free tier limit
db_name              = "sentinel"
db_username          = "postgres"
db_password          = "TODO(human)"  # TODO(human): Enter secure password (8+ chars, mixed case, numbers, symbols)

# Domain Configuration (Optional)
domain_name         = "TODO(human)"  # TODO(human): Enter your domain name (e.g., "yoursentinel.click")
ssl_certificate_arn = ""  # ACM certificate ARN for HTTPS

# Application Secrets
secrets = {
  jwt_secret           = "your-jwt-secret-256-bit-key"
  kakao_client_id     = "your-kakao-client-id"
  kakao_client_secret = "your-kakao-client-secret"
  alphavantage_api_key = "your-alphavantage-api-key"
  finnhub_api_key     = "your-finnhub-api-key"
  gemini_api_key      = "your-gemini-api-key"
}

# Monitoring Configuration
enable_detailed_monitoring = false  # true adds cost
log_retention_days        = 7       # Free tier friendly

# Backup Configuration
backup_retention_days = 7

# Cost Optimization
enable_cost_optimization = true
auto_scaling_enabled    = false  # Disable to avoid additional costs

# Additional Tags
additional_tags = {
  Owner       = "YourName"
  Team        = "Development"
  CostCenter  = "Engineering"
  Environment = "Production"
}