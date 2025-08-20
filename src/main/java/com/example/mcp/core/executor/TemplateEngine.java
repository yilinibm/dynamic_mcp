package com.example.mcp.core.executor;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Map;

public class TemplateEngine {
    public String render(String tpl, JsonNode args, Map<String, String> secrets) {
        if (tpl == null) return null;
        String out = tpl;
        // Replace args.xxx
        if (args != null) {
            Iterator<Map.Entry<String, JsonNode>> it = args.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String key = e.getKey();
                String val = e.getValue().isTextual() ? e.getValue().asText() : e.getValue().toString();
                out = out.replace("{{args." + key + "}}", val);
            }
        }
        // Replace secrets.KEY
        if (secrets != null) {
            for (var en : secrets.entrySet()) {
                out = out.replace("{{secrets." + en.getKey() + "}}", en.getValue());
            }
        }
        return out;
    }
}

