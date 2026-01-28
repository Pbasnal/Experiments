import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics for comparison
const errorRate = new Rate('errors');
const dodDuration = new Trend('comic_visibility_computation_duration_dod', true);
const oopDuration = new Trend('comic_visibility_computation_duration_oop', true);
const dodResponseTime = new Trend('dod_response_time', true);
const oopResponseTime = new Trend('oop_response_time', true);

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },  // Ramp up to 10 users
    { duration: '1m', target: 10 },   // Stay at 10 users
    { duration: '30s', target: 20 },  // Ramp up to 20
    { duration: '1m', target: 20 },   // Stay at 20
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    errors: ['rate<0.1'], // Error rate should be below 10%
    comic_visibility_computation_duration_dod: ['p(95)<1000'], // DoD should be under 1 second
    comic_visibility_computation_duration_oop: ['p(95)<1000'], // OOP should be under 1 second
  },
};

// Main test function
export default function () {
  const dodUrl = __ENV.DOD_API_URL || 'http://localhost:5000';
  const oopUrl = __ENV.OOP_API_URL || 'http://localhost:8080';

  // Test both APIs with the same parameters for fair comparison
  const comicId = Math.floor(Math.random() * 100) + 1; // Random comic ID between 1 and 100
  const startId = Math.floor(Math.random() * 90) + 1; // Random start ID between 1 and 90
  const limit = Math.floor(Math.random() * 10) + 1;   // Random limit between 1 and 10

  // Test DoD API
  const dodStartTime = Date.now();
  const dodRes = http.get(
    `${dodUrl}/api/comics/compute-visibilities?startId=${startId}&limit=${limit}`,
    {
      tags: { endpoint: 'compute-bulk', api: 'DOD' },
    }
  );
  const dodResponseTimeMs = Date.now() - dodStartTime;
  dodResponseTime.add(dodResponseTimeMs);
  dodDuration.add(dodResponseTimeMs);

  check(dodRes, {
    'DoD status is 200': (r) => r.status === 200,
    'DoD response time is reasonable': () => dodResponseTimeMs < 2000, // Under 2 seconds
  }) || errorRate.add(1);

  // Small delay to avoid overwhelming the system
  sleep(0.1);

  // Test OOP API with same parameters
  const oopStartTime = Date.now();
  const oopRes = http.get(
    `${oopUrl}/api/comics/compute-visibilities?startId=${startId}&limit=${limit}`,
    {
      tags: { endpoint: 'compute-bulk', api: 'OOP' },
    }
  );
  const oopResponseTimeMs = Date.now() - oopStartTime;
  oopResponseTime.add(oopResponseTimeMs);
  oopDuration.add(oopResponseTimeMs);

  check(oopRes, {
    'OOP status is 200': (r) => r.status === 200,
    'OOP response time is reasonable': () => oopResponseTimeMs < 2000, // Under 2 seconds
  }) || errorRate.add(1);

  // Log comparison if both succeeded
  if (dodRes.status === 200 && oopRes.status === 200) {
    const diff = dodResponseTimeMs - oopResponseTimeMs;
    if (Math.abs(diff) > 100) { // Only log if difference is significant (>100ms)
      console.log(
        `Comparison: DoD=${dodResponseTimeMs}ms, OOP=${oopResponseTimeMs}ms, Diff=${diff > 0 ? '+' : ''}${diff}ms`
      );
    }
  }

  sleep(2);
}
