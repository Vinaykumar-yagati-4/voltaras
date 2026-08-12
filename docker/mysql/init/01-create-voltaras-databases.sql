-- ============================================================
-- VOLTARAS — MySQL initialization script
--
-- Mounted into /docker-entrypoint-initdb.d and executed ONCE,
-- on the first container start (empty data volume).
--
-- Creates every VOLTARAS database. The application user
-- (default: 'voltaras'@'%') is created by the official mysql
-- image from MYSQL_USER / MYSQL_PASSWORD and is granted
-- privileges here.
--
-- If you change DB_USERNAME in .env, update the GRANT
-- statements below accordingly.
-- ============================================================

CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS meter_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS meter_management_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bill_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS organization_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS complaint_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Application user privileges (user created from MYSQL_USER / MYSQL_PASSWORD)
GRANT ALL PRIVILEGES ON `auth_db`.* TO 'voltaras'@'%';
GRANT ALL PRIVILEGES ON `user_db`.* TO 'voltaras'@'%';
GRANT ALL PRIVILEGES ON `meter_db`.* TO 'voltaras'@'%';
GRANT ALL PRIVILEGES ON `meter_management_db`.* TO 'voltaras'@'%';
GRANT ALL PRIVILEGES ON `bill_db`.* TO 'voltaras'@'%';
GRANT ALL PRIVILEGES ON `organization_db`.* TO 'voltaras'@'%';
GRANT ALL PRIVILEGES ON `payment_db`.* TO 'voltaras'@'%';
GRANT ALL PRIVILEGES ON `complaint_db`.* TO 'voltaras'@'%';
GRANT ALL PRIVILEGES ON `notification_db`.* TO 'voltaras'@'%';

FLUSH PRIVILEGES;
