import { onCLS, onINP, onLCP, type Metric } from 'web-vitals';
import {
  analyticsEnabled,
  trackFrontendException,
  trackWebVital,
} from '../api/analytics';

function ratingOf(metric: Metric): 'good' | 'needs-improvement' | 'poor' {
  if (metric.rating === 'good' || metric.rating === 'needs-improvement' || metric.rating === 'poor') {
    return metric.rating;
  }
  return 'needs-improvement';
}

function onWebVital(metric: Metric): void {
  if (metric.name !== 'LCP' && metric.name !== 'INP' && metric.name !== 'CLS') {
    return;
  }
  trackWebVital(metric.name, metric.value, ratingOf(metric));
}

function installErrorHandlers(): void {
  window.addEventListener('error', (event) => {
    const err = event.error;
    const name =
      err && typeof err === 'object' && 'name' in err
        ? String((err as Error).name)
        : 'Error';
    const message =
      err && typeof err === 'object' && 'message' in err
        ? String((err as Error).message)
        : event.message;
    // Never send stack traces — backend allowlist is name/message only.
    trackFrontendException(name, message);
  });

  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason;
    if (reason instanceof Error) {
      trackFrontendException(reason.name || 'UnhandledRejection', reason.message);
      return;
    }
    trackFrontendException('UnhandledRejection', reason == null ? 'rejected' : String(reason));
  });
}

/** Idempotent browser telemetry bootstrap. No-op in DEV/test. */
export function initTelemetry(): void {
  if (!analyticsEnabled()) {
    return;
  }
  const flag = '__akTelemetryInit';
  if ((window as unknown as Record<string, boolean>)[flag]) {
    return;
  }
  (window as unknown as Record<string, boolean>)[flag] = true;

  onLCP(onWebVital);
  onINP(onWebVital);
  onCLS(onWebVital);
  installErrorHandlers();
}
