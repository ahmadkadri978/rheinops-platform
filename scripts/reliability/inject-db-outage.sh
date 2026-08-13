#!/bin/sh
set -eu

toxiproxy_url="${RHEINOPS_TOXIPROXY_URL:-http://localhost:8474}"

curl --fail --silent --show-error \
  -X POST "${toxiproxy_url}/proxies/rheinops-postgres" \
  -H 'Content-Type: application/json' \
  -d '{"name":"rheinops-postgres","listen":"0.0.0.0:15432","upstream":"postgres:5432","enabled":false}' \
  >/dev/null

echo "Disabled the RheinOps database proxy. Run scripts/reliability/recover-database.sh to recover."
