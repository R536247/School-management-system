-- V4: Create a local demo school with a SCHOOL_ADMIN user
WITH demo_school AS (
  INSERT INTO schools (name, subdomain, contact_email, created_at, updated_at)
  SELECT 'Demo School', 'demo', 'admin@school.com', now(), now()
  WHERE NOT EXISTS (
    SELECT 1 FROM schools WHERE subdomain = 'demo'
  )
  RETURNING id
), school AS (
  SELECT id FROM demo_school
  UNION
  SELECT DISTINCT id FROM schools WHERE subdomain = 'demo'
), demo_role AS (
  INSERT INTO roles (school_id, name, description, created_at, updated_at)
  SELECT s.id, 'SCHOOL_ADMIN', 'Administrator for Demo School', now(), now()
  FROM school s
  WHERE NOT EXISTS (
    SELECT 1 FROM roles r WHERE r.school_id = s.id AND r.name = 'SCHOOL_ADMIN'
  )
  RETURNING id
), admin_user AS (
  INSERT INTO users (school_id, email, password_hash, first_name, last_name, is_active, created_at, updated_at)
  SELECT s.id, 'admin@school.com', '$2b$12$WjVfANPbelUwLDOHYkdeKuA0qQaC69bPb1Lm.YiEy/D8SnKlnyiWy', 'Demo', 'Admin', true, now(), now()
  FROM school s
  WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.school_id = s.id AND lower(u.email) = 'admin@school.com'
  )
  RETURNING id
), user_id AS (
  SELECT id FROM admin_user
  UNION
  SELECT DISTINCT u.id FROM users u JOIN school s ON u.school_id = s.id WHERE lower(u.email) = 'admin@school.com'
), role_id AS (
  SELECT id FROM demo_role
  UNION
  SELECT DISTINCT r.id FROM roles r JOIN school s ON r.school_id = s.id WHERE r.name = 'SCHOOL_ADMIN'
)
INSERT INTO user_roles (user_id, role_id, school_id, created_at)
SELECT u.id, r.id, s.id, now()
FROM user_id u, role_id r, school s
WHERE NOT EXISTS (
  SELECT 1 FROM user_roles ur
  WHERE ur.user_id = u.id AND ur.role_id = r.id AND ur.school_id = s.id
);
