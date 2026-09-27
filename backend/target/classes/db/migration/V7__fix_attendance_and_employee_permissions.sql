-- V7: Repair missing attendance metadata column and admin permissions for employee/attendance operations
ALTER TABLE attendance
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT now();

INSERT INTO permissions (key, description) VALUES
('employees.create', 'Create employees'),
('employees.update', 'Update employees'),
('employees.delete', 'Delete employees'),
('attendance.view', 'View attendance')
ON CONFLICT (key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM roles r
JOIN permissions p ON p.key IN (
  'employees.create',
  'employees.update',
  'employees.delete',
  'attendance.view'
)
WHERE r.name = 'SCHOOL_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
