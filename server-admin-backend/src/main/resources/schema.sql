CREATE DATABASE IF NOT EXISTS server_admin DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE server_admin;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `role` VARCHAR(20) DEFAULT 'user',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `server` (
  `id` VARCHAR(36) PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `host` VARCHAR(100) NOT NULL,
  `port` INT DEFAULT 22,
  `username` VARCHAR(50) NOT NULL,
  `auth_type` VARCHAR(10) DEFAULT 'password',
  `password` TEXT,
  `private_key` TEXT,
  `group_name` VARCHAR(50) DEFAULT 'default',
  `status` VARCHAR(20) DEFAULT 'unknown',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 默认管理员 admin/admin123（BCrypt 加密）
INSERT IGNORE INTO `user` (`username`, `password`, `role`) VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin');
