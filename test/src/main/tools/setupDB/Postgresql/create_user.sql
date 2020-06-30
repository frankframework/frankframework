--Prevent all connections to existing database
--Otherwise it cannot be dropped
REVOKE CONNECT ON DATABASE wearefrank_db FROM public;
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'wearefrank_db'
AND pid <> pg_backend_pid();
--Drop database and user
DROP DATABASE IF EXISTS wearefrank_db;
DROP ROLE IF EXISTS testiaf_user;
--Create new database and user
CREATE DATABASE wearefrank_db;
CREATE USER testiaf_user WITH PASSWORD 'testiaf_pass01';
GRANT ALL PRIVILEGES ON DATABASE wearefrank_db TO testiaf_user;
