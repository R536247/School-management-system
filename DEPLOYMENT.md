# Production Deployment Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CDN (CloudFront)                         │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│                Load Balancer (ALB/NLB)                            │
│                  SSL/TLS Termination                              │
└──────────┬──────────────────────┬──────────────────────┬──────────┘
           │                      │                      │
    ┌──────▼──────┐        ┌──────▼──────┐        ┌──────▼──────┐
    │ ECS Fargate  │        │ ECS Fargate  │        │ ECS Fargate  │
    │  (Backend)   │        │  (Backend)   │        │  (Frontend)  │
    │  Port 8080   │        │  Port 8080   │        │  Port 3000   │
    └──────┬───────┘        └──────┬───────┘        └──────┬───────┘
           │                       │                       │
           └───────────────┬───────┘────────────────────────┘
                           │
                 ┌─────────▼──────────┐
                 │ RDS PostgreSQL     │
                 │ Multi-AZ           │
                 │ Automated backup   │
                 └───────────────────┘
                 
                 ┌─────────────────────┐
                 │ ElastiCache Redis   │
                 │ Multi-Node Cluster  │
                 └─────────────────────┘
```

## AWS Deployment (ECS Fargate + RDS + ElastiCache)

### 1. Prerequisites

- AWS Account with appropriate IAM permissions
- AWS CLI v2 installed and configured
- Docker CLI configured
- kubectl (for monitoring dashboards)

### 2. Set Environment Variables

```bash
# Core configuration
export AWS_REGION="us-east-1"
export AWS_ACCOUNT_ID="123456789012"
export ENVIRONMENT="production"
export APP_NAME="school-management-system"

# Database configuration
export DB_NAME="schoolms"
export DB_USER="postgres"
export DB_PASSWORD="$(openssl rand -base64 32)"
export DB_INSTANCE_CLASS="db.t3.small"

# Redis configuration
export REDIS_NODE_TYPE="cache.t3.micro"
export REDIS_NUM_CACHE_NODES="2"

# Application configuration
export JWT_SECRET="$(openssl rand -base64 32)"
export JWT_EXPIRY_MINUTES="15"
export REFRESH_EXPIRY_DAYS="7"

# Domain configuration
export DOMAIN="school-system.example.com"
export CERTIFICATE_ARN="arn:aws:acm:us-east-1:123456789012:certificate/xxxxx"
```

### 3. Create ECR Repositories

```bash
aws ecr create-repository \
  --repository-name $APP_NAME-backend \
  --region $AWS_REGION

aws ecr create-repository \
  --repository-name $APP_NAME-frontend \
  --region $AWS_REGION
```

### 4. Build and Push Docker Images

```bash
# Authenticate with ECR
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Build backend
cd backend
docker build -t $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$APP_NAME-backend:latest .
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$APP_NAME-backend:latest

# Build frontend
cd ../frontend
docker build -t $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$APP_NAME-frontend:latest .
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$APP_NAME-frontend:latest
```

### 5. Create RDS PostgreSQL Instance

```bash
aws rds create-db-instance \
  --db-instance-identifier $APP_NAME-db \
  --db-instance-class $DB_INSTANCE_CLASS \
  --engine postgres \
  --engine-version 15.3 \
  --master-username $DB_USER \
  --master-user-password "$DB_PASSWORD" \
  --allocated-storage 100 \
  --storage-type gp3 \
  --multi-az \
  --backup-retention-period 30 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "sun:04:00-sun:05:00" \
  --enable-cloudwatch-logs-exports postgresql \
  --db-subnet-group-name default \
  --vpc-security-group-ids sg-xxxxx \
  --region $AWS_REGION
```

**Wait for instance availability** (5-10 minutes), then:

```bash
# Get RDS endpoint
POSTGRES_HOST=$(aws rds describe-db-instances \
  --db-instance-identifier $APP_NAME-db \
  --region $AWS_REGION \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "RDS Endpoint: $POSTGRES_HOST"
```

### 6. Create ElastiCache Redis Cluster

```bash
aws elasticache create-replication-group \
  --replication-group-description "$APP_NAME Redis cluster" \
  --replication-group-id $APP_NAME-redis \
  --engine redis \
  --engine-version 7.0 \
  --cache-node-type $REDIS_NODE_TYPE \
  --num-cache-clusters $REDIS_NUM_CACHE_NODES \
  --automatic-failover-enabled \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled \
  --transit-encryption-mode preferred \
  --auth-token "$(openssl rand -base64 32)" \
  --cache-subnet-group-name default \
  --security-group-ids sg-xxxxx \
  --region $AWS_REGION
```

**Wait for cluster availability** (5-10 minutes), then:

```bash
# Get Redis endpoint
REDIS_ENDPOINT=$(aws elasticache describe-replication-groups \
  --replication-group-id $APP_NAME-redis \
  --region $AWS_REGION \
  --query 'ReplicationGroups[0].PrimaryEndpoint.Address' \
  --output text)

echo "Redis Endpoint: $REDIS_ENDPOINT"
```

### 7. Create ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name $APP_NAME-cluster \
  --region $AWS_REGION
```

### 8. Create CloudWatch Log Groups

```bash
aws logs create-log-group \
  --log-group-name /ecs/$APP_NAME/backend \
  --region $AWS_REGION

aws logs create-log-group \
  --log-group-name /ecs/$APP_NAME/frontend \
  --region $AWS_REGION
```

### 9. Register ECS Task Definitions

**Backend Task Definition** (`ecs-task-backend.json`):
```json
{
  "family": "school-ms-backend",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "backend",
      "image": "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/school-management-system-backend:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:postgresql://${POSTGRES_HOST}:5432/${DB_NAME}"
        },
        {
          "name": "SPRING_DATASOURCE_USERNAME",
          "value": "${DB_USER}"
        },
        {
          "name": "SPRING_REDIS_HOST",
          "value": "${REDIS_ENDPOINT}"
        },
        {
          "name": "SPRING_REDIS_PORT",
          "value": "6379"
        },
        {
          "name": "APP_JWT_SECRET",
          "value": "${JWT_SECRET}"
        },
        {
          "name": "APP_JWT_ACCESS_EXPIRY_MINUTES",
          "value": "${JWT_EXPIRY_MINUTES}"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:${AWS_REGION}:${AWS_ACCOUNT_ID}:secret:${APP_NAME}/db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/${APP_NAME}/backend",
          "awslogs-region": "${AWS_REGION}",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

```bash
# Store secrets in AWS Secrets Manager
aws secretsmanager create-secret \
  --name $APP_NAME/db-password \
  --secret-string "$DB_PASSWORD" \
  --region $AWS_REGION

# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://ecs-task-backend.json \
  --region $AWS_REGION
```

### 10. Create ECS Services

```bash
aws ecs create-service \
  --cluster $APP_NAME-cluster \
  --service-name backend \
  --task-definition school-ms-backend \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx],securityGroups=[sg-xxxxx],assignPublicIp=DISABLED}" \
  --load-balancers targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=backend,containerPort=8080 \
  --region $AWS_REGION
```

### 11. Create Auto Scaling Policy

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/$APP_NAME-cluster/backend \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10 \
  --region $AWS_REGION

# Create scaling policy (scale up at 70% CPU)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/$APP_NAME-cluster/backend \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-scaling \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '
    TargetValue=70.0,
    PredefinedMetricSpecification={PredefinedMetricType=ECSServiceAverageCPUUtilization},
    ScaleOutCooldown=60,
    ScaleInCooldown=300
  ' \
  --region $AWS_REGION
```

## Local Development

### Quick Start
```bash
# 1. Start infrastructure
docker-compose -f docker-compose.yml up -d

# 2. Wait for Postgres to be ready
sleep 10

# 3. Start backend
cd backend && ./mvnw spring-boot:run

# 4. Start frontend (new terminal)
cd frontend && npm install && npm run dev

# 5. Access application
# Backend: http://localhost:8080
# Frontend: http://localhost:5173
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Health Check
```bash
# Backend health
curl http://localhost:8080/actuator/health

# Redis connectivity
redis-cli -h localhost -p 6379 ping

# Database connectivity
psql -h localhost -U postgres -d schoolms -c "SELECT 1"
```

## Backup & Recovery

### Automated Backups (RDS)
```bash
# List automated backups
aws rds describe-db-snapshots \
  --db-instance-identifier $APP_NAME-db \
  --region $AWS_REGION

# Create manual backup
aws rds create-db-snapshot \
  --db-instance-identifier $APP_NAME-db \
  --db-snapshot-identifier $APP_NAME-db-$(date +%Y%m%d-%H%M%S) \
  --region $AWS_REGION
```

### Manual Database Export
```bash
# Export to S3
aws dms create-replication-task \
  --replication-task-identifier export-task \
  --source-endpoint-arn arn:aws:dms:... \
  --target-endpoint-arn arn:aws:dms:... \
  --replication-instance-arn arn:aws:dms:... \
  --migration-type cdc \
  --table-mappings '{"rules": [{"rule-type": "selection", "rule-id": "1", "rule-name": "include-all", "object-locator": {"schema-name": "%", "table-name": "%"}, "rule-action": "include"}]}'
```

## Monitoring & Alerts

### CloudWatch Alarms

```bash
# Backend CPU
aws cloudwatch put-metric-alarm \
  --alarm-name $APP_NAME-backend-cpu-high \
  --alarm-description "Alert when backend CPU > 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=backend Name=ClusterName,Value=$APP_NAME-cluster \
  --alarm-actions arn:aws:sns:$AWS_REGION:$AWS_ACCOUNT_ID:alerts \
  --region $AWS_REGION

# RDS Connections
aws cloudwatch put-metric-alarm \
  --alarm-name $APP_NAME-db-connections-high \
  --alarm-description "Alert when DB connections > 80" \
  --metric-name DatabaseConnections \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=$APP_NAME-db \
  --alarm-actions arn:aws:sns:$AWS_REGION:$AWS_ACCOUNT_ID:alerts \
  --region $AWS_REGION
```

### Application Metrics (Spring Actuator)

```bash
# Enable metrics endpoints in application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# Scrape metrics with Prometheus
# http://localhost:8080/actuator/prometheus
```

## Secrets Management

### Rotating Database Password

```bash
# 1. Generate new password
NEW_PASSWORD=$(openssl rand -base64 32)

# 2. Update in AWS Secrets Manager
aws secretsmanager update-secret \
  --secret-id $APP_NAME/db-password \
  --secret-string "$NEW_PASSWORD" \
  --region $AWS_REGION

# 3. Update RDS user password (while old connection persists)
aws rds modify-db-instance \
  --db-instance-identifier $APP_NAME-db \
  --master-user-password "$NEW_PASSWORD" \
  --apply-immediately \
  --region $AWS_REGION

# 4. Update ECS task definition with new secret
# (Redeploy service to pick up new secret version)
```

### JWT Secret Rotation

⚠️ **Warning**: Changing JWT secret invalidates all active tokens. Do during maintenance window.

```bash
# 1. Update secret in AWS Secrets Manager
aws secretsmanager update-secret \
  --secret-id $APP_NAME/jwt-secret \
  --secret-string "$(openssl rand -base64 32)" \
  --region $AWS_REGION

# 2. Redeploy backend service
aws ecs update-service \
  --cluster $APP_NAME-cluster \
  --service backend \
  --force-new-deployment \
  --region $AWS_REGION

# 3. All users must re-login
```

## Disaster Recovery

### RTO/RPO Targets
- **RTO** (Recovery Time Objective): < 1 hour
- **RPO** (Recovery Point Objective): < 5 minutes

### Failover Procedure

```bash
# 1. Verify RDS Multi-AZ failover status
aws rds describe-db-instances \
  --db-instance-identifier $APP_NAME-db \
  --region $AWS_REGION \
  --query 'DBInstances[0].MultiAZ'

# 2. Manual failover (if needed)
aws rds reboot-db-instance \
  --db-instance-identifier $APP_NAME-db \
  --force-failover \
  --region $AWS_REGION

# 3. Verify new primary endpoint
POSTGRES_HOST=$(aws rds describe-db-instances \
  --db-instance-identifier $APP_NAME-db \
  --region $AWS_REGION \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# 4. Update ECS task definition with new endpoint (if changed)
```

## Cost Optimization

### Recommendations
- Use Fargate Spot for non-critical services (60% cheaper)
- Set RDS max storage to auto-scale (avoid manual expansion)
- Use Reserved Instances for predictable workloads (40% discount)
- Enable S3 Intelligent-Tiering for logs (auto cost optimization)
- Review CloudWatch logs retention (default 7 days, increase only if needed)

### Budget Alert

```bash
aws budgets create-budget \
  --account-id $AWS_ACCOUNT_ID \
  --budget file://budget.json \
  --notifications-with-subscribers file://notifications.json
```

## Rollback Procedure

```bash
# 1. Identify previous task definition revision
aws ecs describe-task-definition \
  --task-definition school-ms-backend \
  --region $AWS_REGION \
  --query 'taskDefinition.revision'

# 2. Update service to use previous revision
aws ecs update-service \
  --cluster $APP_NAME-cluster \
  --service backend \
  --task-definition school-ms-backend:$(previous-revision) \
  --region $AWS_REGION

# 3. Wait for new tasks to be healthy
aws ecs wait services-stable \
  --cluster $APP_NAME-cluster \
  --services backend \
  --region $AWS_REGION
```

## Performance Tuning

### Database Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 600000
```

### Redis Cache TTL
```java
@Cacheable(value = "dashboard-summary", cacheManager = "redisCacheManager")
public Map<String, Object> getSummary() {
    // TTL: 5 minutes (configured in Spring cache config)
}
```

### Query Optimization
- Add indexes on foreign keys
- Use database query logging to identify slow queries
- Archive old activity logs annually
