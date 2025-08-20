#!/usr/bin/env bash
set -euo pipefail
HOST=${HOST:-http://localhost:8080}
curl -s "$HOST/mcp/tools" | jq .

