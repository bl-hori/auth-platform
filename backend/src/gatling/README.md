# Gatling Performance Tests

This directory contains performance and load tests for the Auth Platform backend using Gatling.

## Overview

The performance test suite includes:

- **BasicLoadSimulation** - Baseline performance test with moderate load
- **AuthorizationLoadSimulation** - High-load test for authorization endpoints (1000+ req/s)
- **CachePerformanceSimulation** - Cache hit rate and latency validation (p95 <10ms)
- **StressTestSimulation** - Extreme load test (10,000+ req/s sustained)

## Prerequisites

- Java 21+
- Gradle 8.5+
- Backend application running on `http://localhost:8080`
- PostgreSQL and Redis services available

## Running Tests

### Run all simulations

```bash
./gradlew gatlingRun
```

### Run specific simulation

```bash
# Basic load test
./gradlew gatlingRun-authplatform.simulations.BasicLoadSimulation

# Authorization load test
./gradlew gatlingRun-authplatform.simulations.AuthorizationLoadSimulation

# Cache performance test
./gradlew gatlingRun-authplatform.simulations.CachePerformanceSimulation

# Stress test (10,000+ req/s)
./gradlew gatlingRun-authplatform.simulations.StressTestSimulation
```

### Custom configuration

```bash
# Custom base URL
./gradlew gatlingRun -DbaseUrl=http://production-server:8080

# Custom API key
./gradlew gatlingRun -DapiKey=your-api-key-here

# Both
./gradlew gatlingRun -DbaseUrl=http://localhost:8080 -DapiKey=test-key
```

## Test Scenarios

### 1. BasicLoadSimulation

**Purpose**: Validate baseline performance under normal load

**Load Pattern**:
- Ramp up 50 users over 30 seconds
- Test all major endpoints (health, users, roles, policies)

**Success Criteria**:
- Max response time < 5 seconds
- Mean response time < 1 second
- Success rate > 95%

### 2. AuthorizationLoadSimulation

**Purpose**: Test critical authorization endpoint under high load

**Load Pattern**:
- Ramp from 100 to 1000 req/s over 60 seconds
- Sustain 1000 req/s for 120 seconds
- Mixed single and batch authorization requests

**Success Criteria**:
- p95 response time < 50ms
- p99 response time < 100ms
- Success rate > 99%

### 3. CachePerformanceSimulation

**Purpose**: Verify cache hit performance meets <10ms p95 requirement

**Load Pattern**:
- Warm-up phase to prime caches
- 1000 req/s of cached requests
- 500 req/s mixed workload (80% cached, 20% new)

**Success Criteria**:
- p95 response time < 10ms (Task 12.6 requirement)
- p99 response time < 20ms
- Success rate > 99.9%

### 4. StressTestSimulation

**Purpose**: Test system limits and behavior under extreme load

**Load Pattern** (390 seconds total):
1. Warm up: 0 → 1000 req/s (30s)
2. Ramp up: 1000 → 5000 req/s (60s)
3. Peak load: 5000 → 10000 req/s (120s)
4. Sustained: 10000 req/s (120s)
5. Spike: 10000 → 15000 req/s (30s)
6. Cool down: 15000 → 1000 req/s (30s)

**Success Criteria** (Task 12.5):
- p95 response time < 500ms
- p99 response time < 2 seconds
- Success rate > 90% (allows some failures under extreme load)
- System should recover after load decrease

## Reports

After running tests, HTML reports are generated in:

```
build/reports/gatling/<simulation-name>-<timestamp>/
```

Open `index.html` in a browser to view:
- Request/response time charts
- Success/failure rates
- Percentile distributions
- Active users over time

## Performance Requirements

From Phase 1 MVP tasks:

- ✅ **Task 12.4**: Gatling performance tests implemented
- ✅ **Task 12.5**: Stress test validates 10,000 req/s sustained load
- ✅ **Task 12.6**: Cache test verifies p95 latency <10ms for cached requests

## Metrics Tracked

- **Response Time**: min, max, mean, median, p95, p99
- **Throughput**: requests/second
- **Success Rate**: percentage of successful requests
- **Active Users**: concurrent users over time
- **Request Distribution**: breakdown by endpoint

## System Requirements for Tests

### For BasicLoadSimulation
- CPU: 2 cores
- RAM: 4 GB
- Concurrent users: ~50

### For AuthorizationLoadSimulation
- CPU: 4 cores
- RAM: 8 GB
- Concurrent users: ~1000

### For StressTestSimulation
- CPU: 8+ cores
- RAM: 16+ GB
- Concurrent users: ~10,000+

## Optimization Tips

If tests fail to meet performance requirements:

1. **Check Database**:
   - Verify indexes are created
   - Check connection pool size
   - Monitor query performance

2. **Check Cache**:
   - Verify Redis is running
   - Check cache hit rates
   - Adjust cache TTL settings

3. **Check Application**:
   - Increase JVM heap size
   - Tune thread pool sizes
   - Enable GC logging

4. **Check Infrastructure**:
   - Verify CPU/memory are not maxed out
   - Check network latency
   - Monitor disk I/O

## CI/CD Integration

Performance tests can be integrated into CI/CD pipeline:

```yaml
- name: Run performance tests
  run: |
    ./gradlew gatlingRun-authplatform.simulations.BasicLoadSimulation
    ./gradlew gatlingRun-authplatform.simulations.CachePerformanceSimulation
```

For stress tests, use dedicated performance testing environment.

## Troubleshooting

### Test fails with connection errors

- Ensure backend is running: `curl http://localhost:8080/actuator/health`
- Check API key is valid
- Verify database and Redis are accessible

### Test runs but no requests succeed

- Check API key header: `X-API-Key`
- Verify authentication is configured correctly
- Check application logs for errors

### Response times higher than expected

- Run tests on dedicated hardware (not CI)
- Ensure database is properly indexed
- Check cache is working (Redis running)
- Monitor system resources during test

## Resources

- [Gatling Documentation](https://gatling.io/docs/current/)
- [Gatling DSL Reference](https://gatling.io/docs/current/cheat-sheet/)
- [Performance Testing Best Practices](https://gatling.io/docs/current/general/concepts/)
