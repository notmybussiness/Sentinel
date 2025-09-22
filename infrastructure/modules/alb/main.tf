# Application Load Balancer
resource "aws_lb" "main" {
  name               = "${var.environment}-sentinel-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = var.security_groups
  subnets           = var.subnet_ids

  enable_deletion_protection = var.deletion_protection
  enable_http2              = true
  enable_cross_zone_load_balancing = true

  access_logs {
    bucket  = var.access_logs_bucket
    prefix  = "alb"
    enabled = var.access_logs_bucket != null
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-alb"
    Type = "LoadBalancer"
  })
}

# Target Group for Backend (Spring Boot)
resource "aws_lb_target_group" "backend" {
  name     = "${var.environment}-sentinel-backend-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = var.vpc_id

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 3
  }

  # Stickiness configuration
  stickiness {
    type            = "lb_cookie"
    enabled         = false
    cookie_duration = 86400
  }

  # Connection draining
  deregistration_delay = 300

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-backend-tg"
    Type = "TargetGroup"
    Service = "Backend"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Target Group for Frontend (Next.js)
resource "aws_lb_target_group" "frontend" {
  name     = "${var.environment}-sentinel-frontend-tg"
  port     = 3000
  protocol = "HTTP"
  vpc_id   = var.vpc_id

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/api/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 3
  }

  # Stickiness configuration
  stickiness {
    type            = "lb_cookie"
    enabled         = false
    cookie_duration = 86400
  }

  # Connection draining
  deregistration_delay = 300

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-frontend-tg"
    Type = "TargetGroup"
    Service = "Frontend"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# HTTP Listener (redirect to HTTPS)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }

  tags = var.tags
}

# HTTPS Listener (if certificate is provided)
resource "aws_lb_listener" "https" {
  count             = var.certificate_arn != null ? 1 : 0
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = var.certificate_arn

  # Default action - forward to frontend
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }

  tags = var.tags
}

# HTTP Listener for non-HTTPS setup (development/testing)
resource "aws_lb_listener" "http_only" {
  count             = var.certificate_arn == null ? 1 : 0
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  # Default action - forward to frontend
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }

  tags = var.tags
}

# Listener Rules for API routing
resource "aws_lb_listener_rule" "api" {
  count        = var.certificate_arn != null ? 1 : 0
  listener_arn = aws_lb_listener.https[0].arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }

  condition {
    path_pattern {
      values = ["/api/*", "/actuator/*"]
    }
  }

  tags = var.tags
}

# Listener Rules for API routing (HTTP only)
resource "aws_lb_listener_rule" "api_http" {
  count        = var.certificate_arn == null ? 1 : 0
  listener_arn = aws_lb_listener.http_only[0].arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }

  condition {
    path_pattern {
      values = ["/api/*", "/actuator/*"]
    }
  }

  tags = var.tags
}