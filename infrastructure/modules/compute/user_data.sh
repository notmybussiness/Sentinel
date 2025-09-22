#!/bin/bash
set -e

# Update system
yum update -y

# Install Docker
amazon-linux-extras install docker -y
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# Install and configure Nginx
yum install -y nginx
systemctl enable nginx

# Create Nginx configuration for reverse proxy
cat > /etc/nginx/conf.d/sentinel.conf << 'NGINXCONF'
server {
    listen 80;
    server_name _;
    
    # Frontend (default)
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Health checks
    location /actuator/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
NGINXCONF

# Remove default nginx config
rm -f /etc/nginx/conf.d/default.conf

# Test nginx configuration
nginx -t

# Start nginx (will be started after docker containers are up)
systemctl start nginx

# Install AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
./aws/install

# Install CloudWatch agent
yum install -y amazon-cloudwatch-agent

# Create CloudWatch agent config
cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << 'EOF'
{
    "agent": {
        "metrics_collection_interval": 60,
        "run_as_user": "cwagent"
    },
    "logs": {
        "logs_collected": {
            "files": {
                "collect_list": [
                    {
                        "file_path": "/var/log/messages",
                        "log_group_name": "${log_group_name}",
                        "log_stream_name": "{instance_id}/messages"
                    },
                    {
                        "file_path": "/opt/sentinel/logs/application.log",
                        "log_group_name": "${log_group_name}",
                        "log_stream_name": "{instance_id}/application"
                    }
                ]
            }
        }
    },
    "metrics": {
        "namespace": "CWAgent",
        "metrics_collected": {
            "cpu": {
                "measurement": [
                    "cpu_usage_idle",
                    "cpu_usage_iowait",
                    "cpu_usage_user",
                    "cpu_usage_system"
                ],
                "metrics_collection_interval": 60
            },
            "disk": {
                "measurement": [
                    "used_percent"
                ],
                "metrics_collection_interval": 60,
                "resources": [
                    "*"
                ]
            },
            "diskio": {
                "measurement": [
                    "io_time"
                ],
                "metrics_collection_interval": 60,
                "resources": [
                    "*"
                ]
            },
            "mem": {
                "measurement": [
                    "mem_used_percent"
                ],
                "metrics_collection_interval": 60
            },
            "swap": {
                "measurement": [
                    "swap_used_percent"
                ],
                "metrics_collection_interval": 60
            }
        }
    }
}
EOF

# Start CloudWatch agent
/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json -s

# Create application directory
mkdir -p /opt/sentinel
cd /opt/sentinel

# Create environment file
cat > .env << EOF
SPRING_PROFILES_ACTIVE=${environment}
DATABASE_URL=jdbc:postgresql://${db_endpoint}:5432/${db_name}
DATABASE_USERNAME=${db_username}
DATABASE_PASSWORD=${db_password}
REDIS_HOST=${redis_endpoint}
REDIS_PORT=6379
AWS_REGION=${aws_region}
EOF

# Create docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  backend:
    image: $ECR_REGISTRY/sentinel-backend:$IMAGE_TAG
    container_name: sentinel-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=$${SPRING_PROFILES_ACTIVE}
      - DATABASE_URL=$${DATABASE_URL}
      - DATABASE_USERNAME=$${DATABASE_USERNAME}
      - DATABASE_PASSWORD=$${DATABASE_PASSWORD}
      - REDIS_HOST=$${REDIS_HOST}
      - REDIS_PORT=$${REDIS_PORT}
      - KAKAO_CLIENT_ID=$${KAKAO_CLIENT_ID}
      - KAKAO_CLIENT_SECRET=$${KAKAO_CLIENT_SECRET}
      - ALPHA_VANTAGE_API_KEY=$${ALPHA_VANTAGE_API_KEY}
      - FINNHUB_API_KEY=$${FINNHUB_API_KEY}
    volumes:
      - /opt/sentinel/logs:/app/logs
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    image: $ECR_REGISTRY/sentinel-frontend:$IMAGE_TAG
    container_name: sentinel-frontend
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - NEXT_PUBLIC_API_URL=$${NEXT_PUBLIC_API_URL}
    depends_on:
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
EOF

# Create deployment script
cat > deploy.sh << 'EOF'
#!/bin/bash
set -e

# Load environment variables
source .env

# Get ECR login token
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

# Pull latest images
export ECR_REGISTRY=$(aws ecr describe-repositories --repository-names sentinel-backend --query 'repositories[0].repositoryUri' --output text | cut -d'/' -f1)
export IMAGE_TAG=${1:-latest}
export NEXT_PUBLIC_API_URL="http://$(curl -s http://169.254.169.254/latest/meta-data/public-hostname):8080"

# Pull and start services
docker-compose pull
docker-compose up -d --force-recreate

# TODO(human): Add Nginx restart after containers are up
# Wait for containers to be healthy
sleep 30

# Restart Nginx to ensure proxy is working
systemctl reload nginx

# Cleanup old images
docker image prune -af --filter "until=24h"

echo "Deployment completed successfully!"
EOF

chmod +x deploy.sh

# Create logs directory
mkdir -p /opt/sentinel/logs

# Set permissions
chown -R ec2-user:ec2-user /opt/sentinel

# Create systemd service for auto-deployment monitoring
cat > /etc/systemd/system/sentinel-monitor.service << 'EOF'
[Unit]
Description=Sentinel Application Monitor
After=network.target

[Service]
Type=oneshot
User=ec2-user
WorkingDirectory=/opt/sentinel
ExecStart=/opt/sentinel/deploy.sh
RemainAfterExit=true

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable sentinel-monitor

# Initial deployment will be triggered by CI/CD pipeline
echo "Instance setup completed!"