variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC where EC2 instances will be created"
  type        = string
}

variable "subnet_id" {
  description = "Subnet ID where EC2 instances will be created"
  type        = string
}

variable "security_groups" {
  description = "List of security group IDs for the EC2 instances"
  type        = list(string)
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}

variable "key_name" {
  description = "Name of the EC2 Key Pair to use for the instance"
  type        = string
}

variable "ami_id" {
  description = "AMI ID to use for the instance"
  type        = string
}

variable "root_volume_size" {
  description = "Size of the root volume in GB"
  type        = number
  default     = 20
}

# Auto Scaling Group Configuration
variable "min_capacity" {
  description = "Minimum number of instances in the Auto Scaling Group"
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "Maximum number of instances in the Auto Scaling Group"
  type        = number
  default     = 3
}

variable "desired_capacity" {
  description = "Desired number of instances in the Auto Scaling Group"
  type        = number
  default     = 1
}

# Target Group ARNs - REMOVED for cost optimization (no ALB)
# variable "backend_target_group_arn" {
#   description = "ARN of the backend target group"
#   type        = string
# }

# variable "frontend_target_group_arn" {
#   description = "ARN of the frontend target group"
#   type        = string
# }

# For backward compatibility
# variable "target_group_arn" {
#   description = "ARN of the target group (for backward compatibility)"
#   type        = string
#   default     = ""
# }

variable "assign_public_ip" {
  description = "Assign public IP to EC2 instances"
  type        = bool
  default     = true
}

# Database Configuration
variable "db_endpoint" {
  description = "RDS database endpoint"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "db_username" {
  description = "Database username"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "redis_endpoint" {
  description = "Redis endpoint"
  type        = string
  default     = ""
}

variable "tags" {
  description = "A map of tags to assign to the resource"
  type        = map(string)
  default     = {}
}