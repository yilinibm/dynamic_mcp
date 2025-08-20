package com.example.mcp.core.config;

import com.example.mcp.core.registry.ToolConfig;
import com.example.mcp.core.registry.ToolHandle;
import com.example.mcp.core.registry.ToolRegistry;
import com.example.mcp.infra.db.ToolRepository;
import com.example.mcp.infra.db.ToolRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DbRefresher {
    private final ToolRepository repo;
    private final ToolRegistry registry;
    private final ObjectMapper om = new ObjectMapper();
    private volatile Instant lastSeen = Instant.EPOCH;

    public DbRefresher(ToolRepository repo, ToolRegistry registry,
                       @Value("${app.db-refresh-interval-ms:1000}") long interval) {
        this.repo = repo;
        this.registry = registry;
    }

    public void refreshNow() { doRefresh(true); }

    @Scheduled(fixedDelayString = "${app.db-refresh-interval-ms:1000}")
    public void refresh() { doRefresh(false); }

    private void doRefresh(boolean force) {
        List<ToolRow> changed = force ? repo.findAllEnabled() : repo.findChangedSince(lastSeen);
        if (changed.isEmpty() && !force) return;

        // Load all enabled to rebuild full snapshot（简单做法，便于一致性）
        List<ToolRow> all = repo.findAllEnabled();
        Map<String, ToolHandle> snap = new HashMap<>();
        for (ToolRow r : all) {
            try {
                JsonNode node = om.readTree(r.configJson());
                ToolConfig cfg = new ToolConfig(
                        node.path("name").asText(),
                        node.path("description").asText(null),
                        node.path("type").asText(),
                        node.path("inputSchema"),
                        node.path("http"),
                        node.path("feign")
                );
                snap.put(cfg.name(), new ToolHandle(cfg));
                if (r.updatedAt().isAfter(lastSeen)) {
                    lastSeen = r.updatedAt();
                }
            } catch (Exception e) {
                // skip bad row; keep old snapshot
            }
        }
        registry.replace(snap);
    }
}

