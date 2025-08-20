#!/usr/bin/env bash
set -euo pipefail

# Start MCP Server (WebFlux) on port 8080
# Requires: Java 17+, Maven, MySQL available at 127.0.0.1:3307 with DB `mcp`

echo "[1/1] Starting Spring Boot app..."
exec mvn -q spring-boot:run

