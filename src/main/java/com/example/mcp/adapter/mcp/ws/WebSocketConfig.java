package com.example.mcp.adapter.mcp.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
@EnableWebFlux
public class WebSocketConfig {

    @Bean
    public SimpleUrlHandlerMapping webSocketMapping(McpWebSocketHandler handler) {
        return new SimpleUrlHandlerMapping(Map.of("/mcp/ws", handler), -1);
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() { return new WebSocketHandlerAdapter(); }
}

