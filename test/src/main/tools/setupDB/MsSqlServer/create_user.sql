--DROP DATABASE testiaf;
--DROP login testiaf_user;

CREATE DATABASE testiaf;
GO

USE [testiaf]
GO 

CREATE LOGIN testiaf_user WITH PASSWORD = 'testiaf_user00';
GO

CREATE USER testiaf_user FOR LOGIN testiaf_user;
GO

ALTER ROLE db_owner ADD MEMBER [testiaf_user];
GO

ALTER ROLE SqlJDBCXAUser ADD MEMBER testiaf_user;
GO