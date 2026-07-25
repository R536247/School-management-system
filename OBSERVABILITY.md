# Observability & Monitoring Guide

## Logging Strategy

### 1. Structured Logging with SLF4J

**Application.yml Configuration**:
```yaml
logging:
  level:
    root: INFO
    com.schoolms: DEBUG
    org.springframework.security: DEBUG
    org.springframework.data: DEBUG
    org.hibernate.SQL: DEBUG  # SQL logging
    
  pattern:
    console: "%d{ISO8601} [%thread] %-5level %logger - %msg%n"
    file: "%d{ISO8601} [%thread] %-5level %logger - %msg%n"
  
  file:
    name: logs/application.log
    max-size: 100MB
    max-history: 30
```

### 2. Correlation IDs for Request Tracing

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put("correlation_id", correlationId);
        response.setHeader("X-Correlation-ID", correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

// Add to SecurityConfig
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.addFilterBefore(new CorrelationIdFilter(), UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

**Logging with Correlation ID**:
```java
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);
    
    @GetMapping
    public ResponseEntity<Page<StudentDTO>> list(Pageable pageable) {
        logger.info("Listing students", "page_number", pageable.getPageNumber(), 
                    "page_size", pageable.getPageSize());
        // ...
    }
}

// Output: 
// 2025-01-15T10:30:45.123Z [correlation_id=abc123] INFO StudentController - Listing students page_number=0 page_size=50
```

### 3. Structured JSON Logging

**Add Logback JSON Encoder**:
```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="jsonConsole" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcX>false</includeMdcX>
            <includeTags>false</includeTags>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="jsonConsole" />
    </root>
</configuration>
```

**Output Format**:
```json
{
  "@timestamp": "2025-01-15T10:30:45.123Z",
  "message": "Listing students",
  "logger_name": "com.schoolms.controller.StudentController",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "correlation_id": "abc123",
  "page_number": 0,
  "page_size": 50
}
```

## Metrics Collection

### 1. Spring Boot Actuator

**Enable Actuator Endpoints**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,threaddump,heapdump
      base-path: /management
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
    shutdown:
      enabled: false
  health:
    circuitbreaker:
      enabled: true
    diskspace:
      threshold: 1GB
```

**Key Endpoints**:
- `/management/health` - Application health (UP, DOWN, DEGRADED)
- `/management/health/liveness` - Pod alive check (Kubernetes)
- `/management/health/readiness` - Ready to accept traffic
- `/management/metrics` - Available metrics
- `/management/prometheus` - Prometheus-formatted metrics

### 2. Micrometer Integration

**Custom Metrics**:
```java
@Component
public class StudentMetrics {
    private final MeterRegistry meterRegistry;
    
    public StudentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void recordStudentCreation(Long schoolId) {
        Counter.builder("students.created")
            .tag("school_id", schoolId.toString())
            .register(meterRegistry)
            .increment();
    }
    
    public void recordAttendanceMarked(int count) {
        meterRegistry.gauge("attendance.marked", count);
    }
}

// Usage in Service
@Service
public class StudentService {
    private final StudentMetrics metrics;
    
    public Student create(CreateStudentRequest req, Long schoolId) {
        Student student = new Student();
        // ... set fields
        student = repository.save(student);
        metrics.recordStudentCreation(schoolId);
        return student;
    }
}
```

### 3. Prometheus Scraping

**prometheus.yml**:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'school-ms'

scrape_configs:
  - job_name: 'spring-boot-app'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/management/prometheus'
    scrape_interval: 10s
    scrape_timeout: 5s
    
  - job_name: 'postgres'
    static_configs:
      - targets: ['localhost:5432']
    
  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:6379']
```

**Run Prometheus**:
```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus:latest
```

## Visualization with Grafana

### 1. Grafana Setup

```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  -e GF_SECURITY_ADMIN_PASSWORD=admin \
  grafana/grafana:latest
```

**Access**: http://localhost:3000 (admin/admin)

### 2. Create Dashboard

**Add Prometheus Data Source**:
1. Configuration → Data Sources → Add
2. Select Prometheus
3. Enter URL: http://prometheus:9090

**Import Alerting Dashboard**:
1. Create → Import
2. Upload dashboard JSON (see below)

**Dashboard JSON** (`dashboard.json`):
```json
{
  "dashboard": {
    "title": "School MS - Backend Monitoring",
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Request Rate (RPS)",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[1m])"
          }
        ],
        "yaxes": [
          {
            "label": "Requests/sec",
            "format": "short"
          }
        ]
      },
      {
        "id": 2,
        "title": "Response Latency (P95)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
          }
        ],
        "yaxes": [
          {
            "label": "Seconds",
            "format": "s"
          }
        ]
      },
      {
        "id": 3,
        "title": "Error Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])"
          }
        ],
        "thresholds": "0,0.01,0.05"  // Error if > 1%
      },
      {
        "id": 4,
        "title": "Database Connection Pool",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle"
          }
        ]
      },
      {
        "id": 5,
        "title": "Cache Hit Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "cache_hit_ratio"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "mode": "percentage",
              "steps": [
                { "color": "red", "value": 0 },
                { "color": "yellow", "value": 50 },
                { "color": "green", "value": 80 }
              ]
            }
          }
        }
      },
      {
        "id": 6,
        "title": "CPU Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "process_cpu_usage"
          }
        ],
        "yaxes": [
          {
            "label": "CPU %",
            "format": "percent"
          }
        ]
      },
      {
        "id": 7,
        "title": "Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "process_resident_memory_bytes / 1024 / 1024"
          }
        ],
        "yaxes": [
          {
            "label": "MB",
            "format": "short"
          }
        ]
      }
    ]
  }
}
```

## Alerting

### 1. Alert Rules (alerts.yml)

```yaml
groups:
  - name: school-ms-alerts
    interval: 30s
    rules:
      
      # Error Rate Alert
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate on {{ $labels.instance }}"
          description: "Error rate is {{ $value | humanizePercentage }} (threshold: 1%)"
      
      # Response Time Alert
      - alert: SlowResponses
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.5
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Slow responses on {{ $labels.instance }}"
          description: "P95 latency is {{ $value | humanizeDuration }}"
      
      # Database Connection Alert
      - alert: HighDatabaseConnections
        expr: hikaricp_connections_active > 25
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High DB connections"
          description: "{{ $value }} active connections (max pool: 30)"
      
      # Cache Hit Rate Alert
      - alert: LowCacheHitRate
        expr: cache_hit_ratio < 0.7
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "Low cache hit rate"
          description: "Cache hit rate is {{ $value | humanizePercentage }} (threshold: 70%)"
      
      # Disk Space Alert
      - alert: DiskSpaceRunningOut
        expr: disk_free_bytes / 1024 / 1024 / 1024 < 5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Low disk space"
          description: "Only {{ $value }} GB free"
      
      # Pod Restarts
      - alert: PodRestartingTooOften
        expr: rate(container_restart_count[15m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Pod restarting frequently"
          description: "Restart rate is {{ $value }}"
```

### 2. Alertmanager Configuration

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

route:
  receiver: 'default'
  group_by: ['alertname', 'cluster']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  
  routes:
    - match:
        severity: critical
      receiver: 'critical'
      continue: true

receivers:
  - name: 'default'
    slack_configs:
      - channel: '#alerts'
        title: '[{{ .Status }}] {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
  
  - name: 'critical'
    slack_configs:
      - channel: '#critical-alerts'
      - user_mentions:
          - '@devops-on-call'
    email_configs:
      - to: 'oncall@example.com'
```

### 3. Running Alertmanager

```bash
docker run -d \
  --name alertmanager \
  -p 9093:9093 \
  -v $(pwd)/alertmanager.yml:/etc/alertmanager/alertmanager.yml \
  prom/alertmanager:latest
```

## Tracing with Jaeger

### 1. Distributed Tracing Setup

**Add Dependencies** (pom.xml):
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-instrumentation-spring-webmvc-6.0</artifactId>
</dependency>
```

**Application Configuration** (application.yml):
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests
  otlp:
    tracing:
      endpoint: http://localhost:4317
```

### 2. Deploy Jaeger

```bash
docker run -d \
  --name jaeger \
  -p 5775:5775/udp \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 5778:5778 \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 14269:14269 \
  -p 14250:14250 \
  jaegertracing/all-in-one:latest
```

**Access Jaeger UI**: http://localhost:16686

## Health Checks

### 1. Liveness & Readiness Probes (Kubernetes)

**application.yml**:
```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
    circuitbreaker:
      enabled: true
```

**Kubernetes Pod Configuration** (deployment.yaml):
```yaml
spec:
  containers:
    - name: backend
      livenessProbe:
        httpGet:
          path: /management/health/liveness
          port: 8080
        initialDelaySeconds: 30
        periodSeconds: 10
        timeoutSeconds: 5
        failureThreshold: 3
      
      readinessProbe:
        httpGet:
          path: /management/health/readiness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 5
        timeoutSeconds: 3
        failureThreshold: 3
```

## Log Aggregation with ELK

### 1. Elasticsearch Setup

```bash
docker run -d \
  --name elasticsearch \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  -p 9200:9200 \
  docker.elastic.co/elasticsearch/elasticsearch:8.0.0
```

### 2. Logstash Pipeline

```conf
# logstash.conf
input {
  tcp {
    port => 5000
    codec => json
  }
}

filter {
  mutate {
    add_field => { "[@metadata][index_name]" => "school-ms-%{+YYYY.MM.dd}" }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "%{[@metadata][index_name]}"
  }
}
```

### 3. Application Logstash Integration

**Add logstash-logback-encoder**:
```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.2</version>
</dependency>
```

**logback-spring.xml**:
```xml
<appender name="stash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>logstash:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
</appender>

<root level="INFO">
    <appender-ref ref="stash" />
</root>
```

### 4. Kibana Dashboards

```bash
docker run -d \
  --name kibana \
  -p 5601:5601 \
  -e ELASTICSEARCH_HOSTS=http://elasticsearch:9200 \
  docker.elastic.co/kibana/kibana:8.0.0
```

**Access Kibana**: http://localhost:5601

## Monitoring Checklist

- [ ] Prometheus scraping all metrics
- [ ] Grafana dashboards configured and accessible
- [ ] Alert rules defined and tested
- [ ] Alertmanager receiving alerts
- [ ] Slack/email notifications working
- [ ] Jaeger tracing setup for request flow
- [ ] Correlation IDs in all logs
- [ ] ELK stack aggregating logs (optional)
- [ ] Health checks responding correctly
- [ ] Load testing completed with baseline established
