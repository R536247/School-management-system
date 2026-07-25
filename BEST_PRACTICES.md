# Best Practices & Coding Standards

## Architecture & Layer Separation

### Controller Layer
- **Responsibility**: HTTP request/response handling, input validation, permission checks
- **Pattern**: `@RestController` with `@RequestMapping("/api/v1/resource")`
- **Rules**:
  - Always check permissions before delegating to service
  - Use DTOs (Data Transfer Objects) for request/response
  - Return appropriate HTTP status codes (200, 201, 400, 403, 404, 500)
  - Example:
    ```java
    @PostMapping
    public ResponseEntity<StudentDTO> create(@RequestBody CreateStudentRequest req) {
        if (!permissionService.userHasPermission(userId, "students.create")) {
            return ResponseEntity.status(403).build();
        }
        Student student = studentService.create(req);
        return ResponseEntity.status(201).body(toDTO(student));
    }
    ```

### Service Layer
- **Responsibility**: Business logic, multi-entity operations, permission delegation
- **Pattern**: `@Service` with `@Transactional`
- **Rules**:
  - Keep services stateless (no instance variables)
  - Use dependency injection via constructor
  - Wrap boolean operations with meaningful names (e.g., `isUserOwnerOfEntity()` not just `check()`)
  - Fetch tenant from TenantContext, not from request parameters
  - Example:
    ```java
    @Service
    public class StudentService {
        private final StudentRepository repository;
        private final ClassService classService;
        
        public StudentService(StudentRepository repo, ClassService svc) {
            this.repository = repo;
            this.classService = svc;
        }
        
        public Student create(CreateStudentRequest req) {
            // Tenant auto-set by listener
            Student s = new Student();
            s.setFirstName(req.getFirstName());
            // Validate class exists in same tenant
            ClassRoom c = classService.get(req.getClassId());
            s.setClassRoom(c);
            return repository.save(s);
        }
    }
    ```

### Repository Layer
- **Responsibility**: Database queries, pagination, filtering
- **Pattern**: Extend `JpaRepository` with custom methods for complex queries
- **Rules**:
  - Always include school_id in WHERE clause
  - Use native SQL for complex queries (stored procedures, window functions)
  - Prefer named parameters over positional
  - Example:
    ```java
    public interface StudentRepository extends JpaRepository<Student, Long> {
        Page<Student> findAllBySchoolId(Long schoolId, Pageable page);
        
        @Query(value = "SELECT * FROM students s WHERE s.school_id = :schoolId " +
               "AND LOWER(s.first_name) LIKE LOWER(:query) LIMIT 50", nativeQuery = true)
        List<Student> search(@Param("schoolId") Long schoolId, @Param("query") String query);
    }
    ```

### Entity Layer
- **Responsibility**: Data model definition, ORM mapping
- **Pattern**: Use `@Entity` with `@Data` (Lombok), extend `BaseEntity`
- **Rules**:
  - Never use mutable default arguments (e.g., `= new ArrayList<>()`)
  - Use enums for status/type fields
  - Always have `@EntityListeners(TenantEntityListener.class)` if multi-tenant
  - Example:
    ```java
    @Entity
    @Table(name = "students")
    @Data
    @EntityListeners(TenantEntityListener.class)
    public class Student extends BaseEntity {
        private String firstName;
        private String lastName;
        
        @Enumerated(EnumType.STRING)
        private StudentStatus status; // present, absent, late
        
        @Column(columnDefinition = "jsonb")
        private Map<String, Object> customFields = new HashMap<>();
    }
    ```

## Error Handling

### Global Exception Handler
- **Pattern**: Use `@RestControllerAdvice` with `@ExceptionHandler` methods
- **Rules**:
  - Map application exceptions to HTTP status codes
  - Include error ID for debugging
  - Return consistent error response format
  - Example response structure:
    ```json
    {
      "error_id": "4c54fa31-1234",
      "status": 400,
      "message": "Student not found",
      "timestamp": "2025-01-15T10:30:00Z",
      "details": {
        "student_id": 123
      }
    }
    ```

### Exception Hierarchy
```
Exception
├── ApplicationException (base, HTTP 400)
│   ├── NotFoundException (HTTP 404)
│   ├── PermissionDeniedException (HTTP 403)
│   └── ValidationException (HTTP 400)
├── TenantException (HTTP 403, tenant mismatch)
└── AuthenticationException (HTTP 401)
```

## Logging Strategy

### Log Levels
- **ERROR**: System failures, unexpected exceptions (database connection loss, NPE)
- **WARN**: Recoverable issues (deprecated API usage, missing optional field)
- **INFO**: User actions (login, create student, mark attendance)
- **DEBUG**: Detailed flow (entering method, variable values)
- **TRACE**: Never use in production

### Structured Logging
Use SLF4J with log context for correlation:
```java
// Set correlation ID at request entry point
MDCUtil.setCorrelationId(UUID.randomUUID().toString());

// Logs automatically include correlation ID
logger.info("Created student", "student_id", student.getId(), "school_id", schoolId);
// Output: [correlation_id=abc123] Created student student_id=456 school_id=789

// Clear in finally block
MDCUtil.clear();
```

## Multi-Tenancy Enforcement

### Every Query Must Filter by Tenant
✓ **Correct**:
```java
List<Student> students = studentRepository.findAllBySchoolId(TenantContext.getCurrentTenant());
```

✗ **Incorrect**:
```java
List<Student> students = studentRepository.findAll(); // BUG: crosses tenants!
```

### Every Create Must Set Tenant
✓ **Correct**:
```java
Student s = new Student();
s.setFirstName(req.getFirstName());
// TenantEntityListener auto-sets school_id from context
s = repository.save(s);
```

✗ **Incorrect**:
```java
Student s = new Student(req.getFirstName(), req.getLastName(), null); // school_id null!
```

### Service Layer Must Verify Ownership
```java
public Student get(Long studentId) {
    Optional<Student> student = repository.findById(studentId);
    if (student.isEmpty()) throw new NotFoundException("Student not found");
    
    // Verify tenant owns this entity
    if (!student.get().getSchoolId().equals(TenantContext.getCurrentTenant())) {
        throw new PermissionDeniedException("Cannot access student from other school");
    }
    return student.get();
}
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Package | lowercase, reverse domain | `com.schoolms.service` |
| Class | PascalCase | `StudentService`, `CreateStudentRequest` |
| Method | camelCase, verb-first | `createStudent()`, `getStudentsByClass()` |
| Variable | camelCase | `firstName`, `studentList` |
| Constant | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE`, `DEFAULT_EXPIRY_MINUTES` |
| Table | snake_case, plural | `students`, `user_roles` |
| Column | snake_case, singular | `first_name`, `is_active` |
| Permission key | dot notation | `students.create`, `attendance.view`, `reports.export` |
| API endpoint | kebab-case, plural resource | `/api/v1/students`, `/api/v1/attendance-records` |

## Security & Validation

### Input Validation
- Validate at controller layer before passing to service
- Use annotations: `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Pattern`
- Example:
  ```java
  @PostMapping
  public ResponseEntity<StudentDTO> create(
      @Valid @RequestBody CreateStudentRequest req) {
      // Validation happens automatically
      return ResponseEntity.ok(studentService.create(req));
  }
  
  public record CreateStudentRequest(
      @NotBlank(message = "First name required")
      String firstName,
      
      @NotBlank(message = "Email required")
      @Email(message = "Invalid email format")
      String email,
      
      @Positive(message = "Class ID must be positive")
      Long classId
  ) {}
  ```

### Password Hashing
- Always use BCryptPasswordEncoder; never store plain text
- Hashing config:
  ```java
  @Bean
  public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder(12); // strength 12
  }
  ```

### JWT Security
- Sign with strong secret (32+ random bytes)
- Short expiry (15 min for access, 7 days for refresh)
- Include tenant ID (school_id) in claims
- Never include passwords or sensitive PII
- Example claims:
  ```json
  {
    "sub": "123",
    "school_id": "789",
    "email": "user@example.com",
    "iat": 1673721600,
    "exp": 1673722500
  }
  ```

### Permission Checks
- **Method-level**: Use `@PreAuthorize("hasPermission(...)")` for automatic checks
- **Manual**: Call `permissionService.userHasPermission()` for complex logic
- **Example**:
  ```java
  @PostMapping
  @PreAuthorize("hasPermission(#id, 'Student', 'write')")
  public ResponseEntity<StudentDTO> update(
      @PathVariable Long id,
      @RequestBody UpdateStudentRequest req) {
      return ResponseEntity.ok(toDTO(studentService.update(id, req)));
  }
  ```

## Testing Guidelines

### Unit Testing
- Test business logic in isolation (mock dependencies)
- Use Mockito for mocks/stubs
- Cover happy path + edge cases + error scenarios
- Example:
  ```java
  @ExtendWith(MockitoExtension.class)
  class StudentServiceTest {
      @Mock StudentRepository studentRepository;
      @InjectMocks StudentService studentService;
      
      @Test
      void create_setsSchoolId() {
          Student result = studentService.create(request);
          assertEquals(TENANT_ID, result.getSchoolId());
      }
  }
  ```

### Integration Testing
- Test end-to-end with real database (via @SpringBootTest)
- Verify tenant isolation works
- Use @Transactional + rollback for test cleanup
- Example:
  ```java
  @SpringBootTest
  class StudentControllerIntegrationTest {
      @Test
      void create_persists() throws Exception {
          mockMvc.perform(post("/api/v1/students")
              .header("X-School-Id", "123")
              .contentType(APPLICATION_JSON)
              .content(json(request)))
              .andExpect(status().isCreated());
      }
  }
  ```

### Test Coverage Targets
- Services: 80%+ coverage, focus on business logic + security
- Controllers: 70%+ coverage, focus on happy path + error cases
- Repositories: 90%+ for custom queries
- Entities: 30%+ for validation logic only

## Code Quality Guidelines

### Readability
- Keep methods under 30 lines
- Use descriptive variable names (avoid `x`, `tmp`, `data`)
- Add comments for "why", not "what"
- Example:
  ```java
  // GOOD: Explains intent
  // Skip soft-deleted students (indicated by status='inactive') during attendance check
  List<Student> activeStudents = students.stream()
      .filter(s -> StudentStatus.ACTIVE.equals(s.getStatus()))
      .collect(toList());
  
  // BAD: Obvious from code
  List<Student> result = new ArrayList<>();
  for (Student s : students) {
      if (s.getStatus().equals("ACTIVE")) {
          result.add(s);
      }
  }
  ```

### Avoiding Technical Debt
- Don't "fix later" without a ticket
- Use `@Deprecated` with replacement example for APIs
- Refactor when ratio of new code > logic code in a method
- Add TODO comments sparingly with context

### Dependencies
- Keep Spring Boot and Maven dependencies up-to-date
- Use dependency management in parent POM to avoid version conflicts
- Avoid circular dependencies between packages

## Database Migration Guidelines

### Flyway Best Practices
- One migration per logical database change
- Use V{version}__descriptive_name.sql
- Include rollback comments (informational, Flyway doesn't auto-rollback)
- Always include IF NOT EXISTS checks
- Example:
  ```sql
  -- V5__add_student_phone.sql
  ALTER TABLE students
  ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
  
  CREATE INDEX IF NOT EXISTS idx_students_phone ON students(phone_number);
  
  -- Rollback: ALTER TABLE students DROP COLUMN phone_number;
  ```

### Zero-Downtime Migrations
- Add columns as nullable first
- Add indexes concurrently before adding constraints
- Avoid renaming tables or columns (create new, migrate data, drop old)

## Performance Considerations

### Query Optimization
- Use projections for read-heavy queries (select only needed columns)
- Eager-load relationships to avoid N+1 queries
- Use pagination for large result sets (default: size=50)
- Example:
  ```java
  // GOOD: Single query with join
  @Query("SELECT new com.schoolms.dto.StudentDTO(s.id, s.firstName, s.lastName) " +
         "FROM Student s LEFT JOIN FETCH s.classRoom " +
         "WHERE s.schoolId = :schoolId")
  Page<StudentDTO> findAll(@Param("schoolId") Long schoolId, Pageable page);
  
  // BAD: N+1 queries
  List<Student> students = repository.findAll(page);
  for (Student s : students) {
      ClassRoom c = s.getClassRoom(); // Separate query per student!
  }
  ```

### Caching Strategy
- Cache read-only data (permissions, roles)
- Cache dashboard aggregations (5-10 min TTL)
- Invalidate cache on write (use `@CacheEvict`)
- Never cache PII without encryption

### Connection Pooling
- Default: HikariCP with maxPoolSize = 20 for dev, 50 for production
- Configure via `application.yml`:
  ```yaml
  spring:
    datasource:
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 20000
  ```

## Documentation Standards

### Code Comments
- Document "why", not "what" (code shows the what)
- Use JavaDoc for public APIs
- Example:
  ```java
  /**
   * Marks attendance for students or employees.
   * Only school admins or supervisors can mark future/past attendance.
   * 
   * @param schoolId Current tenant ID
   * @param entityType 'student' or 'employee'
   * @param entityId ID of the entity
   * @param date Attendance date (can be historical)
   * @param status present, absent, or late
   * @throws PermissionDeniedException if user lacks attendance.mark permission
   */
  public void markAttendance(Long schoolId, String entityType, Long entityId, 
                              LocalDate date, AttendanceStatus status) {
  }
  ```

### API Documentation
- Use OpenAPI (Swagger) annotations
- Include request/response examples
- Document error codes and meanings

### README Requirements
- Quick start (docker-compose + build command)
- Architecture overview (links to ARCHITECTURE.md)
- Database schema (links to DATABASE_DESIGN.md)
- API documentation (links to /docs or Swagger UI)
- Deployment (links to DEPLOYMENT.md)
