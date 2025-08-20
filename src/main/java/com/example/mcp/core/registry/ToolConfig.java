package com.example.mcp.core.registry;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolConfig(
        String name,
        String description,
        String type, // http | feign
        JsonNode inputSchema,
        JsonNode http,
        JsonNode feign
) {}

