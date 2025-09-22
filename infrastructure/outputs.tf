# Infrastructure Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.vpc.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "List of IDs of the public subnets"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "List of IDs of the private subnets"
  value       = module.vpc.private_subnet_ids
}

# Database Outputs
output "db_endpoint" {
  description = "RDS instance endpoint"
  value       = module.database.db_endpoint
  sensitive   = true
}

output "db_port" {
  description = "RDS instance port"
  value       = module.database.db_port
}

output "db_identifier" {
  description = "RDS instance identifier"
  value       = module.database.db_identifier
}

# Load Balancer Outputs
output "load_balancer_dns" {
  description = "DNS name of the load balancer"
  value       = module.alb.dns_name
}

output "load_balancer_zone_id" {
  description = "Zone ID of the load balancer"
  value       = module.alb.zone_id
}

output "load_balancer_arn" {
  description = "ARN of the load balancer"
  value       = module.alb.load_balancer_arn
}

# EC2 Outputs
output "instance_id" {
  description = "ID of the EC2 instance"
  value       = module.compute.instance_id
}

output "instance_public_ip" {
  description = "Public IP address of the EC2 instance"
  value       = module.compute.instance_public_ip
}

output "instance_private_ip" {
  description = "Private IP address of the EC2 instance"
  value       = module.compute.instance_private_ip
  sensitive   = true
}

# ECR Outputs
output "backend_repository_url" {
  description = "URL of the backend ECR repository"
  value       = aws_ecr_repository.backend.repository_url
}

output "frontend_repository_url" {
  description = "URL of the frontend ECR repository"
  value       = aws_ecr_repository.frontend.repository_url
}

# S3 Outputs
output "assets_bucket_name" {
  description = "Name of the S3 assets bucket"
  value       = aws_s3_bucket.assets.id
}

output "assets_bucket_domain_name" {
  description = "Domain name of the S3 assets bucket"
  value       = aws_s3_bucket.assets.bucket_domain_name
}

# Security Group Outputs
output "app_security_group_id" {
  description = "ID of the application security group"
  value       = module.security.app_security_group_id
}

output "alb_security_group_id" {
  description = "ID of the ALB security group"
  value       = module.security.alb_security_group_id
}

output "db_security_group_id" {
  description = "ID of the database security group"
  value       = module.security.db_security_group_id
}

# CloudWatch Outputs
output "log_group_name" {
  description = "Name of the CloudWatch log group"
  value       = aws_cloudwatch_log_group.app.name
}

# DNS Outputs (if domain configured)
output "domain_name" {
  description = "Domain name of the application"
  value       = var.domain_name != "" ? var.domain_name : "Not configured"
}

output "nameservers" {
  description = "Nameservers for the domain (if configured)"
  value       = var.domain_name != "" ? aws_route53_zone.main[0].name_servers : []
}

# Application URLs
output "application_url" {
  description = "Main application URL"
  value = var.domain_name != "" ? (
    var.ssl_certificate_arn != "" ? "https://${var.domain_name}" : "http://${var.domain_name}"
  ) : "http://${module.alb.dns_name}"
}

output "api_url" {
  description = "API base URL"
  value = var.domain_name != "" ? (
    var.ssl_certificate_arn != "" ? "https://${var.domain_name}/api" : "http://${var.domain_name}/api"
  ) : "http://${module.alb.dns_name}/api"
}

# Cost Estimation
output "estimated_monthly_cost" {
  description = "Estimated monthly cost in USD"
  value = {
    ec2_instance    = var.instance_type == "t2.micro" ? "Free tier (750 hours)" : "~$8.50"
    rds_instance    = var.db_instance_class == "db.t3.micro" ? "Free tier (750 hours)" : "~$12.60"
    alb             = "~$16.20"
    ebs_storage     = "30GB Free tier"
    data_transfer   = "1GB Free tier"
    cloudwatch      = "5GB Free tier"
    s3              = "5GB Free tier"
    total_estimate  = var.instance_type == "t2.micro" && var.db_instance_class == "db.t3.micro" ? "~$16.20/month (ALB only)" : "~$37.30/month"
  }
}

# Connection Information
output "connection_info" {
  description = "Connection information for the deployed application"
  value = {
    ssh_command = var.key_name != "" ? "ssh -i ~/.ssh/${var.key_name}.pem ec2-user@${module.compute.instance_public_ip}" : "SSH key not configured"
    database_connection = "psql -h ${module.database.db_endpoint} -p ${module.database.db_port} -U ${var.db_username} -d ${var.db_name}"
    application_health  = "${var.domain_name != "" ? "https://${var.domain_name}" : "http://${module.alb.dns_name}"}/api/actuator/health"
  }
  sensitive = true
}