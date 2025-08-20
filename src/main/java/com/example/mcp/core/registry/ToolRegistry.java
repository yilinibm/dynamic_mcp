package com.example.mcp.core.registry;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ToolRegistry {
    private final AtomicReference<Map<String, ToolHandle>> snapshot = new AtomicReference<>(Map.of());

    public List<Map<String, Object>> list() {
        Map<String, ToolHandle> snap = snapshot.get();
        List<Map<String, Object>> res = new ArrayList<>();
        for (ToolHandle h : snap.values()) {
            ToolConfig c = h.config();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.name());
            m.put("description", c.description());
            m.put("inputSchema", c.inputSchema());
            res.add(m);
        }
        return res;
    }

    public Optional<ToolHandle> get(String name) {
        return Optional.ofNullable(snapshot.get().get(name));
    }

    public void replace(Map<String, ToolHandle> newSnap) {
        snapshot.set(Collections.unmodifiableMap(newSnap));
    }
}

