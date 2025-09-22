variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC where database will be created"
  type        = string
}

variable "subnet_ids" {
  description = "List of subnet IDs for the database"
  type        = list(string)
}

variable "security_groups" {
  description = "List of security group IDs for the database"
  type        = list(string)
}

# PostgreSQL Configuration
variable "db_instance_class" {
  description = "The instance class for the RDS instance"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "The allocated storage for the RDS instance (GB)"
  type        = number
  default     = 20
}

variable "db_max_allocated_storage" {
  description = "The maximum allocated storage for the RDS instance (GB)"
  type        = number
  default     = 100
}

variable "db_name" {
  description = "The name of the database"
  type        = string
}

variable "db_username" {
  description = "The username for the database"
  type        = string
}

variable "db_password" {
  description = "The password for the database"
  type        = string
  sensitive   = true
}

variable "backup_retention_period" {
  description = "The backup retention period for the RDS instance (days)"
  type        = number
  default     = 7
}

variable "performance_insights_enabled" {
  description = "Enable Performance Insights for the RDS instance"
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "Enable deletion protection for the RDS instance"
  type        = bool
  default     = true
}

# Redis Configuration
variable "redis_node_type" {
  description = "The instance class for the Redis cluster"
  type        = string
  default     = "cache.t3.micro"
}

variable "redis_num_replicas" {
  description = "Number of replica nodes for Redis cluster"
  type        = number
  default     = 1
}

variable "redis_snapshot_retention_limit" {
  description = "The number of days to retain Redis snapshots"
  type        = number
  default     = 5
}

variable "redis_auth_token" {
  description = "Auth token for Redis cluster"
  type        = string
  sensitive   = true
  default     = null
}

variable "tags" {
  description = "A map of tags to assign to the resource"
  type        = map(string)
  default     = {}
}