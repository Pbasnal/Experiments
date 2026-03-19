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
    errors: ['rate<0.1'], // Error rate should be below 10%
    comic_visibility_computation_duration: ['p(95)<1000'], // Visibility computation should be under 1 second
  },
};

// Metrics for tracking visibility computation performance
const visibilityComputationDuration = new Trend('comic_visibility_computation_duration', true);

function parseResponse(response) {
  try {
    return JSON.parse(response.body);
  } catch (e) {
    console.error(`Failed to parse response: ${response.body.substring(0, 200)}`);
    return null;
  }
}

function getResults(body) {
  return body?.Results || body?.results || [];
}

function computeVisibility(
  baseUrl,
  {
    startId,
    limit,
    endpointTag,
    maxDurationMs,
    validateProcessedCount,
    debugLabel,
  }
) {
  const startTime = Date.now();

  const res = http.get(
    `${baseUrl}/api/comics/compute-visibilities?startId=${startId}&limit=${limit}`,
    {
      tags: { endpoint: endpointTag },
      timeout: '10s',
    }
  );

  const duration = Date.now() - startTime;
  visibilityComputationDuration.add(duration);

  const body = parseResponse(res);
  const results = getResults(body);
  const processedSuccessfully = body?.ProcessedSuccessfully || body?.processedSuccessfully || 0;

  const checks = check(res, {
    'compute visibility status is 200': (r) => r.status === 200,
    'compute visibility not timeout': (r) => r.status !== 504,
    'compute visibility has results': () => {
      if (!body) return false;
      const hasResults = results.length > 0;
      if (!hasResults) {
        console.warn(
          `No results for ${debugLabel} (startId=${startId}, limit=${limit}). Response: ${JSON.stringify(body).substring(0, 300)}`
        );
      }
      return hasResults;
    },
    'compute visibility has computed visibilities': () => {
      if (!body || results.length === 0) return false;
      const allHaveVisibilities = results.every((result) => {
        const visibilities = result.ComputedVisibilities || result.computedVisibilities || [];
        return visibilities.length > 0;
      });
      if (!allHaveVisibilities) {
        console.warn(
          `Some results missing visibilities for ${debugLabel} (startId=${startId}, limit=${limit}). Results: ${JSON.stringify(results).substring(0, 300)}`
        );
      }
      return allHaveVisibilities;
    },
    'compute visibility processed count matches limit': () => {
      if (!validateProcessedCount) return true;
      const matches = processedSuccessfully <= limit;
      if (!matches) {
        console.warn(
          `Processed count ${processedSuccessfully} exceeds limit ${limit} for ${debugLabel} (startId=${startId})`
        );
      }
      return matches;
    },
    'computation duration is reasonable': () => duration < maxDurationMs,
  });

  if (!checks) {
    errorRate.add(1);
    if (res.status === 504) {
      console.error(
        `TIMEOUT: ${debugLabel} compute visibilities timed out after ${duration}ms (startId=${startId}, limit=${limit})`
      );
    } else if (res.status !== 200) {
      console.error(
        `API ERROR (${debugLabel}): Status ${res.status}, Body: ${res.body.substring(0, 500)}`
      );
    }
  }
}

// Main test function
export default function () {
  const baseUrl = __ENV.API_URL || 'http://localhost:8081';

  // Only one random decision per iteration; exactly one visibility computation.
  // This preserves the original relative weights: single (0.4) vs bulk (0.3).
  const roll = Math.random();
  const singleThreshold = 0.4 / (0.4 + 0.3);

  if (roll < singleThreshold) {
    const comicId = Math.floor(Math.random() * 100) + 1; // 1..100
    computeVisibility(baseUrl, {
      startId: comicId,
      limit: 1,
      endpointTag: 'compute-single',
      maxDurationMs: 1000,
      validateProcessedCount: false,
      debugLabel: 'single',
    });
  } else {
    const startId = Math.floor(Math.random() * 90) + 1; // 1..90
    const limit = Math.floor(Math.random() * 10) + 1; // 1..10
    computeVisibility(baseUrl, {
      startId,
      limit,
      endpointTag: 'compute-bulk',
      maxDurationMs: 2000,
      validateProcessedCount: true,
      debugLabel: 'bulk',
    });
  }

  // Only one sleep per iteration.
  sleep(0.02); // 20ms
}