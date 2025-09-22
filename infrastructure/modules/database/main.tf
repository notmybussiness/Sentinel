# RDS Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.environment}-sentinel-db-subnet-group"
  subnet_ids = var.subnet_ids

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-db-subnet-group"
  })
}

# RDS Parameter Group
resource "aws_db_parameter_group" "postgres" {
  family = "postgres15"
  name   = "${var.environment}-sentinel-postgres-params"

  parameter {
    name  = "log_statement"
    value = "all"
  }

  parameter {
    name  = "log_min_duration_statement"
    value = "1000" # Log queries taking more than 1 second
  }

  parameter {
    name  = "shared_preload_libraries"
    value = "pg_stat_statements"
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-postgres-params"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# RDS Instance
resource "aws_db_instance" "postgres" {
  identifier             = "${var.environment}-sentinel-db"
  engine                 = "postgres"
  engine_version         = "15.8"
  instance_class         = var.db_instance_class
  allocated_storage      = var.db_allocated_storage
  max_allocated_storage  = var.db_max_allocated_storage
  storage_type           = "gp3"
  storage_encrypted      = true

  # Database configuration
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  # Network configuration
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = var.security_groups
  publicly_accessible    = false
  port                   = 5432

  # Parameter and option groups
  parameter_group_name = aws_db_parameter_group.postgres.name

  # Backup configuration
  backup_retention_period   = var.backup_retention_period
  backup_window            = "03:00-04:00"  # UTC
  maintenance_window       = "sun:04:00-sun:05:00"  # UTC
  auto_minor_version_upgrade = true

  # Monitoring
  monitoring_interval = 60
  monitoring_role_arn = aws_iam_role.rds_monitoring.arn
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  # Performance Insights
  performance_insights_enabled = var.performance_insights_enabled
  performance_insights_retention_period = var.performance_insights_enabled ? 7 : null

  # Deletion protection
  deletion_protection = var.deletion_protection
  skip_final_snapshot = !var.deletion_protection
  final_snapshot_identifier = var.deletion_protection ? "${var.environment}-sentinel-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null

  # Apply changes immediately in non-prod environments
  apply_immediately = var.environment != "prod"

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-postgres"
    Type = "Database"
  })

  lifecycle {
    ignore_changes = [
      password,
      final_snapshot_identifier
    ]
  }
}

# IAM Role for RDS Monitoring
resource "aws_iam_role" "rds_monitoring" {
  name = "${var.environment}-sentinel-rds-monitoring"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# ElastiCache Subnet Group
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.environment}-sentinel-redis-subnet-group"
  subnet_ids = var.subnet_ids

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-redis-subnet-group"
  })
}

# ElastiCache Parameter Group
resource "aws_elasticache_parameter_group" "redis" {
  family = "redis7.x"
  name   = "${var.environment}-sentinel-redis-params"

  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-redis-params"
  })
}

# ElastiCache Redis Cluster
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id         = "${var.environment}-sentinel-redis"
  description                  = "Redis cluster for Sentinel application"
  
  node_type                   = var.redis_node_type
  port                        = 6379
  parameter_group_name        = aws_elasticache_parameter_group.redis.name
  
  # Cluster configuration
  num_cache_clusters          = var.redis_num_replicas + 1
  automatic_failover_enabled  = var.redis_num_replicas > 0
  multi_az_enabled           = var.redis_num_replicas > 0
  
  # Network
  subnet_group_name          = aws_elasticache_subnet_group.redis.name
  security_group_ids         = var.security_groups
  
  # Backup
  snapshot_retention_limit   = var.redis_snapshot_retention_limit
  snapshot_window           = "03:00-05:00"
  maintenance_window        = "sun:05:00-sun:07:00"
  
  # Encryption
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                = var.redis_auth_token
  
  # Logging
  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis_slow.name
    destination_type = "cloudwatch-logs"
    log_format      = "text"
    log_type        = "slow-log"
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-redis"
    Type = "Cache"
  })

  depends_on = [aws_cloudwatch_log_group.redis_slow]
}

# CloudWatch Log Group for Redis
resource "aws_cloudwatch_log_group" "redis_slow" {
  name              = "/aws/elasticache/redis/${var.environment}-sentinel"
  retention_in_days = 7

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-redis-logs"
  })
}