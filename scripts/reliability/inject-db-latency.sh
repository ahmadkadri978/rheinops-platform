#!/bin/sh
set -eu

toxiproxy_url="${RHEINOPS_TOXIPROXY_URL:-http://localhost:8474}"
proxy_name="rheinops-postgres"
toxic_name="database-latency"

curl --fail --silent --show-error \
  -X DELETE "${toxiproxy_url}/proxies/${proxy_name}/toxics/${toxic_name}" >/dev/null 2>&1 || true

curl --fail --silent --show-error \
  -X POST "${toxiproxy_url}/proxies/${proxy_name}/toxics" \
  -H 'Content-Type: application/json' \
  -d '{"name":"database-latency","type":"latency","stream":"downstream","toxicity":1.0,"attributes":{"latency":1500,"jitter":200}}' \
  >/dev/null

echo "Injected 1500 ms database latency with 200 ms jitter. Run scripts/reliability/recover-database.sh to recover."
