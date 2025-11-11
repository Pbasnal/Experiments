# Comic API Performance Testing and Monitoring


## Load Testing with k6

### Prerequisites
- Docker
- Docker Compose
- k6 (https://k6.io/docs/get-started/installation/)

### Running Load Tests

1. Start the services:
```bash
# For OOP API
docker-compose up -d

# For DOD API
docker-compose -f docker-compose.dod.yml up -d
```

2. Run k6 load test:
```bash
# Test OOP API
k6 run --env API_URL=http://localhost:8080 k6/load-test.js

# Test DOD API
k6 run --env API_URL=http://localhost:8081 k6/load-test.js
```

### Test Configuration

The k6 load test script (`k6/load-test.js`) simulates various scenarios:
- Health checks
- Single comic visibility computation
- Bulk comic visibility computation
- Invalid request handling

#### Load Test Stages
- 0-30s: Ramp up to 10 users
- 30-90s: Maintain 10 users
- 90-120s: Ramp up to 20 users
- 120-180s: Maintain 20 users
- 180-210s: Ramp down to 0 users

## Performance Monitoring with Prometheus and Grafana

### Services
- **Prometheus**: Metrics collection at `http://localhost:9090`
- **Grafana**: Visualization at `http://localhost:3000`

### Metrics Tracked
- HTTP request duration
- Request counts by method and endpoint
- Request status codes
- Visibility computation performance

### Accessing Dashboards
1. Open Grafana at `http://localhost:3000`
2. Login with default credentials (usually admin/admin)
3. Navigate to "Dashboards"
   - "Comic API OOP Performance"
   - "Comic API DOD Performance"

## Comparing OOP vs DOD Performance

### Key Metrics to Compare
- Request latency
- Throughput
- Error rates
- Resource utilization

### Performance Expectations
- DOD approach should show:
  - Lower memory allocation
  - More predictable performance
  - Better cache utilization
  - Potential lower CPU usage

## Troubleshooting

### Common Issues
- Ensure Docker is running
- Check container logs: `docker-compose logs <service>`
- Verify network connectivity
- Confirm MySQL is initialized

### Performance Bottlenecks
- Database query optimization
- Connection pooling
- Caching strategies

## Advanced Monitoring

### Additional Tools
- Jaeger for distributed tracing
- Continuous profiling
- Memory allocation tracking

## Memory Metrics and Fragmentation Analysis

### Metrics Tracked
- **Allocated Memory (`dotnet_memory_allocated_bytes`)**: 
  - Total memory allocated by the application
  - Indicates memory pressure and allocation patterns
  - Helps identify potential memory leaks or excessive allocations

- **Total Memory (`dotnet_memory_total_bytes`)**: 
  - Total memory used by the application
  - Represents the overall memory footprint
  - Useful for understanding memory consumption

- **Garbage Collection Counts (`dotnet_gc_collection_count`)**: 
  - Tracks GC collections by generation (0, 1, 2)
  - Helps understand memory management efficiency
  - High collection rates may indicate memory fragmentation

### Interpreting Memory Fragmentation

#### Key Indicators
1. **Allocation vs. Total Memory Difference**
   - Large gap between allocated and total memory suggests fragmentation
   - Frequent Gen 2 collections indicate potential memory pressure

2. **Garbage Collection Patterns**
   - Frequent Gen 0 collections: Short-lived objects
   - Frequent Gen 2 collections: Long-lived objects, potential memory retention

#### Analysis Strategies
- Compare OOP vs DOD memory metrics
- Look for:
  - Total memory usage
  - Allocation rates
  - GC collection frequencies

### Recommended Tools
- Prometheus for real-time metrics
- Grafana for visualization
- dotnet-trace for detailed memory profiling
- Memory profilers like JetBrains dotMemory

### Performance Optimization Tips
- Minimize object allocations
- Use value types (structs) where possible
- Implement object pooling
- Reduce long-lived object references

### Example Analysis
```
# Low Fragmentation (Ideal)
- Stable allocated memory
- Consistent, low GC collection rates
- Small difference between allocated and total memory

# High Fragmentation (Problematic)
- Rapidly changing allocated memory
- Frequent Gen 2 collections
- Large gap between allocated and total memory
```

### Comparative Analysis: OOP vs DOD
- **OOP Approach**: 
  - More object allocations
  - Potential higher memory fragmentation
  - Complex object lifecycles

- **DOD Approach**:
  - Fewer object allocations
  - More value types
  - Simplified memory management
  - Potentially lower fragmentation

### Continuous Monitoring
- Set up alerts for:
  - Sudden memory spikes
  - Excessive GC collections
  - Memory allocation thresholds

### Contributing
Help improve our memory analysis:
- [ ] Add more detailed memory profiling scripts
- [ ] Create comparative memory analysis tools
- [ ] Develop memory optimization strategies

## Contributing

Contributions to improve load testing and performance monitoring are welcome!

### TODO
- [ ] Add more granular performance tests
- [ ] Implement continuous performance benchmarking
- [ ] Create comparative analysis scripts
