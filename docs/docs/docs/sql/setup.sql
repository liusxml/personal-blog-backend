-- =================================================================
-- Project: Personal Blog Backend
-- Description: Initial environment setup script.
--
-- Instructions:
-- 1. Connect to MySQL with a high-privilege user (e.g., 'root').
-- 2. Execute this script ONE TIME per environment (dev, test, prod).
-- 3. IMPORTANT: Update the password 'your_app_password' below.
-- =================================================================

-- Step 1: Create the database if it doesn't exist.
-- Using utf8mb4 for full Unicode support, including emojis.
CREATE DATABASE IF NOT EXISTS `blog_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Step 2: Create a dedicated user for the application.
-- This follows the Principle of Least Privilege. NEVER use 'root' for applications.
CREATE USER 'blog_user'@'localhost' IDENTIFIED BY 'your_app_password'; -- <-- !!! 请务必修改为一个强密码 !!!

-- Step 3: Grant necessary privileges to the new user on the new database.
-- The user can perform all actions (SELECT, INSERT, UPDATE, DELETE, CREATE TABLE, etc.)
-- ONLY on the 'blog_db' database.
GRANT ALL PRIVILEGES ON `blog_db`.* TO 'blog_user'@'localhost';

-- Step 4: Apply the changes.
FLUSH PRIVILEGES;

-- Verification (Optional): You can run this command to check the grants.
-- SHOW GRANTS FOR 'blog_user'@'localhost';

