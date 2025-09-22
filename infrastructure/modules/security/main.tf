# Security Groups for Sentinel Application

# ALB Security Group
resource "aws_security_group" "alb" {
  name_prefix = "${var.environment}-sentinel-alb-"
  vpc_id      = var.vpc_id
  description = "Security group for Application Load Balancer"

  # HTTP from internet
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP from internet"
  }

  # HTTPS from internet
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS from internet"
  }

  # All outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-alb-sg"
    Type = "ALB"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Application Security Group
resource "aws_security_group" "app" {
  name_prefix = "${var.environment}-sentinel-app-"
  vpc_id      = var.vpc_id
  description = "Security group for Sentinel application servers"

  # HTTP from internet (replacing ALB)
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP from internet"
  }

  # HTTPS from internet (replacing ALB)
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS from internet"
  }

  # Backend API (internal)
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["127.0.0.1/32"]
    description = "Backend API (localhost only)"
  }

  # Frontend (internal)
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["127.0.0.1/32"]
    description = "Frontend (localhost only)"
  }

  # SSH access (restricted to specific IP if needed)
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.ssh_allowed_cidrs
    description = "SSH access"
  }

  # All outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-app-sg"
    Type = "Application"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Database Security Group
resource "aws_security_group" "db" {
  name_prefix = "${var.environment}-sentinel-db-"
  vpc_id      = var.vpc_id
  description = "Security group for Sentinel database"

  # PostgreSQL from application servers only
  ingress {
    from_port                = 5432
    to_port                  = 5432
    protocol                 = "tcp"
    source_security_group_id = aws_security_group.app.id
    description              = "PostgreSQL from application"
  }

  # Redis from application servers only
  ingress {
    from_port                = 6379
    to_port                  = 6379
    protocol                 = "tcp"
    source_security_group_id = aws_security_group.app.id
    description              = "Redis from application"
  }

  # No outbound rules needed for database
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = merge(var.tags, {
    Name = "${var.environment}-sentinel-db-sg"
    Type = "Database"
  })

  lifecycle {
    create_before_destroy = true
  }
}