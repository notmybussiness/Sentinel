output "db_instance_id" {
  description = "The RDS instance ID"
  value       = aws_db_instance.postgres.id
}

output "db_endpoint" {
  description = "The RDS instance endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "db_address" {
  description = "The RDS instance address"
  value       = aws_db_instance.postgres.address
}

output "db_port" {
  description = "The RDS instance port"
  value       = aws_db_instance.postgres.port
}

output "db_arn" {
  description = "The ARN of the RDS instance"
  value       = aws_db_instance.postgres.arn
}

output "redis_cluster_id" {
  description = "The ElastiCache cluster ID"
  value       = aws_elasticache_replication_group.redis.id
}

output "redis_endpoint" {
  description = "The ElastiCache cluster endpoint"
  value       = aws_elasticache_replication_group.redis.configuration_endpoint_address
}

output "redis_port" {
  description = "The ElastiCache cluster port"
  value       = aws_elasticache_replication_group.redis.port
}

output "redis_arn" {
  description = "The ARN of the ElastiCache cluster"
  value       = aws_elasticache_replication_group.redis.arn
}