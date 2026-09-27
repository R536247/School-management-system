# System Architecture & Design

## High-Level System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                          End Users (Browser)                        │
│                   (Students, Teachers, Admins)                      │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ HTTPS
                                 ↓
        ┌────────────────────────────────────────────────┐
        │        Frontend (React + Vite)                 │
        │  ├─ Pages: Login, Students, Employees         │
        │  ├─ Components: Layout, ProtectedRoute         │
        │  ├─ Services: Axios API client                 │
        │  └─ Utils: Auth token management               │
        └────────────────┬─────────────────────────────┘
                         │ HTTPS / REST API
                         ↓
        ┌────────────────────────────────────────────────┐
        │         Backend (Spring Boot 3.1.2)            │
        │                                                │
        │  ┌────────────────────────────────────────┐   │
        │  │  Controllers (REST endpoints)           │   │
        │  │  ├─ AuthController                      │   │
        │  │  ├─ StudentController                   │   │
        │  │  ├─ EmployeeController                  │   │
        │  │  ├─ AttendanceController                │   │
        │  │  └─ DashboardController                 │   │
        │  └────────────────────────────────────────┘   │
        │           │                                    │
        │           ↓                                    │
        │  ┌────────────────────────────────────────┐   │
        │  │  Security & Filters                     │   │
        │  │  ├─ JwtAuthenticationFilter             │   │
        │  │  ├─ TenantFilter (Multi-tenancy)        │   │
        │  │  ├─ SecurityConfig                      │   │
        │  │  └─ CorrelationIdFilter                 │   │
        │  └────────────────────────────────────────┘   │
        │           │                                    │
        │           ↓                                    │
        │  ┌────────────────────────────────────────┐   │
        │  │  Services (Business Logic)              │   │
        │  │  ├─ StudentService                      │   │
        │  │  ├─ EmployeeService                     │   │
        │  │  ├─ AttendanceService                   │   │
        │  │  ├─ PermissionService (RBAC)            │   │
        │  │  ├─ DashboardService                    │   │
        │  │  └─ Caching logic                       │   │
        │  └────────────────────────────────────────┘   │
        │           │                                    │
        │           ↓                                    │
        │  ┌────────────────────────────────────────┐   │
        │  │  Repositories (Data Access)             │   │
        │  │  ├─ StudentRepository                   │   │
        │  │  ├─ EmployeeRepository                  │   │
        │  │  ├─ PermissionRepository                │   │
        │  │  └─ [Native SQL queries]                │   │
        │  └────────────────────────────────────────┘   │
        │                                                │
        └────────────────┬───────────────────┬──────────┘
                         │                   │
                         │ JDBC              │ Redis Protocol
                         ↓                   ↓
        ┌──────────────────────┐   ┌────────────────────┐
        │  PostgreSQL 15       │   │  Redis 7           │
        │  ├─ schools          │   │  ├─ Permissions    │
        │  ├─ users            │   │  ├─ Roles          │
        │  ├─ students         │   │  └─ Dashboard      │
        │  ├─ employees        │   │     (Summary cache)│
        │  ├─ attendance       │   └────────────────────┘
        │  ├─ roles            │
        │  ├─ permissions      │
        │  └─ Tables (12 total)│
        └──────────────────────┘
```

## Component Architecture

### 1. Frontend Layer (React + Vite)

**Purpose**: User interface for all school system interactions

**Key Components**:

```
frontend/src/
├── pages/
│   ├── Login.jsx          # Authentication entry point
│   ├── Students.jsx       # Student list & management
│   ├── Employees.jsx      # Employee list (future)
│   └── Attendance.jsx     # Attendance marking (future)
├── components/
│   ├── Layout.jsx         # Sidebar + topbar layout
│   ├── ProtectedRoute.jsx # Route authentication guard
│   └── [UI components]
├── services/
│   └── api.js             # Axios HTTP client with interceptors
├── utils/
│   └── auth.js            # Token management (localStorage)
└── App.jsx                # Router configuration
```

**Technology Stack**:

- React 18.2.0 (component framework)
- React Router 6.14.1 (client-side routing)
- Axios 1.4.0 (HTTP client)
- Tailwind CSS 3.4.7 (styling)
- React Hook Form 7.45.1 (form state management)

**Authentication Flow**:

```
User Input (email, password, schoolId)
    ↓
Axios POST /api/v1/auth/login
    ↓
Backend validates credentials
    ↓
Backend returns { accessToken, refreshToken }
    ↓
Frontend stores tokens (localStorage)
    ↓
Axios interceptor auto-attaches Authorization header + X-School-Id
    ↓
Subsequent requests authenticated
    ↓
401 response → redirect to /login (interceptor)
```

### 2. Backend Layer (Spring Boot)

**Purpose**: REST API, business logic, data validation, security

**Architecture Layers**:

```
HTTP Request
    ↓
┌─────────────────────────────┐
│ Servlet Filters             │
│ ├─ CorrelationIdFilter      │ (Add trace ID)
│ ├─ TenantFilter             │ (Extract X-School-Id, set context)
│ └─ JwtAuthenticationFilter  │ (Validate JWT token)
└────────┬────────────────────┘
         ↓
┌─────────────────────────────┐
│ DispatcherServlet           │
│ (Route to Controller)       │
└────────┬────────────────────┘
         ↓
┌─────────────────────────────┐
│ Controller Layer            │
│ ├─ Map HTTP → Method calls  │
│ ├─ Validate permissions     │
│ ├─ Validate input (DTOs)    │
│ └─ Call Service layer       │
└────────┬────────────────────┘
         ↓
┌─────────────────────────────┐
│ Service Layer               │
│ ├─ Business logic           │
│ ├─ Multi-entity operations  │
│ ├─ Transaction management   │
│ └─ Call Repository layer    │
└────────┬────────────────────┘
         ↓
┌─────────────────────────────┐
│ Repository Layer            │
│ ├─ JPA entity queries       │
│ ├─ Tenant filtering         │
│ └─ Native SQL (complex)     │
└────────┬────────────────────┘
         ↓
┌─────────────────────────────┐
│ Database │ Cache            │
│ (PostgreSQL │ Redis)        │
└─────────────────────────────┘
```

**Key Packages**:

- `com.schoolms.entity` - JPA entities with tenant support
- `com.schoolms.repository` - Spring Data JPA repositories
- `com.schoolms.service` - Business logic
- `com.schoolms.controller` - REST endpoints
- `com.schoolms.security` - JWT, permissions, auth
- `com.schoolms.tenant` - Multi-tenancy enforcement
- `com.schoolms.config` - Application configuration
- `com.schoolms.exception` - Global exception handling

### 3. Database Layer (PostgreSQL + Flyway)

**Schema Design** (see DATABASE_DESIGN.md):

**Core Tables**:

1. **schools** - Tenant records (one per school)
2. **users** - Login accounts (shared or per-school)
3. **roles** - Permission groups (SUPER_ADMIN, SCHOOL_ADMIN, TEACHER, EMPLOYEE)
4. **permissions** - Granular actions (students.create, attendance.mark, etc.)
5. **user_roles** - Many-to-many: user → role (per school)
6. **role_permissions** - Many-to-many: role → permission
7. **students** - Student records (with class, section, photo, metadata)
8. **employees** - Staff records (with role, department, salary)
9. **classes** - Grade levels (Class 10, Class 11, etc.)
10. **sections** - Class divisions (Section A, Section B, etc.)
11. **attendance** - Daily attendance tracking
12. **activity_logs** - Audit trail of actions

**Migrations**:

- **V1\_\_init.sql** - Base schema (schools, users, roles, permissions)
- **V2\_\_core_entities.sql** - Student/employee/attendance tables
- **V3\_\_rbac_seed.sql** - Initial role and permission data

### 4. Caching Layer (Redis)

**Multi-Level Cache Strategy**:

```
Request
  ↓
┌─────────────────────────┐
│ Cache L1: In-Memory     │ (Java Object cache)
│ ├─ Very short TTL (1m)  │
│ └─ For repeated queries │
└────────┬────────────────┘
         │ Miss
         ↓
┌─────────────────────────┐
│ Cache L2: Redis         │ (Distributed cache)
│ ├─ user-permissions     │ (30 min TTL)
│ ├─ roles                │ (60 min TTL)
│ ├─ dashboard-summary    │ (5 min TTL)
└────────┬────────────────┘
         │ Miss
         ↓
┌─────────────────────────┐
│ Database Query          │
│ (PostgreSQL)            │
└────────┬────────────────┘
         │ Result
         ↓
┌─────────────────────────┐
│ Update Redis Cache      │
│ with TTL                │
└─────────────────────────┘
```

**Cache Annotations**:

- `@Cacheable` - Check cache before query
- `@CacheEvict` - Remove entry on write operations
- `@Caching` - Multiple cache operations in one method

### 5. Multi-Tenancy Architecture

**Tenant Isolation Strategy**:

```
Request Headers:
├─ Authorization: Bearer <JWT>
├─ X-School-Id: 123          ← Tenant identifier
└─ X-Correlation-ID: abc123

↓

TenantFilter (HttpFilter):
├─ Extract X-School-Id
├─ Validate tenant in JWT
├─ Set TenantContext.setCurrentTenant(schoolId)
└─ Add to request attributes

↓

TenantContext (ThreadLocal):
├─ getCurrentTenant() → Long schoolId
└─ Clear after request

↓

Repository Queries:
├─ All queries include: WHERE school_id = ?
├─ auto-set on insert via TenantEntityListener
└─ Service layer verifies ownership

Result: Strict tenant isolation
└─ Student 456 in School 1 cannot access data from School 2
└─ Data leakage prevented at multiple layers
```

### 6. RBAC Implementation

**Permission Enforcement Flow**:

```
Controller Method:
@PostMapping
@PreAuthorize("hasPermission(#id, 'Student', 'write')")
public ResponseEntity<StudentDTO> update(@PathVariable Long id, ...) {
    // Only reaches here if permission check passes
}

↓

CustomPermissionEvaluator (Spring Security):
├─ Parse #id → Student entity
├─ Parse permission string → 'Student.write'
├─ Call PermissionService.userHasPermission(userId, 'Student.write')
└─ Return boolean

↓

PermissionService:
├─ Get current tenant from TenantContext
├─ Query permission repository:
│  SELECT EXISTS (
│    SELECT 1 FROM user_roles ur
│    JOIN role_permissions rp ON ur.role_id = rp.role_id
│    JOIN permissions p ON rp.permission_id = p.id
│    WHERE ur.user_id = ? AND p.key = ?
│      AND ur.school_id = ?
│  )
└─ Return result (cached via Redis)

↓

Result: true → Continue
Result: false → 403 Forbidden (GlobalExceptionHandler)
```

**Permission Keys** (namespace.action):

```
students.*
├─ students.view    - List/get students
├─ students.create  - Create new student
├─ students.update  - Modify student
└─ students.delete  - Remove student

employees.*
├─ employees.view
├─ employees.create
├─ employees.update
└─ employees.delete

attendance.*
└─ attendance.mark  - Mark attendance

reports.*
├─ reports.view
└─ reports.export
```

**Built-in Roles** (V3 seed data):

1. **SUPER_ADMIN** - All permissions across all schools
2. **SCHOOL_ADMIN** - All permissions within one school
3. **TEACHER** - View students, mark attendance, view reports
4. **EMPLOYEE** - View own information only

## API Design

### RESTful Endpoint Structure

**Versioning**: `/api/v1/`

**Base Resources**:

```
POST   /api/v1/auth/login                    - Login (credentials → JWT)
GET    /api/v1/students                      - List (paginated, filtered by school_id)
POST   /api/v1/students                      - Create (permission gated)
GET    /api/v1/students/{id}                 - Get single
PUT    /api/v1/students/{id}                 - Update
DELETE /api/v1/students/{id}                 - Delete

GET    /api/v1/employees                     - Employee list
POST   /api/v1/employees                     - Create employee
GET    /api/v1/employees/{id}                - Get employee
PUT    /api/v1/employees/{id}                - Update employee
DELETE /api/v1/employees/{id}                - Delete employee

GET    /api/v1/classes                       - List all classes
POST   /api/v1/classes                       - Create class
PUT    /api/v1/classes/{id}                  - Update class
DELETE /api/v1/classes/{id}                  - Delete class

GET    /api/v1/sections                      - List sections
POST   /api/v1/sections                      - Create section
PUT    /api/v1/sections/{id}                 - Update section
DELETE /api/v1/sections/{id}                 - Delete section

POST   /api/v1/attendance/mark               - Mark attendance
GET    /api/v1/attendance?date=2025-01-15    - Get attendance for date

GET    /api/v1/dashboard/summary             - Dashboard aggregates (cached)
```

### Request/Response Format

**Login Request**:

```json
{
  "schoolId": 1,
  "email": "teacher@school.com",
  "password": "SecurePassword123"
}
```

**Login Response** (200):

```json
{
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 900
}
```

**Student List** (200):

```json
{
  "content": [
    {
      "id": 1,
      "firstName": "John",
      "lastName": "Doe",
      "admissionNo": "2024001",
      "status": "active",
      "createdAt": "2025-01-10T10:30:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 50
}
```

**Error Response** (403):

```json
{
  "error_id": "4c54fa31-1234",
  "status": 403,
  "message": "Access denied",
  "timestamp": "2025-01-15T10:30:00Z",
  "path": "/api/v1/students"
}
```

## Security Architecture

### Authentication Flow (JWT)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. User submits credentials                                 │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────────────────────┐
│ AuthController.login()                                      │
│ ├─ Validate schoolId exists                                 │
│ ├─ Find user by email + schoolId                            │
│ ├─ Hash submitted password, compare to stored hash          │
│ └─ On success → Generate tokens                             │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────────────────────┐
│ JwtUtil.generateAccessToken()                               │
│ ├─ Create JWT claims:                                       │
│ │  ├─ sub: userId                                           │
│ │  ├─ school_id: tenantId                                   │
│ │  ├─ iat: issuedAt                                         │
│ │  ├─ exp: now + 15 minutes                                 │
│ │  └─ email: user email                                     │
│ ├─ Sign with HS256 (JWT_SECRET)                             │
│ └─ Return Base64 encoded JWT                                │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────────────────────┐
│ Frontend receives tokens                                    │
│ ├─ localStorage.setItem('accessToken', token)               │
│ └─ Optional: Set refreshToken in httpOnly cookie            │
└──────────────────┬──────────────────────────────────────────┘
                   │
        ↓ Subsequent Request ↓
┌─────────────────────────────────────────────────────────────┐
│ Axios interceptor adds headers                              │
│ ├─ Authorization: Bearer <accessToken>                      │
│ └─ X-School-Id: <schoolId from localStorage>                │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────────────────────┐
│ Backend receives request                                    │
│ ├─ JwtAuthenticationFilter extracts token from header       │
│ ├─ JwtUtil.validateToken(token)                             │
│ ├─ Verify signature valid                                   │
│ ├─ Verify expiry > now                                      │
│ ├─ Extract userId + schoolId from claims                    │
│ └─ Set SecurityContext + TenantContext                      │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ↓ Allow Request:
         Response 200 with data

         OR

                   ↓ Token Invalid/Expired:
         JwtAuthenticationFilter catches exception
         Response 401 Unauthorized
         Frontend receives 401 → Axios interceptor redirects to /login
```

### Password Security

```
User registers/changes password:
    ↓
Plain text password submitted via HTTPS
    ↓
BCryptPasswordEncoder.encode(password) with strength=12
    ├─ Generate random salt
    ├─ Hash with bcrypt 12 rounds (~100ms per hash)
    └─ Store hash in database (never store plain text!)
    ↓
On login:
    ├─ User submits plain text password
    └─ Compare: BCryptPasswordEncoder.matches(submitted, stored_hash)
       ├─ Results match → Allow login
       └─ No match → Reject login
```

## Deployment Architecture

### Local Development

```
Developer Machine:
├─ Docker (running Postgres + Redis)
├─ Java 17 + Maven (running Spring Boot)
├─ Node.js + npm (running Vite dev server)
└─ Browser (accessing http://localhost:3000)
```

### Production (AWS)

```
┌─────────────────────────────────┐
│ CloudFront CDN                  │
│ (Static assets cached)          │
└────────┬────────────────────────┘
         │
┌────────▼────────────────────────┐
│ Application Load Balancer       │
│ (SSL/TLS termination)           │
│ (Health check: /management/health)
└─────┬──────────────┬────────────┘
      │              │
      │ Multi-AZ     │ Multi-AZ
      ↓              ↓
┌─────────────┐  ┌─────────────┐
│ ECS Fargate │  │ ECS Fargate │
│ Backend     │  │ Backend     │
│ (2-10 tasks)│  │ (2-10 tasks)│
└─────────────┘  └─────────────┘
      │              │
      └──────┬───────┘
             ↓
       Multi-Region
       │              │
       ↓              ↓
    ┌─────────────────┐    ┌──────────────┐
    │ RDS PostgreSQL  │    │ ElastiCache  │
    │ Multi-AZ        │    │ Redis Cluster│
    │ Automated Backup│    │ Multi-node   │
    └─────────────────┘    └──────────────┘
```

## Scaling Strategy

### Horizontal Scaling (Adding servers)

**Frontend**:

- CDN handles static assets globally
- Multiple instances behind ALB
- Scale based on CPU/memory (ECS auto-scaling)

**Backend**:

- Stateless design (no session affinity needed)
- Scale independently based on:
  - CPU utilization > 70%
  - Memory usage > 80%
  - Request rate: +1 instance per 1000 RPS
- Max instances: 10 per region (cost optimization)

**Database**:

- RDS Multi-AZ (automatic failover)
- Read replicas for reporting queries
- Connection pooling (HikariCP 20-50 connections)
- Query optimization to reduce load

**Cache**:

- Redis Multi-node cluster (automatic sharding)
- TTL strategy prevents stale data
- Cache invalidation on writes (immediate consistency)

### Vertical Scaling (Bigger servers)

**When to scale vertically**:

- Single instance near CPU limit but request rate low (code optimization needed)
- Database needs more RAM for working set
- Memory leaks or unoptimized queries (fix first)

**Architecture preserves vertical scaling**:

- No hard-coded instance limits
- Can upgrade ECS task CPU/memory via task definition
- Can upgrade RDS instance class (Multi-AZ restart required)

## Disaster Recovery

**RTO/RPO Targets**:

- RTO: < 1 hour (Recover Time Objective)
- RPO: < 5 minutes (Recovery Point Objective)

**Multi-Region Failover** (optional, for very high availability):

```
Primary Region (US-East-1)
├─ ECS tasks + RDS + ElastiCache
└─ DynamoDB Global Tables (optional)

Failover Region (US-West-2)
├─ Standby ECS cluster
├─ RDS read replica (can promote to primary)
└─ Redis replication
```

**Backup Strategy**:

- RDS automated backups: 30 days retention
- Manual snapshots: Before major deployments
- Cross-region snapshot copy (optional)
- Point-in-time recovery: Available for 35 days

## Observability

**Logging**:

- Structured JSON logs to ELK stack
- Correlation IDs for request tracing
- Log levels: ERROR/WARN/INFO/DEBUG

**Metrics**:

- Prometheus scraping Micrometer metrics
- Grafana dashboards for visualization
- Custom metrics: students created, attendance marked, etc.

**Tracing**:

- Jaeger for distributed tracing
- Identify slow services in service mesh
- Debug latency issues

**Alerting**:

- Alert on high error rate (> 1%)
- Alert on slow responses (P95 > 500ms)
- Alert on low cache hit rate (< 70%)
- Alert on high DB connections (> 25)
- Slack + email notifications

## Summary

This architecture provides:

1. **Scalability**: Stateless design, distributed caching, database optimization
2. **Security**: JWT auth, RBAC permissions, encrypted secrets, audit logs
3. **Reliability**: Multi-AZ deployment, automated failover, comprehensive monitoring
4. **Performance**: Multi-level caching, query optimization, CDN for static assets
5. **Maintainability**: Clear layer separation, testable code, comprehensive documentation
6. **Flexibility**: Can deploy locally or on any cloud provider (AWS, GCP, Azure)
7. **Cost-optimization**: Spot instances, auto-scaling, cache warming strategies
