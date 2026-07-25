# Performance Optimization & Tuning Guide

## Performance Benchmarks

### Baseline Metrics (Single Server, PostgreSQL on localhost)
- **Page Load**: 200-300ms (login page)
- **API Response**: 50-150ms (GET students list with 50 items)
- **Dashboard Load**: 200-400ms (aggregating counts)
- **Login Endpoint**: 150-250ms (password hash verification)
- **Database Query**: 10-50ms (indexed queries), 100-500ms (full table scans)

### Production Targets (ECS + RDS + ElastiCache)
- **P95 Response**: < 200ms
- **P99 Response**: < 500ms
- **Error Rate**: < 0.1%
- **Availability**: 99.9%

## Database Optimization

### 1. Indexing Strategy

**Required Indexes** (created in V1/V2 migrations):
```sql
-- Tenant isolation
CREATE INDEX idx_users_school ON users(school_id);
CREATE INDEX idx_students_school ON students(school_id);
CREATE INDEX idx_employees_school ON employees(school_id);
CREATE INDEX idx_attendance_school ON attendance(school_id);

-- Common queries
CREATE INDEX idx_attendance_school_date ON attendance(school_id, date);
CREATE INDEX idx_users_email ON users(email);
CREATE UNIQUE INDEX ux_users_email_school ON users(school_id, lower(email));

-- Composite indexes for frequently combined filters
CREATE INDEX idx_students_school_class ON students(school_id, class_id);
CREATE INDEX idx_sections_class ON sections(class_id);
```

**Query-Specific Indexes** (add as performance improves):
```sql
-- For sorting/pagination
CREATE INDEX idx_students_school_created ON students(school_id, created_at DESC);

-- For search queries
CREATE INDEX idx_students_name ON students USING GIN (to_tsvector('english', first_name || ' ' || last_name));
```

### 2. Query Optimization Examples

❌ **N+1 Query Problem**:
```java
// BAD: Loads all students first, then loads each class in a loop
List<Student> students = studentRepository.findAllBySchoolId(schoolId);
for (Student s : students) {
    ClassRoom c = s.getClassRoom(); // Separate query per student!
    System.out.println(c.getName());
}
```

✅ **Solution 1 - Eager Loading**:
```java
@Query("SELECT s FROM Student s " +
       "LEFT JOIN FETCH s.classRoom c " +
       "WHERE s.schoolId = :schoolId")
List<Student> findAllWithClassroom(@Param("schoolId") Long schoolId);
```

✅ **Solution 2 - Projection**:
```java
@Query("SELECT new com.schoolms.dto.StudentDTO(s.id, s.firstName, c.name) " +
       "FROM Student s LEFT JOIN s.classRoom c " +
       "WHERE s.schoolId = :schoolId")
Page<StudentDTO> findStudentDTOs(@Param("schoolId") Long schoolId, Pageable page);
```

### 3. Connection Pooling

**Application.yml Configuration**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/schoolms
    hikari:
      # Dev: 10-20 connections
      # Prod: 30-50 connections
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000  # 20 sec
      idle-timeout: 600000       # 10 min
      max-lifetime: 1800000      # 30 min
      
      # Leak detection
      leak-detection-threshold: 60000    # Alert if connection held > 60s
```

**Monitoring Pool Health**:
```bash
# View active connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'schoolms';

# View waiting connections
SELECT * FROM pg_stat_activity WHERE wait_event IS NOT NULL;
```

### 4. Slow Query Detection

**Enable Query Logging in PostgreSQL**:
```yaml
# application.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
    
# Or set in application log handler
org:
  springframework:
    boot:
      logging:
        level:
          org.springframework.data.repository: DEBUG
```

**PostgreSQL Slow Query Log**:
```sql
-- Connect as postgres admin
ALTER SYSTEM SET log_min_duration_statement = 1000;  -- Log queries > 1 second
SELECT pg_reload_conf();

-- View logs
SELECT * FROM pg_log;
```

**Analyze Slow Queries**:
```sql
-- Explain plan for slow queries
EXPLAIN ANALYZE
SELECT * FROM students 
WHERE school_id = 1 
  AND LOWER(first_name) LIKE '%john%';

-- Use EXPLAIN ANALYZE to see actual vs estimated rows
-- If seq scan appears and index available, investigate missing indexes
```

## Caching Strategy

### 1. Redis Cache Layers

**Layer 1: Permission Cache** (highest priority for speed)
```java
@Service
public class PermissionService {
    @Cacheable(value = "user-permissions", key = "#userId + '-' + #schoolId", cacheManager = "redisCacheManager")
    public boolean userHasPermission(Long userId, Long schoolId, String permission) {
        return permissionRepository.userHasPermission(userId, schoolId, permission);
    }
}

// Cache invalidation on role change
@CacheEvict(value = "user-permissions", allEntries = true)
public void assignRole(Long userId, Long roleId) {
    // ...
}
```

**Layer 2: Reference Data** (roles, permissions)
```java
@Service
public class RoleService {
    @Cacheable(value = "roles", key = "#schoolId", cacheManager = "redisCacheManager")
    public List<Role> getAllRoles(Long schoolId) {
        return roleRepository.findAllBySchoolId(schoolId);
    }
    
    @CacheEvict(value = "roles", key = "#role.schoolId")
    public Role createRole(Role role) {
        return roleRepository.save(role);
    }
}
```

**Layer 3: Dashboard Aggregations** (most expensive queries)
```java
@Service
public class DashboardService {
    @Cacheable(value = "dashboard-summary", key = "#schoolId", cacheManager = "redisCacheManager")
    public Map<String, Object> getSummary(Long schoolId) {
        Long studentCount = studentRepository.countBySchoolId(schoolId);
        Long employeeCount = employeeRepository.countBySchoolId(schoolId);
        
        return Map.of(
            "total_students", studentCount,
            "total_employees", employeeCount,
            "timestamp", LocalDateTime.now()
        );
    }
    
    // Invalidate on create/delete
    @CacheEvict(value = "dashboard-summary", key = "#schoolId")
    public Student createStudent(Student student, Long schoolId) {
        return studentRepository.save(student);
    }
}
```

### 2. Spring Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))  // Default TTL
                .disableCachingNullValues()
            )
            .withCacheConfiguration("user-permissions",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30))  // No changes often
            )
            .withCacheConfiguration("roles",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(60))
            )
            .withCacheConfiguration("dashboard-summary",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5))   // Needs freshness
            )
            .build();
    }
}
```

### 3. Cache Warming (Preload on Startup)

```java
@Component
public class CacheWarmer implements InitializingBean {
    private final RoleService roleService;
    private final PermissionService permissionService;
    
    @Override
    public void afterPropertiesSet() {
        // Preload global roles
        Long globalSchoolId = 0L;
        roleService.getAllRoles(globalSchoolId);
        
        logger.info("Cache warming completed");
    }
}
```

## Frontend Optimization

### 1. Code Splitting & Lazy Loading

```javascript
// App.jsx
import { Suspense, lazy } from 'react';

const Students = lazy(() => import('./pages/Students'));
const Employees = lazy(() => import('./pages/Employees'));
const Attendance = lazy(() => import('./pages/Attendance'));

export default function App() {
  return (
    <Routes>
      <Route path="/students" element={
        <Suspense fallback={<Loading />}>
          <Students />
        </Suspense>
      } />
      <Route path="/employees" element={
        <Suspense fallback={<Loading />}>
          <Employees />
        </Suspense>
      } />
    </Routes>
  );
}
```

### 2. Image Optimization

```javascript
// Use NextGen formats (WebP)
function StudentPhoto({ photoPath, name }) {
  return (
    <picture>
      <source srcSet={`${photoPath}.webp`} type="image/webp" />
      <img src={`${photoPath}.jpg`} alt={name} loading="lazy" />
    </picture>
  );
}

// Lazy load below-the-fold images
<img src={photoPath} alt={name} loading="lazy" width="100" height="100" />
```

### 3. API Call Optimization

```javascript
// services/api.js
import axios from 'axios';

// Reduce redundant API calls
const cache = new Map();

async function getCachedStudents(page, size) {
  const key = `students:${page}:${size}`;
  
  if (cache.has(key) && cache.get(key).expires > Date.now()) {
    return cache.get(key).data;
  }
  
  const response = await api.get('/students', { params: { page, size } });
  
  cache.set(key, {
    data: response.data,
    expires: Date.now() + 5 * 60 * 1000  // 5 min TTL
  });
  
  return response.data;
}

// Batch API calls
async function loadDashboardData() {
  const [students, employees, attendance] = await Promise.all([
    getCachedStudents(0, 100),
    api.get('/employees'),
    api.get('/attendance/today')
  ]);
  
  return { students, employees, attendance };
}
```

### 4. Building Optimization

```bash
# frontend/package.json
{
  "scripts": {
    "build": "vite build --minify=esbuild",
    "preview": "vite preview"
  },
  "devDependencies": {
    "compression-webpack-plugin": "^10.2.0"
  }
}

# frontend/vite.config.js
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['react', 'react-router-dom', 'axios'],
          'charts': ['chart.js', 'react-chartjs-2'],
          'ui': ['lucide-react']
        }
      }
    },
    chunkSizeWarningLimit: 1000,
    minify: 'esbuild'
  }
}
```

## Load Testing

### 1. Apache JMeter Test Plan

```bash
# Install JMeter
brew install jmeter

# Create test plan for Students endpoint
jmeter -n -t test-plan.jmx -l results.jtl -j jmeter.log

# View results
jmeter -g results.jtl -o report
```

**test-plan.jmx**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup">
      <elementProp name="ThreadGroup.main_controller">
        <collectionProp name="ThreadGroup.main_controller"/>
      </elementProp>
      <stringProp name="ThreadGroup.num_threads">100</stringProp>
      <stringProp name="ThreadGroup.ramp_time">30</stringProp>
      <boolProp name="ThreadGroup.scheduler">false</boolProp>
    </ThreadGroup>
    <!-- HTTP Sampler for /students endpoint -->
    <HTTPSampler guiclass="HttpTestSampleGui" testclass="HTTPSampler">
      <elementProp name="HTTPsampler.Arguments" class="Arguments">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
      <stringProp name="HTTPSampler.domain">localhost</stringProp>
      <stringProp name="HTTPSampler.port">8080</stringProp>
      <stringProp name="HTTPSampler.protocol">http</stringProp>
      <stringProp name="HTTPSampler.path">/api/v1/students?page=0&size=50</stringProp>
    </HTTPSampler>
  </hashTree>
</jmeterTestPlan>
```

### 2. Locate Load Testing

```bash
# Install locust
pip install locust

# Run with locustfile.py
locust -f locustfile.py --host=http://localhost:8080 --users=1000 --spawn-rate=10
```

**locustfile.py**:
```python
from locust import HttpUser, between, task

class StudentAPIUser(HttpUser):
    wait_time = between(1, 3)
    
    @task(3)
    def get_students(self):
        self.client.get("/api/v1/students?page=0&size=50",
            headers={"Authorization": "Bearer <token>"})
    
    @task(1)
    def get_dashboard(self):
        self.client.get("/api/v1/dashboard/summary",
            headers={"Authorization": "Bearer <token>"})
    
    def on_start(self):
        # Login first
        response = self.client.post("/api/v1/auth/login", json={
            "schoolId": 1,
            "email": "admin@school.com",
            "password": "password"
        })
        self.headers = {"Authorization": f"Bearer {response.json()['accessToken']}"}
```

### 3. Interpreting Results

**Key Metrics**:
- **Response Time (P95)**: 95% of requests complete in < X ms
- **Throughput**: Requests per second (RPS)
- **Error Rate**: % of failed requests
- **Resource Usage**: CPU, Memory, connections

**Example Output**:
```
Type      Name                Method  Count   Mean   Min   Max  P95  P99
GET       /api/v1/students    GET     10000    95    32   450  145  220
GET       /api/v1/dashboard   GET      5000   110    45   520  160  280
POST      /api/v1/auth/login  POST     1000   150    80   600  210  350

Response time summary:
Total:   16000 requests
Failure: 12 (0.075%)
P95:     180ms
```

## Monitoring in Production

### 1. Prometheus Metrics

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
        step: 1m

# Scrape in Prometheus
# prometheus.yml
scrape_configs:
  - job_name: 'school-ms-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### 2. Grafana Dashboard

```json
{
  "dashboard": {
    "title": "School MS Backend",
    "panels": [
      {
        "title": "Request Latency (P95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, http_server_requests_seconds_bucket)"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])"
          }
        ]
      },
      {
        "title": "Cache Hit Rate",
        "targets": [
          {
            "expr": "cache_gets_total - cache_puts_total / cache_gets_total"
          }
        ]
      },
      {
        "title": "Database Connections",
        "targets": [
          {
            "expr": "hikaricp_connections_active"
          }
        ]
      }
    ]
  }
}
```

### 3. Alert Rules

```yaml
# alerts.yml
groups:
  - name: school-ms
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.01
        for: 5m
        annotations:
          summary: "High error rate (> 1%)"

      - alert: SlowResponses
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 0.5
        for: 10m
        annotations:
          summary: "P95 response time > 500ms"

      - alert: LowCacheHitRate
        expr: cache_hit_ratio < 0.7
        for: 15m
        annotations:
          summary: "Cache hit rate < 70%"
```

## Scaling Checklist

- [ ] Database indexes on all foreign keys and frequently queried columns
- [ ] Redis cache configured for permissions, roles, dashboard
- [ ] Frontend code split and lazy loaded
- [ ] API pagination with default size=50, max=1000
- [ ] Load balancer configured with health checks
- [ ] Auto-scaling policies set (scale up at 70% CPU, down at 20%)
- [ ] CloudWatch alarms for latency, errors, cache hit rate
- [ ] Read replicas for reporting queries (if using RDS)
- [ ] CDN configured for static assets
- [ ] Slow query log enabled and monitored
