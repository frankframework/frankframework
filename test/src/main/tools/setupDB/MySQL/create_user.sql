CREATE DATABASE IF NOT EXISTS wearefrank_db;
USE wearefrank_db;

CREATE USER IF NOT EXISTS 'wearefrank_user'@'localhost' IDENTIFIED BY 'wearefrankPass01';

GRANT ALL PRIVILEGES ON *.* TO 'wearefrank_user'@'localhost';