import { afterEach, beforeEach, expect, test, vi } from 'vitest';

let apiCached;
let api;
let invalidateApiCache;
let API_CACHE_CONFIG;

beforeEach(async () => {
  vi.resetModules();
  const shared = await import('../shared.js');
  apiCached = shared.apiCached;
  api = shared.api;
  invalidateApiCache = shared.invalidateApiCache;
  API_CACHE_CONFIG = shared.API_CACHE_CONFIG;
  localStorage.clear();
});

afterEach(() => {
  vi.restoreAllMocks();
  localStorage.clear();
  invalidateApiCache();
});

test('GET dedupe keys include tenant and language context', async () => {
  const fetchResolvers = [];
  const fetchMock = vi.fn(() => new Promise((resolve) => {
    fetchResolvers.push(resolve);
  }));
  vi.stubGlobal('fetch', fetchMock);

  localStorage.setItem('crm_last_tenant', 'tenant-a');
  const requestA = apiCached('/dedupe', { method: 'GET' }, '', 'en');
  const requestB = apiCached('/dedupe', { method: 'GET' }, '', 'en');
  expect(fetchMock).toHaveBeenCalledTimes(1);

  localStorage.setItem('crm_last_tenant', 'tenant-b');
  const requestC = apiCached('/dedupe', { method: 'GET' }, '', 'en');
  expect(fetchMock).toHaveBeenCalledTimes(2);

  localStorage.setItem('crm_last_tenant', 'tenant-a');
  const requestD = apiCached('/dedupe', { method: 'GET' }, '', 'zh');
  expect(fetchMock).toHaveBeenCalledTimes(3);

  const sampleResponse = {
    ok: true,
    status: 200,
    json: async () => ({ ok: true })
  };
  fetchResolvers.forEach((resolve) => resolve(sampleResponse));
  await Promise.all([requestA, requestB, requestC, requestD]);
});

test('GET dedupe keys include auth context', async () => {
  const fetchResolvers = [];
  const fetchMock = vi.fn(() => new Promise((resolve) => {
    fetchResolvers.push(resolve);
  }));
  vi.stubGlobal('fetch', fetchMock);
  localStorage.setItem('crm_last_tenant', 'tenant-auth');

  const requestA = api('/dedupe-auth', { method: 'GET' }, 'token-a', 'en');
  const requestB = api('/dedupe-auth', { method: 'GET' }, 'token-a', 'en');
  const requestC = api('/dedupe-auth', { method: 'GET' }, 'token-b', 'en');

  expect(fetchMock).toHaveBeenCalledTimes(2);

  const sampleResponse = {
    ok: true,
    status: 200,
    json: async () => ({ ok: true }),
  };
  fetchResolvers.forEach((resolve) => resolve(sampleResponse));
  await Promise.all([requestA, requestB, requestC]);
});

test('GET dedupe keys should not collide for same-prefix auth tokens', async () => {
  const fetchResolvers = [];
  const fetchMock = vi.fn(() => new Promise((resolve) => {
    fetchResolvers.push(resolve);
  }));
  vi.stubGlobal('fetch', fetchMock);
  localStorage.setItem('crm_last_tenant', 'tenant-auth-collision');

  const tokenA = `${'x'.repeat(24)}-aaaaaaaaaa`;
  const tokenB = `${'x'.repeat(24)}-bbbbbbbbbb`;

  const requestA = api('/dedupe-auth-collision', { method: 'GET' }, tokenA, 'en');
  const requestB = api('/dedupe-auth-collision', { method: 'GET' }, tokenB, 'en');

  expect(fetchMock).toHaveBeenCalledTimes(2);

  const sampleResponse = {
    ok: true,
    status: 200,
    json: async () => ({ ok: true }),
  };
  fetchResolvers.forEach((resolve) => resolve(sampleResponse));
  await Promise.all([requestA, requestB]);
});

test('GET requests with abort signal should not dedupe', async () => {
  const fetchMock = vi.fn(() =>
    Promise.resolve({
      ok: true,
      status: 200,
      json: async () => ({ ok: true }),
    })
  );
  vi.stubGlobal('fetch', fetchMock);
  localStorage.setItem('crm_last_tenant', 'tenant-signal');
  const controller = new AbortController();

  await Promise.all([
    api('/dedupe-signal', { method: 'GET', signal: controller.signal }, '', 'en'),
    api('/dedupe-signal', { method: 'GET', signal: controller.signal }, '', 'en'),
  ]);

  expect(fetchMock).toHaveBeenCalledTimes(2);
});

test('Cache evicts lower priority entries when size limit reached', async () => {
  const fetchMock = vi.fn((url) =>
    Promise.resolve({
      ok: true,
      status: 200,
      json: async () => ({ url })
    })
  );
  vi.stubGlobal('fetch', fetchMock);

  API_CACHE_CONFIG.maxSize = 2;
  localStorage.setItem('crm_last_tenant', 'tenant-cache');

  await apiCached('/path-a', { method: 'GET' }, '', 'en', true, 'high');
  await apiCached('/path-b', { method: 'GET' }, '', 'en', true, 'low');
  await apiCached('/path-c', { method: 'GET' }, '', 'en', true, 'medium');

  expect(fetchMock).toHaveBeenCalledTimes(3);

  await apiCached('/path-a', { method: 'GET' }, '', 'en');
  expect(fetchMock).toHaveBeenCalledTimes(3);

  await apiCached('/path-b', { method: 'GET' }, '', 'en');
  expect(fetchMock).toHaveBeenCalledTimes(4);
});

test('Cache hit keeps array payload structure stable', async () => {
  const payload = [{ id: '1' }, { id: '2' }];
  const fetchMock = vi.fn(() =>
    Promise.resolve({
      ok: true,
      status: 200,
      json: async () => payload,
    })
  );
  vi.stubGlobal('fetch', fetchMock);

  localStorage.setItem('crm_last_tenant', 'tenant-array');
  const first = await apiCached('/array-data', { method: 'GET' }, '', 'en');
  const second = await apiCached('/array-data', { method: 'GET' }, '', 'en');

  expect(fetchMock).toHaveBeenCalledTimes(1);
  expect(Array.isArray(first)).toBe(true);
  expect(Array.isArray(second)).toBe(true);
  expect(second).toEqual(payload);
});

test('Tenant-required endpoint throws observable error when tenant is missing', async () => {
  const fetchMock = vi.fn();
  vi.stubGlobal('fetch', fetchMock);
  localStorage.removeItem('crm_last_tenant');

  await expect(api('/v1/customers', { method: 'POST', body: JSON.stringify({}) }, null, 'en')).rejects.toMatchObject({
    code: 'TENANT_REQUIRED',
    status: 400,
    path: '/v1/customers',
  });
  expect(fetchMock).not.toHaveBeenCalled();
});

test('Auth endpoint stays compatible without tenant context', async () => {
  const fetchMock = vi.fn(() =>
    Promise.resolve({
      ok: true,
      status: 200,
      json: async () => ({ ok: true }),
    })
  );
  vi.stubGlobal('fetch', fetchMock);
  localStorage.removeItem('crm_last_tenant');

  const result = await api('/v1/auth/session', { method: 'GET' }, null, 'en');
  expect(result).toEqual({ ok: true });
  expect(fetchMock).toHaveBeenCalledTimes(1);
});
