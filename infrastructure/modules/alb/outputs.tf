output "alb_id" {
  description = "The ID of the load balancer"
  value       = aws_lb.main.id
}

output "alb_arn" {
  description = "The ARN of the load balancer"
  value       = aws_lb.main.arn
}

output "dns_name" {
  description = "The DNS name of the load balancer"
  value       = aws_lb.main.dns_name
}

output "zone_id" {
  description = "The canonical hosted zone ID of the load balancer"
  value       = aws_lb.main.zone_id
}

output "backend_target_group_arn" {
  description = "ARN of the backend target group"
  value       = aws_lb_target_group.backend.arn
}

output "frontend_target_group_arn" {
  description = "ARN of the frontend target group"
  value       = aws_lb_target_group.frontend.arn
}

output "backend_target_group_name" {
  description = "Name of the backend target group"
  value       = aws_lb_target_group.backend.name
}

output "frontend_target_group_name" {
  description = "Name of the frontend target group"
  value       = aws_lb_target_group.frontend.name
}

# For backward compatibility (main.tf expects this)
output "target_group_arn" {
  description = "ARN of the backend target group (for backward compatibility)"
  value       = aws_lb_target_group.backend.arn
}