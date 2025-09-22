# Infrastructure Variables
variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "sentinel"
}

# Network Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

# Compute Configuration
variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
  
  validation {
    condition     = can(regex("^t2\\.micro$|^t3\\.micro$|^t3\\.small$", var.instance_type))
    error_message = "Instance type must be free-tier eligible: t2.micro, t3.micro, or t3.small."
  }
}

variable "key_name" {
  description = "AWS key pair name for EC2 access"
  type        = string
  default     = ""
}

# Database Configuration
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
  
  validation {
    condition     = can(regex("^db\\.t3\\.micro$|^db\\.t4g\\.micro$", var.db_instance_class))
    error_message = "DB instance class must be free-tier eligible: db.t3.micro or db.t4g.micro."
  }
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 20
  
  validation {
    condition     = var.db_allocated_storage >= 20 && var.db_allocated_storage <= 20
    error_message = "DB allocated storage must be 20GB for free tier."
  }
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "sentinel"
}

variable "db_username" {
  description = "Database username"
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
  
  validation {
    condition     = length(var.db_password) >= 8
    error_message = "Database password must be at least 8 characters long."
  }
}

# SSL/Domain Configuration
variable "domain_name" {
  description = "Domain name for the application (optional)"
  type        = string
  default     = ""
}

variable "ssl_certificate_arn" {
  description = "ARN of SSL certificate for HTTPS (optional)"
  type        = string
  default     = ""
}

# Application Configuration
variable "app_environment" {
  description = "Application environment variables"
  type        = map(string)
  default = {
    SPRING_PROFILES_ACTIVE = "prod"
    NODE_ENV              = "production"
    NEXT_TELEMETRY_DISABLED = "1"
  }
  sensitive = true
}

# Secret Configuration
variable "secrets" {
  description = "Application secrets"
  type = object({
    jwt_secret              = string
    kakao_client_id        = string
    kakao_client_secret    = string
    alphavantage_api_key   = string
    finnhub_api_key        = string
    gemini_api_key         = string
  })
  sensitive = true
  default = {
    jwt_secret              = ""
    kakao_client_id        = ""
    kakao_client_secret    = ""
    alphavantage_api_key   = ""
    finnhub_api_key        = ""
    gemini_api_key         = ""
  }
}

# Monitoring Configuration
variable "enable_detailed_monitoring" {
  description = "Enable detailed CloudWatch monitoring"
  type        = bool
  default     = false
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7
  
  validation {
    condition = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653], var.log_retention_days)
    error_message = "Log retention days must be a valid CloudWatch retention value."
  }
}

# Backup Configuration
variable "backup_retention_days" {
  description = "RDS backup retention period in days"
  type        = number
  default     = 7
  
  validation {
    condition     = var.backup_retention_days >= 1 && var.backup_retention_days <= 35
    error_message = "Backup retention days must be between 1 and 35."
  }
}

# Cost Optimization
variable "enable_cost_optimization" {
  description = "Enable cost optimization features"
  type        = bool
  default     = true
}

variable "auto_scaling_enabled" {
  description = "Enable auto scaling (additional cost)"
  type        = bool
  default     = false
}

# Resource Tags
variable "additional_tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
}