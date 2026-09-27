# Database Design & ERD

## Entity Relationship Diagram (Text-based)

```
┌─────────────────┐
│    schools      │
│   (id, name)    │
└────────┬────────┘
         │ 1
         │
    ┌────┴──────────────────────────────────────┐
    │ N                                          │ N
    │                                            │
┌───┴──────┐    ┌──────────┐    ┌───────────┐ ┌┴─────────┐
│  users   │◄───┤roles     │───►│permissions│ │  schools │
│(auth)    │    │(RBAC)    │    │(RBAC)     │ │  (data)  │
└──────────┘    └──────────┘    └───────────┘ └──────────┘
     │ 1              │ N             │ N
     │                │               │
     │ N              │ N             │
     ├─ user_roles ───┘               │
     │                          role_permissions
     └────────────────────────────────┘

┌─────────────┐      ┌─────────────┐
│  classes    │◄─────│  sections   │
│(id, name)   │  1:N │(id, name)   │
└─────────────┘      └──────┬──────┘
                             │
                        ┌────┴────┐
                        │ 1:N      │
                    ┌───┴──────┐
                    │ students │
                    │(id, name)│
                    └──────────┘


┌──────────────┐      ┌────────────┐
│ employees    │◄─────│ attendance │
│(id, role,    │  N:1 │(date,      │
│ salary)      │      │ status)    │
└──────────────┘      └────────────┘

┌─────────────────┐
│ activity_logs   │
│(id, action,     │
│ user_id, ts)    │
└─────────────────┘
```

## Core Tables

### schools

- `id` BIGSERIAL PRIMARY KEY
- `name` TEXT NOT NULL
- `subdomain` TEXT
- `contact_email` TEXT
- `logo_path` TEXT
- `settings` JSONB
- `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE

### users

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT (nullable for Super Admin)
- `email` TEXT UNIQUE (per school)
- `password_hash` TEXT
- `first_name`, `last_name` TEXT
- `is_active` BOOLEAN DEFAULT TRUE
- `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE

### roles

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT (0 for global roles)
- `name` TEXT
- `description` TEXT
- `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE

### permissions

- `id` BIGSERIAL PRIMARY KEY
- `key` TEXT (e.g., "students.create")
- `description` TEXT

### user_roles (Join table)

- `id` BIGSERIAL PRIMARY KEY
- `user_id` BIGINT REFERENCES users
- `role_id` BIGINT REFERENCES roles
- `school_id` BIGINT (for isolation)

### role_permissions (Join table)

- `id` BIGSERIAL PRIMARY KEY
- `role_id` BIGINT REFERENCES roles
- `permission_id` BIGINT REFERENCES permissions

### classes

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT NOT NULL
- `name` TEXT
- `code` TEXT
- `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE

### sections

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT NOT NULL
- `class_id` BIGINT REFERENCES classes
- `name` TEXT
- `teacher_id` BIGINT (optional)
- `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE

### students

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT NOT NULL
- `first_name`, `last_name` TEXT
- `dob` DATE
- `gender` TEXT
- `admission_no` TEXT
- `class_id`, `section_id` BIGINT (foreign keys)
- `photo_path` TEXT
- `parent_info`, `custom_fields` JSONB
- `status` TEXT DEFAULT 'active'
- `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE

### employees

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT NOT NULL
- `user_id` BIGINT (optional, links to users if employee has login)
- `first_name`, `last_name` TEXT
- `role_title`, `department` TEXT
- `salary` NUMERIC
- `hire_date` DATE
- `status` TEXT DEFAULT 'active'
- `meta` JSONB
- `created_at`, `updated_at` TIMESTAMP WITH TIME ZONE

### attendance

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT NOT NULL
- `entity_type` TEXT ('student' or 'employee')
- `entity_id` BIGINT
- `date` DATE
- `status` TEXT ('present', 'absent', 'late')
- `meta` JSONB
- `created_at` TIMESTAMP WITH TIME ZONE

### activity_logs

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT NOT NULL
- `user_id` BIGINT
- `action` TEXT
- `entity_type` TEXT
- `entity_id` BIGINT
- `ip_address`, `user_agent` TEXT
- `details` JSONB
- `created_at` TIMESTAMP WITH TIME ZONE

### notifications

- `id` BIGSERIAL PRIMARY KEY
- `school_id` BIGINT NOT NULL
- `user_id` BIGINT
- `title`, `body` TEXT
- `channel` TEXT ('email', 'push', 'in-app')
- `read_at` TIMESTAMP WITH TIME ZONE
- `meta` JSONB
- `created_at` TIMESTAMP WITH TIME ZONE

### refresh_tokens

- `id` BIGSERIAL PRIMARY KEY
- `user_id` BIGINT
- `token_hash` TEXT
- `expires_at` TIMESTAMP WITH TIME ZONE
- `created_at` TIMESTAMP WITH TIME ZONE

## Indexing Strategy

```sql
-- Tenant isolation
CREATE INDEX idx_users_school ON users(school_id);
CREATE INDEX idx_roles_school ON roles(school_id);
CREATE INDEX idx_students_school ON students(school_id);
CREATE INDEX idx_employees_school ON employees(school_id);
CREATE INDEX idx_attendance_school ON attendance(school_id);
CREATE INDEX idx_activity_school ON activity_logs(school_id);

-- Unique constraints
CREATE UNIQUE INDEX ux_users_email_school ON users(school_id, lower(email));
CREATE UNIQUE INDEX ux_role_name_school ON roles(school_id, name);

-- Composite indexes for common queries
CREATE INDEX idx_attendance_school_date ON attendance(school_id, date);
CREATE INDEX idx_students_admission ON students(school_id, admission_no);
CREATE INDEX idx_user_roles ON user_roles(user_id, school_id);

-- Foreign keys
ALTER TABLE user_roles ADD FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_roles ADD FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE role_permissions ADD FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE role_permissions ADD FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE;
ALTER TABLE sections ADD FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE;
```

## Partitioning Strategy (Optional, for scale)

For large deployments, consider table partitioning:

- **attendance**: Partition by date range (monthly or quarterly)
- **activity_logs**: Partition by date range (monthly, archive old quarters)

Example:

```sql
CREATE TABLE attendance_2025_01 PARTITION OF attendance
  FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
```

## Data Retention Policy

- **activity_logs**: Retain for 1 year; archive older records
- **attendance**: Retain for 5 years (regulatory requirement)
- **refresh_tokens**: Delete expired tokens via batch job
- **notifications**: Delete read notifications older than 90 days
