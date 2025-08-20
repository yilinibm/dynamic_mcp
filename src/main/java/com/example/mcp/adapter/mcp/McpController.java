package com.example.mcp.adapter.mcp;

import com.example.mcp.core.executor.ToolExecutor;
import com.example.mcp.core.registry.ToolRegistry;
import com.example.mcp.core.registry.ToolHandle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(path = "/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
public class McpController {
    private final ToolRegistry registry;
    private final ToolExecutor executor;
    private final ObjectMapper om = new ObjectMapper();

    public McpController(ToolRegistry registry, ToolExecutor executor) {
        this.registry = registry;
        this.executor = executor;
    }

    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        return Map.of("tools", registry.list());
    }

    public record CallReq(String tool, JsonNode arguments) {}

    @PostMapping("/tools/call")
    public Map<String, Object> call(@RequestBody CallReq req) {
        Optional<ToolHandle> h = registry.get(req.tool());
        if (h.isEmpty()) {
            return error("NOT_FOUND", "Tool not found: " + req.tool());
        }
        return executor.execute(h.get(), req.arguments());
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("ok", false);
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("code", code);
        e.put("message", message);
        err.put("error", e);
        return err;
    }
}

