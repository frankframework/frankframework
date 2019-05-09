DROP DATABASE testiaf;
DROP login testiaf_user;

CREATE LOGIN testiaf_user WITH PASSWORD = 'testiaf_user00';
CREATE DATABASE testiaf;

GO

USE testiaf;

CREATE USER testiaf_user;
GRANT CONTROL TO testiaf_user;

GO

