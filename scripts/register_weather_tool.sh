#!/usr/bin/env bash
set -euo pipefail

HOST=${HOST:-http://localhost:8080}

cat > /tmp/weather.search.json <<'JSON'
{
  "name": "weather.search",
  "description": "查询天气（echo 测试）",
  "type": "http",
  "inputSchema": {
    "type": "object",
    "required": ["city"],
    "properties": { "city": { "type": "string" } }
  },
  "http": {
    "method": "GET",
    "url": "https://postman-echo.com/get",
    "query": { "q": "{{args.city}}" },
    "headers": { "X-Demo": "mcp-lite" },
    "timeoutMs": 3000
  }
}
JSON

curl -s -X POST "$HOST/admin/tools" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"weather.search\",\"enabled\":true,\"configJson\":$(cat /tmp/weather.search.json)}" | jq .

echo "Done."

