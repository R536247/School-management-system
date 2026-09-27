-- V9: Set a documented password for the local demo administrator
UPDATE users
SET password_hash = '$2y$12$hcO8aLtxfB3shx35QAgFweUVsjjuNQMs9Ah7dWcSrs/jQYxh/lduS',
    updated_at = now()
WHERE email = 'admin@school.com'
  AND school_id = (SELECT id FROM schools WHERE subdomain = 'demo');
