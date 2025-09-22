# IAM Role for EC2 instance
resource "aws_iam_role" "ec2_role" {
  name = "${var.environment}-sentinel-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

# IAM Role Policy for SSM and CloudWatch
resource "aws_iam_role_policy" "ec2_policy" {
  name = "${var.environment}-sentinel-ec2-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:UpdateInstanceInformation",
          "ssm:SendCommand",
          "ssm:GetCommandInvocation",
          "ssm:DescribeInstanceInformation",
          "ssm:DescribeCommands",
          "ec2messages:AcknowledgeMessage",
          "ec2messages:DeleteMessage",
          "ec2messages:FailMessage",
          "ec2messages:GetEndpoint",
          "ec2messages:GetMessages",
          "ec2messages:SendReply"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams",
          "logs:DescribeLogGroups"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage"
        ]
        Resource = "*"
      }
    ]
  })
}

# Attach SSM managed policy
resource "aws_iam_role_policy_attachment" "ssm_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# Attach CloudWatch agent policy
resource "aws_iam_role_policy_attachment" "cloudwatch_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# Instance Profile
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.environment}-sentinel-ec2-profile"
  role = aws_iam_role.ec2_role.name

  tags = var.tags
}

# User Data Script
locals {
  user_data = base64encode(templatefile("${path.module}/user_data.sh", {
    environment     = var.environment
    db_endpoint     = var.db_endpoint
    db_name         = var.db_name
    db_username     = var.db_username
    db_password     = var.db_password
    redis_endpoint  = var.redis_endpoint
    aws_region      = data.aws_region.current.name
    log_group_name  = "/aws/ec2/${var.environment}-sentinel"
  }))
}

# Data sources
data "aws_region" "current" {}

# Launch Template
resource "aws_launch_template" "app" {
  name_prefix   = "${var.environment}-sentinel-"
  description   = "Launch template for Sentinel application servers"
  image_id      = var.ami_id
  instance_type = var.instance_type
  key_name      = var.key_name

  # vpc_security_group_ids = var.security_groups  # Moved to network_interfaces

  network_interfaces {
    associate_public_ip_address = var.assign_public_ip
    security_groups = var.security_groups
    delete_on_termination = true
  }

  iam_instance_profile {
    name = aws_iam_instance_profile.ec2_profile.name
  }

  user_data = local.user_data

  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size = var.root_volume_size
      volume_type = "gp3"
      encrypted   = true
      throughput  = 125
      iops        = 3000
      delete_on_termination = true
    }
  }

  monitoring {
    enabled = true
  }

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
    http_put_response_hop_limit = 1
  }

  tag_specifications {
    resource_type = "instance"
    tags = merge(var.tags, {
      Name = "${var.environment}-sentinel-app"
      Type = "Application"
    })
  }

  tag_specifications {
    resource_type = "volume"
    tags = merge(var.tags, {
      Name = "${var.environment}-sentinel-app-volume"
    })
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-launch-template"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Auto Scaling Group
resource "aws_autoscaling_group" "app" {
  name                = "${var.environment}-sentinel-asg"
  vpc_zone_identifier = [var.subnet_id]
  # target_group_arns   = [var.backend_target_group_arn, var.frontend_target_group_arn]  # Removed ALB
  health_check_type   = "EC2"  # Changed from ELB to EC2
  health_check_grace_period = 300

  min_size         = var.min_capacity
  max_size         = var.max_capacity
  desired_capacity = var.desired_capacity

  launch_template {
    id      = aws_launch_template.app.id
    version = "$Latest"
  }

  # Instance refresh configuration
  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 50
      instance_warmup        = 300
    }
  }

  # Scaling policies will be added via separate resources
  enabled_metrics = [
    "GroupMinSize",
    "GroupMaxSize",
    "GroupDesiredCapacity",
    "GroupInServiceInstances",
    "GroupTotalInstances"
  ]

  tag {
    key                 = "Name"
    value               = "${var.environment}-sentinel-asg"
    propagate_at_launch = false
  }

  dynamic "tag" {
    for_each = var.tags
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }

  lifecycle {
    create_before_destroy = true
  }
}

# CloudWatch Alarms for Auto Scaling
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "${var.environment}-sentinel-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "120"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors ec2 cpu utilization"
  alarm_actions       = [aws_autoscaling_policy.scale_up.arn]

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.app.name
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "low_cpu" {
  alarm_name          = "${var.environment}-sentinel-low-cpu"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "120"
  statistic           = "Average"
  threshold           = "10"
  alarm_description   = "This metric monitors ec2 cpu utilization"
  alarm_actions       = [aws_autoscaling_policy.scale_down.arn]

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.app.name
  }

  tags = var.tags
}

# Auto Scaling Policies
resource "aws_autoscaling_policy" "scale_up" {
  name                   = "${var.environment}-sentinel-scale-up"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 300
  autoscaling_group_name = aws_autoscaling_group.app.name
}

resource "aws_autoscaling_policy" "scale_down" {
  name                   = "${var.environment}-sentinel-scale-down"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 300
  autoscaling_group_name = aws_autoscaling_group.app.name
}