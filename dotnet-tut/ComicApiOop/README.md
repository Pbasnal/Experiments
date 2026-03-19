# Comic API (OOP vs DOD): load tests + metrics

This repo contains two implementations of the same API:
- **OOP**: `ComicApiOop` (port **8080**)
- **DOD**: `ComicApiDod` (port **8081**)

Both are backed by MySQL and expose Prometheus metrics for Grafana dashboards.

## Run with Docker Compose

```bash
# OOP stack (MySQL + API + Prometheus + Grafana)
docker-compose up -d --build

# DOD stack (MySQL + API + Prometheus + Grafana)
docker-compose -f docker-compose.dod.yml up -d --build --force-recreate
```

## Load testing (k6)

Only these two scripts are supported:
- `k6/load-test.js`
- `k6/load-test-max-throughput.js`

```bash
# OOP
k6 run --env API_URL=http://localhost:8080 k6/load-test.js
k6 run --env API_URL=http://localhost:8080 k6/load-test-max-throughput.js

# DOD
k6 run --env API_URL=http://localhost:8081 k6/load-test.js
k6 run --env API_URL=http://localhost:8081 k6/load-test-max-throughput.js
```

## Metrics

- **Prometheus**: `http://localhost:9090`
- **Grafana**: `http://localhost:3000` (default `admin/admin`)

### Endpoints

- **OOP**: `http://localhost:8080/api/comics/compute-visibilities?startId=1&limit=20`
- **DOD**: `http://localhost:8081/api/comics/compute-visibilities?startId=1&limit=20`
