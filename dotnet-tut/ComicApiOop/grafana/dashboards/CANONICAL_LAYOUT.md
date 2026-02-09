# Canonical dashboard layout (DOD vs OOP comparison)

Same panel order and grid in both dashboards. Row height = 8.

| id | Title                     | gridPos (x, y, w, h) |
|----|---------------------------|----------------------|
| 1  | API Throughput            | (0, 0, 12, 8)        |
| 2  | API Latency               | (12, 0, 12, 8)       |
| 3  | HTTP Status Codes         | (0, 8, 12, 8)        |
| 4  | Total request latency     | (12, 8, 12, 8)       |
| 5  | Garbage Collection        | (0, 16, 12, 8)       |
| 6  | Memory allocation         | (12, 16, 12, 8)      |
| 7  | Request wait time         | (0, 24, 24, 8)       |
| 8  | DB / EF query duration    | (0, 32, 12, 8)       |
| 9  | DB / EF query count       | (12, 32, 12, 8)      |
| 10 | EF Core change tracker    | (0, 40, 12, 8)       |
| 11 | Memory per operation      | (12, 40, 12, 8)      |
| 12+| Implementation-specific  | (0, 48, ...)         |

Datasource: uid "Prometheus", type "prometheus".
