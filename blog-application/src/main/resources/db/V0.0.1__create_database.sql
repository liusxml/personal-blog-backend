-- ============================================================================
-- Personal Blog Backend - Database Creation Script
-- Version: 0.0.1
-- Author: liusxml
-- Description: Create blog_db database if not exists
-- Note: Character set utf8mb4 supports Emoji and special characters
-- ============================================================================

-- Create database with UTF8MB4 character set
CREATE
DATABASE IF NOT EXISTS blog_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Use the database
USE
blog_db;

-- Display confirmation message
SELECT 'Database blog_db created successfully!' AS status;
