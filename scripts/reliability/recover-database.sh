#!/bin/sh
set -eu

toxiproxy_url="${RHEINOPS_TOXIPROXY_URL:-http://localhost:8474}"
proxy_name="rheinops-postgres"

curl --fail --silent --show-error \
  -X DELETE "${toxiproxy_url}/proxies/${proxy_name}/toxics/database-latency" \
  >/dev/null 2>&1 || true

curl --fail --silent --show-error \
  -X POST "${toxiproxy_url}/proxies/${proxy_name}" \
  -H 'Content-Type: application/json' \
  -d '{"name":"rheinops-postgres","listen":"0.0.0.0:15432","upstream":"postgres:5432","enabled":true}' \
  >/dev/null

echo "Removed database toxics and re-enabled the RheinOps database proxy."
