package com.example.mcp.adapter.admin;

import com.example.mcp.infra.db.ToolRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {
    private final ToolRepository repo;

    public AdminController(ToolRepository repo) {
        this.repo = repo;
    }

    public record UpsertReq(String name, boolean enabled, JsonNode configJson) {}

    @PostMapping("/tools")
    public Map<String, Object> upsert(@RequestBody UpsertReq req) {
        repo.upsert(req.name(), req.enabled(), req.configJson().toString());
        return Map.of("ok", true);
    }

    @DeleteMapping("/tools/{name}")
    public Map<String, Object> disable(@PathVariable("name") String name) {
        repo.setEnabled(name, false);
        return Map.of("ok", true);
    }
}

