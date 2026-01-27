import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

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
    comic_visibility_computation_duration: ['p(95)<1000'], // Visibility computation should be under 1 second
  },
};

// Metrics for tracking visibility computation performance
const visibilityComputationDuration = new Trend('comic_visibility_computation_duration', true);

// Main test function
export default function () {
  const baseUrl = __ENV.API_URL || 'http://localhost:8081';

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
        timeout: '10s', // Explicit timeout
      }
    );
    
    const duration = Date.now() - startTime;
    visibilityComputationDuration.add(duration);
    
    // Helper to safely parse and handle both camelCase and PascalCase
    const parseResponse = (r) => {
      try {
        return JSON.parse(r.body);
      } catch (e) {
        console.error(`Failed to parse response: ${r.body.substring(0, 200)}`);
        return null;
      }
    };
    
    // Helper to get results array (handles both casing)
    const getResults = (body) => {
      return body?.Results || body?.results || [];
    };
    
    const body = parseResponse(computeRes);
    const results = getResults(body);
    
    const checks = check(computeRes, {
      'compute single status is 200': (r) => r.status === 200,
      'compute single not timeout': (r) => r.status !== 504,
      'compute single has results': () => {
        if (!body) return false;
        const hasResults = results.length > 0;
        if (!hasResults) {
          console.warn(`No results for comicId ${comicId}. Response: ${JSON.stringify(body).substring(0, 300)}`);
        }
        return hasResults;
      },
      'compute single has computed visibilities': () => {
        if (!body || results.length === 0) return false;
        // Each result should have computed visibilities (handle both casing)
        const allHaveVisibilities = results.every(result => {
          const visibilities = result.ComputedVisibilities || result.computedVisibilities || [];
          return visibilities.length > 0;
        });
        if (!allHaveVisibilities) {
          console.warn(`Some results missing visibilities for comicId ${comicId}. Results: ${JSON.stringify(results).substring(0, 300)}`);
        }
        return allHaveVisibilities;
      },
      'computation duration is reasonable': () => duration < 1000, // Under 1 second
    });
    
    if (!checks) {
      errorRate.add(1);
      // Log diagnostic info on failure
      if (computeRes.status === 504) {
        console.error(`TIMEOUT: Request to compute visibilities timed out after ${duration}ms`);
      } else if (computeRes.status !== 200) {
        console.error(`API ERROR: Status ${computeRes.status}, Body: ${computeRes.body.substring(0, 500)}`);
      }
    }
    
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
        timeout: '10s', // Explicit timeout
      }
    );
    
    const duration = Date.now() - startTime;
    visibilityComputationDuration.add(duration);
    
    // Helper to safely parse and handle both camelCase and PascalCase
    const parseResponse = (r) => {
      try {
        return JSON.parse(r.body);
      } catch (e) {
        console.error(`Failed to parse response: ${r.body.substring(0, 200)}`);
        return null;
      }
    };
    
    // Helper to get results array (handles both casing)
    const getResults = (body) => {
      return body?.Results || body?.results || [];
    };
    
    const body = parseResponse(computeBulkRes);
    const results = getResults(body);
    const processedSuccessfully = body?.ProcessedSuccessfully || body?.processedSuccessfully || 0;
    
    const checks = check(computeBulkRes, {
      'compute bulk status is 200': (r) => r.status === 200,
      'compute bulk not timeout': (r) => r.status !== 504,
      'compute bulk has results': () => {
        if (!body) return false;
        const hasResults = results.length > 0;
        if (!hasResults) {
          console.warn(`No results for startId ${startId}, limit ${limit}. Response: ${JSON.stringify(body).substring(0, 300)}`);
        }
        return hasResults;
      },
      'compute bulk has computed visibilities': () => {
        if (!body || results.length === 0) return false;
        // Each result should have computed visibilities (handle both casing)
        const allHaveVisibilities = results.every(result => {
          const visibilities = result.ComputedVisibilities || result.computedVisibilities || [];
          return visibilities.length > 0;
        });
        if (!allHaveVisibilities) {
          console.warn(`Some results missing visibilities for startId ${startId}. Results: ${JSON.stringify(results).substring(0, 300)}`);
        }
        return allHaveVisibilities;
      },
      'compute bulk processed count matches limit': () => {
        const matches = processedSuccessfully <= limit;
        if (!matches) {
          console.warn(`Processed count ${processedSuccessfully} exceeds limit ${limit}`);
        }
        return matches;
      },
      'bulk computation duration is reasonable': () => duration < 2000, // Under 2 seconds
    });
    
    if (!checks) {
      errorRate.add(1);
      // Log diagnostic info on failure
      if (computeBulkRes.status === 504) {
        console.error(`TIMEOUT: Bulk request timed out after ${duration}ms`);
      } else if (computeBulkRes.status !== 200) {
        console.error(`API ERROR: Status ${computeBulkRes.status}, Body: ${computeBulkRes.body.substring(0, 500)}`);
      }
    }
    
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