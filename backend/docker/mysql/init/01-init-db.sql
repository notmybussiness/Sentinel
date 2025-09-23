-- Sentinel Development Database Initialization
-- Auto-executed when MySQL container starts

-- Set timezone
SET time_zone = '+09:00';

-- Create additional development user with full privileges
CREATE USER IF NOT EXISTS 'dev_admin'@'%' IDENTIFIED BY 'dev_admin_pass';
GRANT ALL PRIVILEGES ON sentinel_dev.* TO 'dev_admin'@'%';

-- Optimize MySQL for development
SET GLOBAL innodb_buffer_pool_size = 256*1024*1024;
SET GLOBAL max_connections = 100;

-- Create indexes for better performance (will be applied after tables are created)
-- These will be executed by Spring Boot JPA

FLUSH PRIVILEGES;

-- Log initialization
SELECT 'Sentinel MySQL Development Database Initialized Successfully' AS Status;