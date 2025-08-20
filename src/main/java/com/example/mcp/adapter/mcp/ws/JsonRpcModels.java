package com.example.mcp.adapter.mcp.ws;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonRpcModels {
    public record Request(String jsonrpc, String method, JsonNode params, JsonNode id) {}
    public record Response(String jsonrpc, JsonNode id, JsonNode result, Error error) {
        public static Response ok(JsonNode id, JsonNode result) {
            return new Response("2.0", id, result, null);
        }
        public static Response err(JsonNode id, int code, String message) {
            return new Response("2.0", id, null, new Error(code, message, null));
        }
    }
    public record Error(int code, String message, JsonNode data) {}
}

