/**
 * Max throughput load test: find the highest request rate (RPS) the API can sustain
 * before timeouts (504) become a significant problem.
 *
 * Default: 1000 RPS test for DOD API — 5 min ramp up, then 10 min at 1000 RPS.
 * Run with: k6 run k6/load-test-max-throughput.js
 * DOD API (local): API_URL=http://localhost:8081 k6 run k6/load-test-max-throughput.js
 * Override rate: MAX_RPS=500 k6 run k6/load-test-max-throughput.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.API_URL || 'http://localhost:8081';
const maxRps = __ENV.MAX_RPS ? parseInt(__ENV.MAX_RPS, 10) : 300;

// Custom metrics
const timeoutRate = new Rate('timeouts');
const successRate = new Rate('success');
const reqDuration = new Trend('http_req_compute_duration', true);

// 5 min ramp to maxRps, then 10 min at maxRps (e.g. 1000 RPS for DOD)
export const options = {
  scenarios: {
    max_throughput: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 1500,
      stages: [
        { duration: '1m', target: maxRps * 0.1 },   // 0 → 200 RPS
        { duration: '1m', target: maxRps * 0.4 },   // 200 → 400 RPS
        { duration: '1m', target: maxRps * 0.6 },   // 400 → 600 RPS
        { duration: '1m', target: maxRps * 0.8 },   // 600 → 800 RPS
        { duration: '1m', target: maxRps }, // 800 → maxRps (ramp done in 5 min)
        { duration: '10m', target: maxRps }, // hold at maxRps for 10 min
      ],
    },
  },
  thresholds: {
    timeouts: ['rate<0.15'],
    success: ['rate>0.85'],
  },
};

export default function () {
  const startId = 1;
  const limit = 5;
  const url = `${baseUrl}/api/comics/compute-visibilities?startId=${startId}&limit=${limit}`;

  const res = http.get(url, {
    tags: { endpoint: 'compute-visibilities' },
    timeout: '12s', // slightly above API timeout to detect 504 from server
  });

  const isSuccess = res.status === 200;
  const isTimeout = res.status === 504;

  if (isTimeout) timeoutRate.add(1); else timeoutRate.add(0);
  if (isSuccess) successRate.add(1); else successRate.add(0);
  reqDuration.add(res.timings.duration);

  check(res, {
    'status 200 or 504 (timeout)': (r) => r.status === 200 || r.status === 504,
  });
}

export function handleSummary(data) {
  const timeouts = data.metrics.timeouts?.values?.rate ?? 0;
  const success = data.metrics.success?.values?.rate ?? 0;
  const totalReqs = data.metrics.http_reqs?.values?.count ?? 0;
  const avgDuration = data.metrics.http_req_duration?.values?.avg ?? 0;
  const p95Duration = data.metrics.http_req_duration?.values?.['p(95)'] ?? 0;

  return {
    'stdout': [
      '',
      '--- Max throughput test summary ---',
      `Total requests:     ${totalReqs}`,
      `Success rate:       ${(success * 100).toFixed(2)}%`,
      `Timeout rate:      ${(timeouts * 100).toFixed(2)}%`,
      `Avg latency:       ${(avgDuration / 1000).toFixed(3)}s`,
      `p95 latency:       ${(p95Duration / 1000).toFixed(3)}s`,
      '',
      'Interpretation: 5 min ramp to ' + maxRps + ' RPS, then 10 min hold. If timeout rate is low,',
      'the API can sustain that rate. Use API_URL=http://localhost:8081 for DOD.',
      '',
    ].join('\n'),
    'summary.json': JSON.stringify(data, null, 2),
  };
}
