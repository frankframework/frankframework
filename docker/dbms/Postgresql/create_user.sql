--Prevent all connections to existing database
--Otherwise it cannot be dropped
REVOKE CONNECT ON DATABASE testiaf FROM public;
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'testiaf'
AND pid <> pg_backend_pid();
--Drop database and user
DROP DATABASE IF EXISTS testiaf;
DROP ROLE IF EXISTS testiaf_user;
--Create new database and user
CREATE DATABASE testiaf;
CREATE USER testiaf_user WITH PASSWORD 'testiaf_user00';
GRANT ALL PRIVILEGES ON DATABASE testiaf TO testiaf_user;
