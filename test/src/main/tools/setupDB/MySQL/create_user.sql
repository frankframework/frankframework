DROP DATABASE wearefrank_db;
CREATE DATABASE wearefrank_db;
USE testiaf;

DROP USER 'wearefrank_user'@'localhost';

CREATE USER 'wearefrank_user'@'localhost' IDENTIFIED BY 'wearefrankPass01';

GRANT ALL PRIVILEGES ON *.* TO 'wearefrank_user'@'localhost';