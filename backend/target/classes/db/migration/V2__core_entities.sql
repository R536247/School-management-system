-- V2: Core entities: classes, sections, students, employees, attendance, activity_logs, permissions, role_permissions, notifications

CREATE TABLE classes (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT NOT NULL,
  name TEXT NOT NULL,
  code TEXT,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_classes_school ON classes(school_id);

CREATE TABLE sections (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  teacher_id BIGINT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_sections_school ON sections(school_id);

CREATE TABLE students (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT NOT NULL,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  dob DATE,
  gender TEXT,
  admission_no TEXT,
  class_id BIGINT REFERENCES classes(id),
  section_id BIGINT REFERENCES sections(id),
  photo_path TEXT,
  parent_info JSONB,
  custom_fields JSONB,
  status TEXT DEFAULT 'active',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_students_school ON students(school_id);
CREATE INDEX idx_students_admission ON students(school_id, admission_no);

CREATE TABLE employees (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT NOT NULL,
  user_id BIGINT,
  first_name TEXT,
  last_name TEXT,
  role_title TEXT,
  department TEXT,
  salary NUMERIC,
  hire_date DATE,
  status TEXT DEFAULT 'active',
  meta JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_employees_school ON employees(school_id);

CREATE TABLE attendance (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id BIGINT NOT NULL,
  date DATE NOT NULL,
  status TEXT NOT NULL,
  meta JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_attendance_school_date ON attendance(school_id, date);

CREATE TABLE activity_logs (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT NOT NULL,
  user_id BIGINT,
  action TEXT NOT NULL,
  entity_type TEXT,
  entity_id BIGINT,
  ip_address TEXT,
  user_agent TEXT,
  details JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_activity_school ON activity_logs(school_id);

CREATE TABLE permissions (
  id BIGSERIAL PRIMARY KEY,
  key TEXT NOT NULL UNIQUE,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE role_permissions (
  id BIGSERIAL PRIMARY KEY,
  role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT NOT NULL,
  user_id BIGINT,
  title TEXT,
  body TEXT,
  channel TEXT,
  read_at TIMESTAMP WITH TIME ZONE,
  meta JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_notifications_school ON notifications(school_id);
