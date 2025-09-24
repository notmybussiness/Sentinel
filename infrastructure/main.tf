# Sentinel AWS Infrastructure
terraform {
  required_version = "~> 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend configuration for state management
  # Commented out for initial testing - uncomment after S3 bucket is created
  # backend "s3" {
  #   bucket = "sentinel-terraform-state"
  #   key    = "prod/terraform.tfstate"
  #   region = "ap-northeast-2"
  #
  #   # State locking
  #   dynamodb_table = "sentinel-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "Sentinel"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}

# Local values
locals {
  availability_zones = slice(data.aws_availability_zones.available.names, 0, 2)
  
  common_tags = {
    Project     = "Sentinel"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# VPC and Networking
module "vpc" {
  source = "./modules/vpc"
  
  environment        = var.environment
  vpc_cidr          = var.vpc_cidr
  availability_zones = local.availability_zones
  
  tags = local.common_tags
}

# Security
module "security" {
  source = "./modules/security"
  
  environment = var.environment
  vpc_id     = module.vpc.vpc_id
  
  tags = local.common_tags
}

# Database
module "database" {
  source = "./modules/database"
  
  environment     = var.environment
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.private_subnet_ids
  security_groups = [module.security.db_security_group_id]
  
  db_instance_class = var.db_instance_class
  db_allocated_storage = var.db_allocated_storage
  db_name           = var.db_name
  db_username       = var.db_username
  db_password       = var.db_password
  
  tags = local.common_tags
}

# TODO(human): ALB module commented out for cost optimization
# Application Load Balancer
# module "alb" {
#   source = "./modules/alb"
#   
#   environment     = var.environment
#   vpc_id         = module.vpc.vpc_id
#   subnet_ids     = module.vpc.public_subnet_ids
#   security_groups = [module.security.alb_security_group_id]
#   
#   certificate_arn = var.ssl_certificate_arn
#   
#   tags = local.common_tags
# }

# EC2 Instance
module "compute" {
  source = "./modules/compute"
  
  environment      = var.environment
  vpc_id          = module.vpc.vpc_id
  subnet_id       = module.vpc.public_subnet_ids[0]  # Changed to public for direct internet access
  security_groups = [module.security.app_security_group_id]
  assign_public_ip = true  # Enable public IP for direct access
  
  instance_type    = var.instance_type
  key_name        = var.key_name
  ami_id          = data.aws_ami.amazon_linux.id
  
  # Application configuration
  db_endpoint = module.database.db_endpoint
  db_name     = var.db_name
  db_username = var.db_username
  db_password = var.db_password
  redis_endpoint = module.database.redis_endpoint
  
  # Load balancer target groups - REMOVED for cost optimization
  # backend_target_group_arn  = module.alb.backend_target_group_arn
  # frontend_target_group_arn = module.alb.frontend_target_group_arn
  # target_group_arn         = module.alb.backend_target_group_arn
  
  tags = local.common_tags
}

# ECR Repositories
resource "aws_ecr_repository" "backend" {
  name = "sentinel-backend"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  lifecycle_policy {
    policy = jsonencode({
      rules = [
        {
          rulePriority = 1
          description  = "Keep last 10 images"
          selection = {
            tagStatus     = "tagged"
            tagPrefixList = ["v"]
            countType     = "imageCountMoreThan"
            countNumber   = 10
          }
          action = {
            type = "expire"
          }
        }
      ]
    })
  }
  
  tags = local.common_tags
}

resource "aws_ecr_repository" "frontend" {
  name = "sentinel-frontend"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  lifecycle_policy {
    policy = jsonencode({
      rules = [
        {
          rulePriority = 1
          description  = "Keep last 10 images"
          selection = {
            tagStatus     = "tagged"
            tagPrefixList = ["v"]
            countType     = "imageCountMoreThan"
            countNumber   = 10
          }
          action = {
            type = "expire"
          }
        }
      ]
    })
  }
  
  tags = local.common_tags
}

# S3 Bucket for static assets
resource "aws_s3_bucket" "assets" {
  bucket = "${var.project_name}-${var.environment}-assets"
  
  tags = local.common_tags
}

resource "aws_s3_bucket_versioning" "assets" {
  bucket = aws_s3_bucket.assets.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_encryption" "assets" {
  bucket = aws_s3_bucket.assets.id

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
}

resource "aws_s3_bucket_public_access_block" "assets" {
  bucket = aws_s3_bucket.assets.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "app" {
  name              = "/aws/ec2/${var.project_name}-${var.environment}"
  retention_in_days = 7
  
  tags = local.common_tags
}

# Route 53 (Optional)
resource "aws_route53_zone" "main" {
  count = var.domain_name != "" ? 1 : 0
  name  = var.domain_name
  
  tags = local.common_tags
}

resource "aws_route53_record" "app" {
  count   = var.domain_name != "" ? 1 : 0
  zone_id = aws_route53_zone.main[0].zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = module.alb.dns_name
    zone_id                = module.alb.zone_id
    evaluate_target_health = true
  }
}