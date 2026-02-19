/**
 * Max throughput load test: find the highest request rate (RPS) the API can sustain
 * before timeouts (504) become a significant problem.
 *
 * Strategy: ramp arrival rate from low to high over time; track success vs timeout.
 * Run with: k6 run k6/load-test-max-throughput.js
 * Optional: API_URL=http://localhost:8081 MAX_RPS=80 k6 run k6/load-test-max-throughput.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.API_URL || 'http://localhost:8082';
const maxRps = __ENV.MAX_RPS ? parseInt(__ENV.MAX_RPS, 10) : 200;

// Custom metrics
const timeoutRate = new Rate('timeouts');
const successRate = new Rate('success');
const reqDuration = new Trend('http_req_compute_duration', true);

// Ramp request rate from 2 RPS up to maxRps over ~8 min, then hold at max for 2 min
export const options = {
  scenarios: {
    max_throughput: {
      executor: 'ramping-arrival-rate',
      startRate: 2,
      timeUnit: '1s',
      preAllocatedVUs: 4,
      maxVUs: 200,
      stages: [
        { duration: '1m', target: 5 },           // 2 → 5 RPS
        { duration: '1m', target: 10 },        // 5 → 10 RPS
        { duration: '1m', target: 20 },        // 10 → 20 RPS
        { duration: '1m', target: 30 },        // 20 → 30 RPS
        { duration: '1m', target: 40 },        // 30 → 40 RPS
        { duration: '1m', target: 50 },        // 40 → 50 RPS
        { duration: '1m', target: maxRps },    // 50 → maxRps RPS
        { duration: '2m', target: maxRps },    // hold at maxRps for 2 min
      ],
    },
  },
  thresholds: {
    // Fail only if timeouts are very high (test is for discovery; adjust as needed)
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
      'Interpretation: Ramp went from 2 to ' + maxRps + ' RPS. If timeout rate is low,',
      'the API can likely sustain at least that rate. Re-run with higher MAX_RPS',
      'or inspect the Grafana/HTML report to see when timeouts started increasing.',
      '',
    ].join('\n'),
    'summary.json': JSON.stringify(data, null, 2),
  };
}
