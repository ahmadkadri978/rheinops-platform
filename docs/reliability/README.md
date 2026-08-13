# Reliability lab

Milestone 4 turns RheinOps into a reproducible operations exercise rather than a dashboard-only demo. The lab adds secured metrics collection, provisioned dashboards, distributed tracing, structured logs, controlled load, and reversible database failures.

## Start the lab

Requirements: Docker with Compose v2 and `curl`.

```bash
docker compose -f compose.yaml -f compose.reliability.yaml up --build -d
docker compose -f compose.yaml -f compose.reliability.yaml ps
```

Wait until `http://localhost:8080/actuator/health/liveness` returns `UP`, then open:

- Grafana dashboard: <http://localhost:3000/d/rheinops-reliability/rheinops-reliability-overview>
- Prometheus targets and alerts: <http://localhost:9090/targets> and <http://localhost:9090/alerts>
- Tempo API: <http://localhost:3200/ready>

Grafana allows anonymous local read access. Its local administrator credentials are `admin` / `admin-demo-only`. Prometheus uses the imported `rheinops-prometheus` Keycloak service account to reach the otherwise administrator-only metrics endpoint. These committed credentials are disposable lab data and must be replaced outside local Compose.

## Generate representative traffic

The steady-state scenario drives ten authenticated queue reads per second for three minutes and enforces the normal SLO thresholds:

```bash
docker compose -f compose.yaml -f compose.reliability.yaml \
  --profile load run --rm k6 run /scripts/steady-state.js
```

The spike scenario ramps from five to fifty virtual users and uses explicitly looser surge thresholds:

```bash
docker compose -f compose.yaml -f compose.reliability.yaml \
  --profile load run --rm k6 run /scripts/spike.js
```

Authentication requests are tagged separately and excluded from the application latency threshold. The reliability overlay raises the per-instance rate-limit capacity because all container traffic shares one peer address; the production limiter remains enabled.

## Run controlled failure drills

Run load in one terminal and inject one failure from another. Every drill has an explicit recovery command.

```bash
# Add 1500 ms latency and 200 ms jitter to database responses
scripts/reliability/inject-db-latency.sh

# Or make the database network path unavailable
scripts/reliability/inject-db-outage.sh

# Remove latency and re-enable the proxy
scripts/reliability/recover-database.sh
```

Use the matching runbook while observing request latency, 5xx ratio, connection-pool pressure, alerts, structured API logs, and Tempo traces. Do not run these scripts against any environment other than the local reliability overlay. They target `localhost:8474` by default and do not delete data.

## Stop the lab

```bash
docker compose -f compose.yaml -f compose.reliability.yaml down
```

Volumes are retained. Add `-v` only when you intentionally want to delete the local PostgreSQL, Prometheus, Tempo, and Grafana data.

## Evidence map

| Engineering claim | Executable evidence |
|---|---|
| Secured monitoring | Keycloak client-credentials scrape and security integration test |
| Golden signals and domain risk | Prometheus rules, provisioned Grafana dashboard, support SLA gauge |
| Trace/log correlation | W3C propagation, OTLP to Tempo, ECS JSON containing MDC `traceId`, `spanId`, and `correlationId` |
| Performance objectives | Versioned k6 steady-state and spike scenarios plus [SLO specification](slo.md) |
| Incident response | Reversible Toxiproxy drills and task-oriented [runbooks](runbooks/) |
| Learning culture | Blameless [postmortem template](postmortems/template.md) and two clearly labelled lab examples |
