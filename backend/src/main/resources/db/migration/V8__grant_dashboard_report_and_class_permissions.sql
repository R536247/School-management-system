-- V8: Add permissions required by the dashboard, reports, classes and sections
INSERT INTO permissions (key, description) VALUES
('dashboard.view', 'View dashboard summary'),
('reports.view', 'View reports'),
('classes.create', 'Create classes'),
('classes.view', 'View classes'),
('classes.update', 'Update classes'),
('classes.delete', 'Delete classes'),
('sections.create', 'Create sections'),
('sections.view', 'View sections'),
('sections.update', 'Update sections'),
('sections.delete', 'Delete sections')
ON CONFLICT (key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM roles r
JOIN permissions p ON p.key IN (
  'dashboard.view', 'reports.view',
  'classes.create', 'classes.view', 'classes.update', 'classes.delete',
  'sections.create', 'sections.view', 'sections.update', 'sections.delete'
)
WHERE r.name = 'SCHOOL_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );