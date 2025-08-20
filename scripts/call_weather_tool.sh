#!/usr/bin/env bash
set -euo pipefail
HOST=${HOST:-http://localhost:8080}
CITY=${CITY:-Shanghai}
curl -s -X POST "$HOST/mcp/tools/call" \
  -H 'Content-Type: application/json' \
  -d "{\"tool\":\"weather.search\",\"arguments\":{\"city\":\"$CITY\"}}" | jq .

