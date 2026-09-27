-- V3: Seed built-in roles and sample permissions
INSERT INTO permissions (key, description) VALUES
('students.create','Create students'),
('students.view','View students'),
('students.update','Update students'),
('students.delete','Delete students'),
('employees.create','Create employees'),
('employees.view','View employees'),
('employees.update','Update employees'),
('employees.delete','Delete employees'),
('attendance.view','View attendance'),
('attendance.mark','Mark attendance')
ON CONFLICT (key) DO NOTHING;

INSERT INTO roles (school_id, name, description, created_at) VALUES
(0, 'SUPER_ADMIN', 'System super administrator', now()),
(0, 'SCHOOL_ADMIN', 'Administrator for a single school', now()),
(0, 'TEACHER', 'Teacher role', now()),
(0, 'EMPLOYEE', 'General employee', now())
ON CONFLICT DO NOTHING;

-- Link basic permissions to SCHOOL_ADMIN and TEACHER by name lookup
-- Note: role_permissions linking performed in application bootstrap if needed
