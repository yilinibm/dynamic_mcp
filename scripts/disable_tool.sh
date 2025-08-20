#!/usr/bin/env bash
set -euo pipefail
HOST=${HOST:-http://localhost:8888}
NAME=${NAME:-weather.search}
curl -s -X DELETE "$HOST/admin/tools/$NAME" | jq .

