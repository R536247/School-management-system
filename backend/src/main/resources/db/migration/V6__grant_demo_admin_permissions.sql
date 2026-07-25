-- V6: Grant Demo School SCHOOL_ADMIN role the existing school permissions
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, now()
FROM schools s
JOIN roles r ON r.school_id = s.id
CROSS JOIN permissions p
WHERE s.subdomain = 'demo'
  AND r.name = 'SCHOOL_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
  );
