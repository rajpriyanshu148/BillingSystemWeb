-- ============================================================
-- BillingSystem Pro — MySQL Production Setup Script
-- Run this ONCE on your MySQL server to create the database
-- Usage: mysql -u root -p < setup_mysql.sql
-- ============================================================

-- Create database
CREATE DATABASE IF NOT EXISTS billing_system
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Create dedicated app user (more secure than using root)
-- Change 'billing_password' to a strong password!
CREATE USER IF NOT EXISTS 'billing_user'@'%' IDENTIFIED BY 'billing_password';
GRANT ALL PRIVILEGES ON billing_system.* TO 'billing_user'@'%';
FLUSH PRIVILEGES;

-- Switch to the database
USE billing_system;

SELECT 'Database billing_system created successfully!' AS status;
