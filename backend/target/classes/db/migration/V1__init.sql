-- Initial schema for School Management System (minimal core)
CREATE TABLE schools (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  subdomain TEXT,
  contact_email TEXT,
  logo_path TEXT,
  settings JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  school_id BIGINT,
  email TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  first_name TEXT,
  last_name TEXT,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE UNIQUE INDEX ux_users_email_school ON users (school_id, lower(email));

CREATE TABLE user_roles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  school_id BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Basic indexes for tenant isolation
CREATE INDEX idx_roles_school ON roles(school_id);
CREATE INDEX idx_user_school ON users(school_id);

-- Additional core tables will be added in next migrations
