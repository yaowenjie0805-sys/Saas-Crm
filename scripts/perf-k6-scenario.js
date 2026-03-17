/* global __ENV */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = __ENV.PERF_BASE_URL || 'http://127.0.0.1:18080';
const tenantId = __ENV.PERF_TENANT_ID || 'tenant_default';
const username = __ENV.PERF_ADMIN_USER || 'admin';
const password = __ENV.PERF_ADMIN_PASSWORD || 'admin123';
const mode = (__ENV.PERF_MODE || 'baseline').toLowerCase();

const loginRequests = new Counter('perf_login_requests');
const loginErrors = new Counter('perf_login_errors');
const loginDuration = new Trend('perf_login_duration', true);

const dashboardRequests = new Counter('perf_dashboard_requests');
const dashboardErrors = new Counter('perf_dashboard_errors');
const dashboardDuration = new Trend('perf_dashboard_duration', true);

const customersRequests = new Counter('perf_customers_requests');
const customersErrors = new Counter('perf_customers_errors');
const customersDuration = new Trend('perf_customers_duration', true);

const reportsRequests = new Counter('perf_reports_requests');
const reportsErrors = new Counter('perf_reports_errors');
const reportsDuration = new Trend('perf_reports_duration', true);

const timeoutCount = new Counter('perf_timeout_count');

const smokeStages = [
  { duration: '10s', target: 2 },
  { duration: '20s', target: 5 },
  { duration: '10s', target: 0 },
];

const baselineStages = [
  { duration: '15s', target: 5 },
  { duration: '45s', target: 15 },
  { duration: '20s', target: 25 },
  { duration: '15s', target: 0 },
];

export const options = {
  stages: mode === 'smoke' ? smokeStages : baselineStages,
  noConnectionReuse: false,
  userAgent: 'crm-perf-k6/1.0',
};

function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    'X-Tenant-Id': tenantId,
    'content-type': 'application/json',
  };
}

function recordResult(result, requestCounter, errorCounter, durationTrend) {
  requestCounter.add(1);
  durationTrend.add(result.timings.duration);
  if (result.status >= 500 || result.status === 0 || result.error) {
    errorCounter.add(1);
    if (result.status === 0 || result.error_code) {
      timeoutCount.add(1);
    }
  }
}

export function setup() {
  const res = http.post(`${baseUrl}/api/auth/login`, JSON.stringify({ username, password }), {
    headers: { 'content-type': 'application/json' },
    timeout: '5s',
  });
  loginRequests.add(1);
  loginDuration.add(res.timings.duration);
  const ok = check(res, {
    'login status 200': (r) => r.status === 200,
    'login has token': (r) => {
      try {
        return !!r.json('token');
      } catch {
        return false;
      }
    },
  });
  if (!ok) {
    loginErrors.add(1);
    if (res.status === 0 || res.error_code) timeoutCount.add(1);
    throw new Error(`login failed status=${res.status} body=${res.body}`);
  }
  return { token: res.json('token') };
}

export default function (data) {
  const token = data.token;
  const headers = authHeaders(token);

  const dashboardRes = http.get(`${baseUrl}/api/dashboard`, { headers, timeout: '5s' });
  recordResult(dashboardRes, dashboardRequests, dashboardErrors, dashboardDuration);
  check(dashboardRes, { 'dashboard status < 500': (r) => r.status > 0 && r.status < 500 });

  const customersRes = http.get(`${baseUrl}/api/customers/search?q=smoke&page=1&size=20`, { headers, timeout: '5s' });
  recordResult(customersRes, customersRequests, customersErrors, customersDuration);
  check(customersRes, { 'customers status < 500': (r) => r.status > 0 && r.status < 500 });

  const reportsRes = http.get(`${baseUrl}/api/v1/reports/overview`, { headers, timeout: '5s' });
  recordResult(reportsRes, reportsRequests, reportsErrors, reportsDuration);
  check(reportsRes, { 'reports status < 500': (r) => r.status > 0 && r.status < 500 });

  sleep(0.2);
}
