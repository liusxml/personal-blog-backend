-- 文件路径: src/test/resources/schema.sql
-- 如果表已存在，则先删除，确保每次测试都是从一个干净的状态开始
DROP TABLE IF EXISTS test_entity;
-- 创建 test_entity 表
CREATE TABLE test_entity
(
    id   BIGINT PRIMARY KEY AUTO_INCREMENT, -- 主键，自增
    name VARCHAR(255) NOT NULL              -- 名称字段
    -- 在这里添加你在 TestEntity 中定义的其他字段...
);