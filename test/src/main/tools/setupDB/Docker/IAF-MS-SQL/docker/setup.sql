CREATE DATABASE wearefrank_db;
GO

USE [wearefrank_db]
GO 

CREATE LOGIN wearefrank_user WITH PASSWORD = 'wearefrankPass01';
GO

CREATE USER wearefrank_user FOR LOGIN wearefrank_user;
GO

ALTER ROLE db_owner ADD MEMBER [wearefrank_user];
GO

ALTER ROLE SqlJDBCXAUser ADD MEMBER wearefrank_user;
GO