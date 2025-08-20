package com.example.mcp.adapter.mcp.ws;

import com.example.mcp.core.executor.ToolExecutor;
import com.example.mcp.core.registry.ToolRegistry;
import com.example.mcp.core.registry.ToolHandle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Component
public class McpWebSocketHandler implements WebSocketHandler {
    private final ToolRegistry registry;
    private final ToolExecutor executor;
    private final ObjectMapper om = new ObjectMapper();

    public McpWebSocketHandler(ToolRegistry registry, ToolExecutor executor) {
        this.registry = registry;
        this.executor = executor;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<WebSocketMessage> output = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(this::route)
                .map(s -> session.textMessage(s));
        return session.send(output);
    }

    private Mono<String> route(String text) {
        try {
            JsonNode node = om.readTree(text);
            String method = node.path("method").asText();
            JsonNode id = node.path("id");
            return switch (method) {
                case "initialize" -> handleInitialize(id);
                case "tools/list" -> handleToolsList(id);
                case "tools/call" -> handleToolsCall(id, node.path("params"));
                default -> Mono.just(om.writeValueAsString(JsonRpcModels.Response.err(id, -32601, "Method not found")));
            };
        } catch (Exception e) {
            return Mono.just("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}");
        }
    }

    private Mono<String> handleInitialize(JsonNode id) throws Exception {
        Map<String, Object> serverInfo = Map.of(
                "name", "mcp-lite-java",
                "version", "0.1.0"
        );
        Map<String, Object> capabilities = Map.of("tools", true);
        JsonNode result = om.valueToTree(Map.of("serverInfo", serverInfo, "capabilities", capabilities));
        return Mono.just(om.writeValueAsString(JsonRpcModels.Response.ok(id, result)));
    }

    private Mono<String> handleToolsList(JsonNode id) throws Exception {
        JsonNode result = om.valueToTree(Map.of("tools", registry.list()));
        return Mono.just(om.writeValueAsString(JsonRpcModels.Response.ok(id, result)));
    }

    private Mono<String> handleToolsCall(JsonNode id, JsonNode params) {
        String tool = params.path("tool").asText();
        JsonNode arguments = params.path("arguments");
        Optional<ToolHandle> h = registry.get(tool);
        if (h.isEmpty()) {
            try {
                return Mono.just(om.writeValueAsString(JsonRpcModels.Response.err(id, -32004, "Tool not found")));
            } catch (Exception e) {
                return Mono.just("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}");
            }
        }
        return executor.executeReactive(h.get(), arguments)
                .flatMap(res -> {
                    if (Boolean.TRUE.equals(res.get("ok"))) {
                        JsonNode result = om.valueToTree(Map.of("content", res.get("result")));
                        try {
                            return Mono.just(om.writeValueAsString(JsonRpcModels.Response.ok(id, result)));
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                    } else {
                        Map<String, Object> err = (Map<String, Object>) res.get("error");
                        int code = -32000;
                        String msg = String.valueOf(err.get("message"));
                        try {
                            return Mono.just(om.writeValueAsString(JsonRpcModels.Response.err(id, code, msg)));
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                    }
                })
                .onErrorResume(e -> {
                    try {
                        return Mono.just(om.writeValueAsString(JsonRpcModels.Response.err(id, -32603, "Internal error")));
                    } catch (Exception ex) {
                        return Mono.just("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}");
                    }
                });
    }
}

