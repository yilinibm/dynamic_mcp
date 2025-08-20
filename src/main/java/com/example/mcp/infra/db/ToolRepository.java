package com.example.mcp.infra.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Repository
public class ToolRepository {
    private final JdbcTemplate jdbc;

    public ToolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ToolRow> MAPPER = new RowMapper<>() {
        @Override
        public ToolRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ToolRow(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getBoolean("enabled"),
                    rs.getString("config_json"),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }
    };

    public List<ToolRow> findAllEnabled() {
        return jdbc.query("SELECT id,name,enabled,config_json,updated_at FROM mcp_tool WHERE enabled=1", MAPPER);
    }

    public List<ToolRow> findChangedSince(Instant since) {
        return jdbc.query("SELECT id,name,enabled,config_json,updated_at FROM mcp_tool WHERE updated_at > ?",
                ps -> ps.setTimestamp(1, java.sql.Timestamp.from(since)), MAPPER);
    }

    public void upsert(String name, boolean enabled, String configJson) {
        // Try update first
        int updated = jdbc.update("UPDATE mcp_tool SET enabled=?, config_json=?, updated_at=CURRENT_TIMESTAMP WHERE name=?",
                enabled ? 1 : 0, configJson, name);
        if (updated == 0) {
            jdbc.update("INSERT INTO mcp_tool(name, enabled, config_json) VALUES (?,?,?)",
                    name, enabled ? 1 : 0, configJson);
        }
    }

    public ToolRow findByName(String name) {
        List<ToolRow> list = jdbc.query("SELECT id,name,enabled,config_json,updated_at FROM mcp_tool WHERE name=?",
                ps -> ps.setString(1, name), MAPPER);
        return list.isEmpty() ? null : list.get(0);
    }
}

