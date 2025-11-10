import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const visibilityComputationDuration = new Trend('comic_visibility_computation_duration_oop', true);

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
    'http_req_duration{endpoint:health}': ['p(99)<100'], // Health check should be fast
    errors: ['rate<0.1'], // Error rate should be below 10%
    comic_visibility_computation_duration_oop: ['p(95)<1000'], // Visibility computation should be under 1 second
  },
};

// Main test function
export default function () {
  const baseUrl = __ENV.API_URL || 'http://localhost:8080';

  // Group 1: Health Check (20% of requests)
  if (Math.random() < 0.2) {
    const healthRes = http.get(`${baseUrl}/health`, {
      tags: { endpoint: 'health' },
    });
    
    check(healthRes, {
      'health check status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    
    sleep(1);
  }

  // Group 2: Compute Visibility for Single Comic (40% of requests)
  if (Math.random() < 0.4) {
    const comicId = Math.floor(Math.random() * 100) + 1; // Random comic ID between 1 and 100
    const startTime = Date.now();
    
    const computeRes = http.get(
      `${baseUrl}/api/comics/compute-visibilities?startId=${comicId}&limit=1`,
      {
        tags: { endpoint: 'compute-single' },
      }
    );
    
    const duration = Date.now() - startTime;
    visibilityComputationDuration.add(duration);
    
    check(computeRes, {
      'compute single status is 200': (r) => r.status === 200,
      'compute single has results': (r) => {
        const body = JSON.parse(r.body);
        return body.Results && body.Results.length > 0;
      },
      'computation duration is reasonable': () => duration < 1000, // Under 1 second
    }) || errorRate.add(1);
    
    sleep(2);
  }

  // Group 3: Compute Visibility for Multiple Comics (30% of requests)
  if (Math.random() < 0.3) {
    const startId = Math.floor(Math.random() * 90) + 1; // Random start ID between 1 and 90
    const limit = Math.floor(Math.random() * 10) + 1;   // Random limit between 1 and 10
    
    const startTime = Date.now();
    const computeBulkRes = http.get(
      `${baseUrl}/api/comics/compute-visibilities?startId=${startId}&limit=${limit}`,
      {
        tags: { endpoint: 'compute-bulk' },
      }
    );
    
    const duration = Date.now() - startTime;
    visibilityComputationDuration.add(duration);
    
    check(computeBulkRes, {
      'compute bulk status is 200': (r) => r.status === 200,
      'compute bulk has results': (r) => {
        const body = JSON.parse(r.body);
        return body.Results && body.Results.length > 0;
      },
      'compute bulk processed count matches limit': (r) => {
        const body = JSON.parse(r.body);
        return body.ProcessedSuccessfully <= limit;
      },
      'bulk computation duration is reasonable': () => duration < 2000, // Under 2 seconds
    }) || errorRate.add(1);
    
    sleep(3);
  }

  // Group 4: Invalid Requests (10% of requests)
  if (Math.random() < 0.1) {
    const invalidRes = http.get(
      `${baseUrl}/api/comics/compute-visibilities?startId=0&limit=100`,
      {
        tags: { endpoint: 'invalid-request' },
      }
    );
    
    check(invalidRes, {
      'invalid request returns 400': (r) => r.status === 400,
    }) || errorRate.add(1);
    
    sleep(1);
  }
}


