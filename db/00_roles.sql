-- =====================================================================
-- CivicConnect - database roles (run ONCE, as an admin, BEFORE the migrations)
--   Locally : connect as CCAdmin (or postgres) in pgAdmin -> Query Tool
--   Hosted  : connect as the provider's default owner role
--
-- SECURITY: the repository is public (CO-08). The passwords below are
-- placeholders. Passwords already given to users.
-- Real values go in environment
-- variables / the platform secret store (PED Section 23.3).
-- =====================================================================

DO $$
BEGIN
  -- Owns every table; used ONLY to run migrations (DDL).
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'civicconnect_owner') THEN
    CREATE ROLE civicconnect_owner LOGIN PASSWORD 'N0t@5camm3r!'
      NOSUPERUSER NOCREATEDB NOCREATEROLE;
  END IF;

  -- Used by the running Spring Boot application. Data access only, no DDL.
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'civicconnect_app') THEN
    CREATE ROLE civicconnect_app LOGIN PASSWORD '61s&fra1d0f7'
      NOSUPERUSER NOCREATEDB NOCREATEROLE;
  END IF;

  -- Inspection / reporting tools. Reads the reporting view only.
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'civicconnect_readonly') THEN
    CREATE ROLE civicconnect_readonly LOGIN PASSWORD 'S3m1Ins@n3001'
      NOSUPERUSER NOCREATEDB NOCREATEROLE;
  END IF;
END
$$;

-- Database and schema ownership.
-- (On a hosted provider that is not a true superuser you may first need:
--  GRANT civicconnect_owner TO <your admin role>;)
ALTER DATABASE civicconnect OWNER TO civicconnect_owner;
ALTER SCHEMA public OWNER TO civicconnect_owner;

-- Nobody connects by default; grant it explicitly.
REVOKE ALL ON DATABASE civicconnect FROM PUBLIC;
GRANT CONNECT ON DATABASE civicconnect
  TO civicconnect_owner, civicconnect_app, civicconnect_readonly;

GRANT USAGE ON SCHEMA public TO civicconnect_app, civicconnect_readonly;
-- Table-level privileges are granted at the bottom of V1__init_schema.sql.
