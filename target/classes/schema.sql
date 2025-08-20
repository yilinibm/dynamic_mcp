CREATE TABLE IF NOT EXISTS mcp_tool (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) UNIQUE NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  config_json JSON NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- Ensure index exists (idempotent across MySQL versions)
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'mcp_tool' AND index_name = 'idx_mcp_tool_updated_at');
SET @sql := IF(@exists = 0, 'CREATE INDEX idx_mcp_tool_updated_at ON mcp_tool(updated_at)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

