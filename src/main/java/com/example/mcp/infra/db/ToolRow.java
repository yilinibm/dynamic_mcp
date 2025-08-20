package com.example.mcp.infra.db;

import java.time.Instant;

public record ToolRow(Long id, String name, boolean enabled, String configJson, Instant updatedAt) {}

