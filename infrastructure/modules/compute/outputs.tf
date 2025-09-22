output "launch_template_id" {
  description = "ID of the launch template"
  value       = aws_launch_template.app.id
}

output "launch_template_arn" {
  description = "ARN of the launch template"
  value       = aws_launch_template.app.arn
}

output "autoscaling_group_id" {
  description = "The autoscaling group id"
  value       = aws_autoscaling_group.app.id
}

output "autoscaling_group_name" {
  description = "The autoscaling group name"
  value       = aws_autoscaling_group.app.name
}

output "autoscaling_group_arn" {
  description = "The ARN for this autoscaling group"
  value       = aws_autoscaling_group.app.arn
}

output "iam_role_arn" {
  description = "ARN of the IAM role"
  value       = aws_iam_role.ec2_role.arn
}

output "iam_instance_profile_name" {
  description = "Name of the IAM instance profile"
  value       = aws_iam_instance_profile.ec2_profile.name
}

output "scale_up_policy_arn" {
  description = "ARN of the scale up policy"
  value       = aws_autoscaling_policy.scale_up.arn
}

output "scale_down_policy_arn" {
  description = "ARN of the scale down policy"
  value       = aws_autoscaling_policy.scale_down.arn
}