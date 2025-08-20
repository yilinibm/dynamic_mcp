package com.example.mcp.core.executor;

import com.example.mcp.core.registry.ToolHandle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import feign.Client;
import feign.Request;
import feign.Response;

@Component
public class ToolExecutor {
    private final ObjectMapper om = new ObjectMapper();
    private final TemplateEngine tpl = new TemplateEngine();

    public Map<String, Object> execute(ToolHandle handle, JsonNode args) {
        String type = handle.config().type();
        try {
            return switch (type) {
                case "http" -> execHttp(handle, args);
                case "feign" -> execFeign(handle, args);
                default -> error("UNSUPPORTED_TYPE", "Unsupported type: " + type);
            };
        } catch (Exception e) {
            return error("INTERNAL", e.getMessage());
        }
    }

    private Map<String, Object> execHttp(ToolHandle handle, JsonNode args) throws Exception {
        JsonNode http = handle.config().http();
        String method = http.path("method").asText("GET");
        String urlTpl = http.path("url").asText();
        int timeoutMs = http.path("timeoutMs").asInt(3000);

        Map<String, String> secrets = System.getenv();
        String url = tpl.render(urlTpl, args, secrets);

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofMillis(timeoutMs))))
                .build();

        // headers
        HttpHeaders headers = new HttpHeaders();
        if (http.has("headers")) {
            Iterator<String> f = http.get("headers").fieldNames();
            while (f.hasNext()) {
                String k = f.next();
                String v = tpl.render(http.get("headers").get(k).asText(), args, secrets);
                headers.add(k, v);
            }
        }

        // query (simple append for GET)
        if (http.has("query")) {
            Iterator<String> f = http.get("query").fieldNames();
            StringBuilder sb = new StringBuilder(url);
            boolean first = !url.contains("?");
            while (f.hasNext()) {
                String k = f.next();
                String v = tpl.render(http.get("query").get(k).asText(), args, secrets);
                sb.append(first ? '?' : '&').append(k).append('=').append(v);
                first = false;
            }
            url = sb.toString();
        }

        WebClient.RequestBodyUriSpec uriSpec = client.method(org.springframework.http.HttpMethod.valueOf(method));
        WebClient.RequestBodySpec bodySpec = uriSpec.uri(url).headers(h -> h.addAll(headers));
        WebClient.RequestHeadersSpec<?> reqSpec;

        if (http.has("body")) {
            String bodyStr = tpl.render(http.get("body").toString(), args, secrets);
            reqSpec = bodySpec.contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(bodyStr));
        } else {
            reqSpec = bodySpec;
        }

        String resp = reqSpec.retrieve().bodyToMono(String.class).block(Duration.ofMillis(timeoutMs + 500L));
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("ok", true);
        ok.put("result", om.readTree(resp));
        return ok;
    }

    private Map<String, Object> execFeign(ToolHandle handle, JsonNode args) throws Exception {
        JsonNode feign = handle.config().feign();
        String baseUrl = feign.path("baseUrl").asText();
        String pathTpl = feign.path("path").asText();
        String method = feign.path("method").asText("GET");
        int timeoutMs = feign.path("timeoutMs").asInt(3000);
        Map<String, String> secrets = System.getenv();
        String path = tpl.render(pathTpl, args, secrets);

        // headers
        Map<String, Collection<String>> headers = new LinkedHashMap<>();
        if (feign.has("headers")) {
            Iterator<String> f = feign.get("headers").fieldNames();
            while (f.hasNext()) {
                String k = f.next();
                String v = tpl.render(feign.get("headers").get(k).asText(), args, secrets);
                headers.put(k, List.of(v));
            }
        }

        byte[] body = new byte[0];
        if (feign.has("body")) {
            String bodyStr = tpl.render(feign.get("body").toString(), args, secrets);
            body = bodyStr.getBytes(StandardCharsets.UTF_8);
            headers.putIfAbsent("Content-Type", List.of("application/json"));
        }

        Request req = Request.create(
                Request.HttpMethod.valueOf(method),
                baseUrl + path,
                headers,
                body,
                StandardCharsets.UTF_8,
                null
        );
        Client client = new feign.okhttp.OkHttpClient();
        Response resp = client.execute(req, new feign.Request.Options(timeoutMs, TimeUnit.MILLISECONDS, timeoutMs, TimeUnit.MILLISECONDS, true));
        String bodyStr = resp.body() != null ? new String(resp.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8) : "{}";
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("ok", true);
        ok.put("result", om.readTree(bodyStr));
        return ok;
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

