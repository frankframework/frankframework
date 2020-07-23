--DROP DATABASE testiaf;
--DROP login testiaf_user;

USE master;
GO

print 'enable jdbc XA support';
-- see https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-configure-msdtc-docker?view=sql-server-2017
EXEC sp_sqljdbc_xa_install

print 'create user testiaf_user';
CREATE LOGIN testiaf_user WITH PASSWORD = 'testiaf_user00';
GO

CREATE USER testiaf_user FOR LOGIN testiaf_user WITH DEFAULT_SCHEMA=[dbo];
GO
ALTER ROLE SqlJDBCXAUser ADD MEMBER [testiaf_user];

print 'create database testiaf'
CREATE DATABASE testiaf;
GO

ALTER AUTHORIZATION ON DATABASE::testiaf TO testiaf_user;
GO

print ''
print 'testiaf setup complete'
