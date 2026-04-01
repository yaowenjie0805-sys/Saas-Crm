// ============================================
// API 配置 | API Configuration
// ============================================

// 优化：根据环境自动选择 API 基础路径
const getApiBase = () => {
  const env = import.meta.env.MODE || 'development';
  
  if (env === 'production') {
    // 生产环境使用相对路径，通过反向代理访问
    return '/api';
  }
  
  // 开发/测试环境使用配置的 URL
  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
};

export const API_BASE = getApiBase();

// API 请求缓存配置 | API Request Cache Configuration
export const API_CACHE_CONFIG = {
  enabled: true,
  ttl: 60 * 1000,        // 缓存 TTL: 60秒
  maxSize: 100,           // 最大缓存条目数
  preloadEnabled: true,    // 启用预加载
  versionedCache: true,    // 启用版本控制
  priorityLevels: {
    high: 5 * 60 * 1000,   // 高优先级缓存: 5分钟
    medium: 60 * 1000,     // 中优先级缓存: 1分钟
    low: 10 * 1000         // 低优先级缓存: 10秒
  }
};

// 内部请求缓存（按 URL + method + body 哈希）
const requestCache = new Map();
const requestCacheMeta = new Map();

/**
 * 生成请求缓存 key
 */
const TENANT_HEADER_KEYS = ['X-Tenant-Id', 'x-tenant-id'];

const readHeaderValue = (headers, key) => {
  if (!headers) return null;
  if (typeof headers.get === 'function') return headers.get(key);
  return headers[key];
};

const getCacheContext = (options = {}, lang) => {
  const headerTenant = TENANT_HEADER_KEYS.reduce((value, key) => {
    if (value) return value;
    const headerValue = readHeaderValue(options.headers, key);
    return headerValue ? String(headerValue).trim() : value;
  }, '');
  const tenantId = headerTenant || getTenantId();
  const normalizedLang = String(lang || '').trim() || 'en';
  return { tenantId: tenantId || '__NO_TENANT__', lang: normalizedLang };
};

const getCacheKey = (path, options = {}, context) => {
  const method = (options.method || 'GET').toUpperCase();
  const body = options.body ? JSON.stringify(options.body) : '';
  return `${method}:${context.tenantId}:${context.lang}:${path}:${body}`;
};

/**
 * 清理过期缓存
 */
const cleanExpiredCache = () => {
  const now = Date.now();
  for (const [key, meta] of requestCacheMeta.entries()) {
    if (now > meta.expiresAt) {
      requestCache.delete(key);
      requestCacheMeta.delete(key);
    }
  }
};

const PRIORITY_RANK = {
  low: 0,
  medium: 1,
  high: 2,
};

const getPriorityRank = (priority) => {
  const normalized = String(priority || '').toLowerCase();
  return Object.prototype.hasOwnProperty.call(PRIORITY_RANK, normalized)
    ? PRIORITY_RANK[normalized]
    : PRIORITY_RANK.medium;
};

const evictCacheEntries = () => {
  const maxSize = API_CACHE_CONFIG.maxSize;
  if (maxSize <= 0) return;
  const entries = Array.from(requestCacheMeta.entries()).sort(([, metaA], [, metaB]) => {
    const priorityDiff = getPriorityRank(metaA.priority) - getPriorityRank(metaB.priority);
    if (priorityDiff !== 0) return priorityDiff;
    return (metaA.timestamp || 0) - (metaB.timestamp || 0);
  });
  for (const [key] of entries) {
    if (requestCache.size < maxSize) break;
    requestCache.delete(key);
    requestCacheMeta.delete(key);
  }
};

const ensureCacheCapacity = (cacheKey) => {
  const maxSize = API_CACHE_CONFIG.maxSize;
  if (maxSize <= 0) return false;
  if (requestCache.has(cacheKey)) return true;
  if (requestCache.size < maxSize) return true;
  cleanExpiredCache();
  if (requestCache.size < maxSize) return true;
  evictCacheEntries();
  return requestCache.size < maxSize;
};

export const FILTERS_KEY = 'crm_filters_v2'
export const LANG_KEY = 'crm_lang'
export const OIDC_STATE_KEY = 'crm_oidc_state'
export const ROLES = ['ADMIN', 'MANAGER', 'SALES', 'ANALYST']
export const READ_OPS = ['opViewDashboard', 'opViewReports']
export const WRITE_OPS = ['opManageCustomers', 'opDeleteCustomers', 'opManageTasks', 'opManageFollowUps', 'opCreateOpportunity', 'opEditOpportunityAmount']
export const CUSTOMER_STATUS_OPTIONS = ['Active', 'Pending', 'Inactive']
export const OPPORTUNITY_STAGE_OPTIONS = ['Lead', 'Qualified', 'Proposal', 'Negotiation', 'Closed Won', 'Closed Lost']
export const CONTRACT_STATUS_OPTIONS = ['Draft', 'Signed']
export const PAYMENT_STATUS_OPTIONS = ['Pending', 'Received', 'Overdue']
export const PAYMENT_METHOD_OPTIONS = ['Bank', 'Transfer', 'Cash', 'Card']
export const FOLLOW_UP_CHANNEL_OPTIONS = ['Phone', 'Email', 'WeChat', 'Visit', 'Meeting']
export const MARKET_PROFILE_OPTIONS = ['CN', 'GLOBAL']
export const TENANT_APPROVAL_MODE_OPTIONS = ['STRICT', 'STAGE_GATE']

const normalize = (value) => String(value || '').trim().replace(/[-\s]+/g, '_').toUpperCase()

const ROLE_MAP = {
  ADMIN: 'roleAdmin',
  ROLEADMIN: 'roleAdmin',
  MANAGER: 'roleManager',
  ROLEMANAGER: 'roleManager',
  SALES: 'roleSales',
  ROLESALES: 'roleSales',
  ANALYST: 'roleAnalyst',
  ROLEANALYST: 'roleAnalyst',
}

const STATUS_MAP = {
  ACTIVE: 'statusActive',
  PENDING: 'statusPending',
  WAITING: 'statusWaiting',
  INACTIVE: 'statusInactive',
  NEW: 'statusNew',
  OPEN: 'statusOpen',
  WON: 'statusWon',
  LOST: 'statusLost',
  DRAFT: 'statusDraft',
  SUBMITTED: 'statusSubmitted',
  CONFIRMED: 'statusConfirmed',
  FULFILLING: 'statusFulfilling',
  APPROVED: 'statusApproved',
  REJECTED: 'statusRejected',
  ESCALATED: 'statusEscalated',
  CANCELED: 'statusCanceled',
  RETRY: 'statusRetry',
  SUCCESS: 'statusSuccess',
  PARTIAL_SUCCESS: 'statusPartialSuccess',
  SIGNED: 'statusSigned',
  RECEIVED: 'statusReceived',
  COMPLETED: 'statusCompleted',
  FAILED: 'statusFailed',
  RUNNING: 'statusRunning',
  DONE: 'statusDone',
  PAID: 'statusPaid',
  UNPAID: 'statusUnpaid',
  OVERDUE: 'statusOverdue',
}

const STAGE_MAP = {
  LEAD: 'stageLead',
  QUALIFIED: 'stageQualified',
  PROPOSAL: 'stageProposal',
  NEGOTIATION: 'stageNegotiation',
  CLOSED_WON: 'stageClosedWon',
  CLOSED_LOST: 'stageClosedLost',
  STAGELEAD: 'stageLead',
  STAGEQUALIFIED: 'stageQualified',
  STAGEPROPOSAL: 'stageProposal',
  STAGENEGOTIATION: 'stageNegotiation',
  STAGECLOSEDWON: 'stageClosedWon',
  STAGECLOSEDLOST: 'stageClosedLost',
}

const CHANNEL_MAP = {
  PHONE: 'channelPhone',
  EMAIL: 'channelEmail',
  WECHAT: 'channelWechat',
  VISIT: 'channelVisit',
  MEETING: 'channelMeeting',
}

const METHOD_MAP = {
  BANK: 'methodBank',
  CASH: 'methodCash',
  TRANSFER: 'methodTransfer',
  CARD: 'methodCard',
}

const LEVEL_MAP = {
  HIGH: 'levelHigh',
  MEDIUM: 'levelMedium',
  LOW: 'levelLow',
}

const TIME_MAP = {
  TODAY: 'today',
  TOMORROW: 'tomorrow',
  THIS_WEEK: 'thisWeek',
}

const STAT_LABEL_MAP = {
  TOTAL_CUSTOMERS: 'statTotalCustomers',
  PROJECTED_REVENUE: 'statProjectedRevenue',
  AVG_SALES_CYCLE: 'statAvgSalesCycle',
  RETENTION_RATE: 'statRetentionRate',
}

const DATASET_MAP = {
  CUSTOMERS: 'customers',
  OPPORTUNITIES: 'pipeline',
  CONTRACTS: 'contracts',
  PAYMENTS: 'payments',
  LEADS: 'leads',
}

const VISIBILITY_MAP = {
  PRIVATE: 'reportVisibilityPrivate',
  DEPARTMENT: 'reportVisibilityDepartment',
  TENANT: 'reportVisibilityTenant',
}

const OWNER_ALIAS_MAP = {
  SYSTEMADMIN: 'ownerSystemAdmin',
  ADMIN: 'ownerAdmin',
  MANAGER: 'ownerManager',
  SALES: 'ownerSales',
  ANALYST: 'ownerAnalyst',
}

const translateMapped = (t, mapping, value) => {
  if (value === null || value === undefined || value === '') return '-'
  const key = mapping[normalize(value)]
  return key ? t(key) : String(value)
}

export const formatDateTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')
export const mapToBars = (obj) => Object.entries(obj || {}).map(([label, value]) => ({ label, value: Number(value || 0) }))
export const formatMoney = (v) => {
  const n = Number(v || 0)
  const cnySymbol = '\u00A5'
  if (Math.abs(n) >= 1e6) return `${cnySymbol}${(n / 1e6).toFixed(2)}M`
  if (Math.abs(n) >= 1e3) return `${cnySymbol}${(n / 1e3).toFixed(1)}K`
  return `${cnySymbol}${n}`
}
export const formatMoneyByCurrency = (v, currency = 'CNY') => {
  const n = Number(v || 0)
  if (!Number.isFinite(n)) return `${currency} 0`
  const symbol = currency === 'CNY' ? '\u00A5' : currency === 'USD' ? '$' : `${currency} `
  if (Math.abs(n) >= 1e6) return `${symbol}${(n / 1e6).toFixed(2)}M`
  if (Math.abs(n) >= 1e3) return `${symbol}${(n / 1e3).toFixed(1)}K`
  return `${symbol}${n}`
}
export const translateRole = (t, value) => translateMapped(t, ROLE_MAP, value)
export const translateStatus = (t, value) => translateMapped(t, STATUS_MAP, value)
export const translateStage = (t, value) => translateMapped(t, STAGE_MAP, value)
export const translateChannel = (t, value) => translateMapped(t, CHANNEL_MAP, value)
export const translateMethod = (t, value) => translateMapped(t, METHOD_MAP, value)
export const translateTaskLevel = (t, value) => translateMapped(t, LEVEL_MAP, value)
export const translateTimeLabel = (t, value) => translateMapped(t, TIME_MAP, value)
export const translateStatLabel = (t, value) => translateMapped(t, STAT_LABEL_MAP, value)
export const translateDataset = (t, value) => translateMapped(t, DATASET_MAP, value)
export const translateVisibility = (t, value) => translateMapped(t, VISIBILITY_MAP, value)
export const translateOwnerAlias = (t, value) => translateMapped(t, OWNER_ALIAS_MAP, value)
export const formatStatValue = (t, label, value) => {
  const normalizedLabel = normalize(label)
  const raw = String(value || '')
  if (normalizedLabel === 'PROJECTED_REVENUE') {
    const n = Number(raw.replace(/[^\d.-]/g, ''))
    return Number.isFinite(n) && raw ? formatMoney(n) : raw.replace(/^CNY\s+/i, '\u00A5')
  }
  if (normalizedLabel === 'AVG_SALES_CYCLE') {
    const days = raw.match(/(\d+)/)?.[1]
    return days ? `${days} ${t('dayUnit')}` : raw
  }
  return raw.replace(/^CNY\s+/i, '\u00A5')
}
export const readFilters = () => {
  try {
    return JSON.parse(localStorage.getItem(FILTERS_KEY) || '{}')
  } catch {
    return {}
  }
}
export const parseHashPage = () => {
  const raw = (window.location.hash || '').replace(/^#\/?/, '')
  return raw || 'dashboard'
}

// 请求去重 Map：防止短时间内重复发送相同 GET 请求
const pendingRequests = new Map()

export async function api(path, options = {}, token, lang = 'en') {
  // 只对 GET 请求去重
  const requestContext = getCacheContext(options, lang)
  const method = (options.method || 'GET').toUpperCase()
  const dedupeKey =
    method === 'GET'
      ? `${method}:${requestContext.tenantId}:${requestContext.lang}:${path}`
      : null

  // 如果有相同的 GET 请求正在进行，直接返回该 Promise
  if (dedupeKey && pendingRequests.has(dedupeKey)) {
    return pendingRequests.get(dedupeKey)
  }

  const requestPromise = (async () => {
    const body = options.body
    const isFormData = typeof FormData !== 'undefined' && body instanceof FormData
    const headers = { 'Accept-Language': requestContext.lang, ...(options.headers || {}) }
    if (!isFormData && !headers['Content-Type']) headers['Content-Type'] = 'application/json'
    if (isFormData && headers['Content-Type']) delete headers['Content-Type']
    if (token && token !== 'COOKIE_SESSION') headers.Authorization = `Bearer ${token}`
    if (!headers['X-Tenant-Id']) {
      try {
        const auth = JSON.parse(localStorage.getItem('crm_auth') || 'null')
        const tenantFromAuth = String(auth?.tenantId || '').trim()
        if (tenantFromAuth) headers['X-Tenant-Id'] = tenantFromAuth
      } catch {
        // ignore localStorage parse errors
      }
      if (!headers['X-Tenant-Id']) {
        const lastTenant = String(localStorage.getItem('crm_last_tenant') || '').trim()
        if (lastTenant) headers['X-Tenant-Id'] = lastTenant
      }
    }
    if (requiresTenant(path) && !String(headers['X-Tenant-Id'] || '').trim()) {
      const fallback = requestContext.lang === 'zh'
        ? '缺少租户上下文，请重新选择租户后再试'
        : 'Missing tenant context; please select a tenant and retry'
      const err = new Error(fallback)
      err.code = 'TENANT_REQUIRED'
      err.status = 400
      err.path = path
      throw err
    }
    const res = await fetch(`${API_BASE}${path}`, { ...options, credentials: 'include', headers, body })
    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      const fallback = requestContext.lang === 'zh' ? '\u8bf7\u6c42\u5931\u8d25' : 'Request failed'
      const err = new Error(body.message || fallback)
      err.code = body.code || ''
      err.details = body.details || {}
      err.requestId = body.requestId || ''
      err.status = res.status
      err.validationErrors = body.validationErrors || null
      throw err
    }
    if (res.status === 204) return null
    return res.json()
  })()

  // 注册 GET 请求到 pendingRequests
  if (dedupeKey) {
    pendingRequests.set(dedupeKey, requestPromise)
    // 请求完成后清理
    requestPromise.finally(() => {
      pendingRequests.delete(dedupeKey)
    })
  }

  return requestPromise
}

/**
 * 缓存清理定时器
 */
let cacheCleanupInterval = null;

/**
 * 初始化缓存清理
 */
const initCacheCleanup = () => {
  if (!cacheCleanupInterval && API_CACHE_CONFIG.enabled) {
    cacheCleanupInterval = setInterval(cleanExpiredCache, API_CACHE_CONFIG.ttl);
  }
};

/**
 * API 请求函数（带缓存支持）
 * @param {string} path - API 路径
 * @param {object} options - 请求选项
 * @param {string} token - 认证 token
 * @param {string} lang - 语言
 * @param {boolean} useCache - 是否使用缓存（仅 GET 请求生效）
 */
export async function apiCached(path, options = {}, token, lang = 'en', useCache = true, priority = 'medium') {
  const method = (options.method || 'GET').toUpperCase();
  const requestContext = getCacheContext(options, lang);
  const requestLang = requestContext.lang;
  const cacheKey = getCacheKey(path, options, requestContext);
  const isCacheable =
    useCache && method === 'GET' && API_CACHE_CONFIG.enabled && API_CACHE_CONFIG.maxSize > 0;
  const priorityTtl = API_CACHE_CONFIG.priorityLevels[priority] || API_CACHE_CONFIG.ttl;

  // 检查缓存
  if (isCacheable) {
    initCacheCleanup();

    const cached = requestCache.get(cacheKey);
    const meta = requestCacheMeta.get(cacheKey);
    if (cached && meta && Date.now() < meta.expiresAt) {
      return cached;
    }
  }

  // 执行请求
  const result = await api(path, options, token, requestLang);

  // 存入缓存
  if (isCacheable && result && ensureCacheCapacity(cacheKey)) {
    requestCache.set(cacheKey, result);
    requestCacheMeta.set(cacheKey, {
      expiresAt: Date.now() + priorityTtl,
      priority,
      timestamp: Date.now()
    });
  }

  return result;
}

/**
 * 清除指定路径的缓存
 */
export const invalidateApiCache = (pathPrefix = null) => {
  if (pathPrefix) {
    for (const key of requestCache.keys()) {
      if (key.includes(pathPrefix)) {
        requestCache.delete(key);
        requestCacheMeta.delete(key);
      }
    }
  } else {
    requestCache.clear();
    requestCacheMeta.clear();
  }
};

/**
 * 预加载API数据
 * @param {Array} requests - 请求配置数组 [{ path, options, priority }]
 * @param {string} token - 认证 token
 * @param {string} lang - 语言
 */
export async function preloadApiData(requests = [], token, lang = 'en') {
  if (!API_CACHE_CONFIG.preloadEnabled) return;
  
  const preloadPromises = requests.map(async (req) => {
    try {
      await apiCached(req.path, req.options || {}, token, lang, true, req.priority || 'medium');
    } catch (error) {
      console.warn('Preload API failed:', req.path, error);
    }
  });
  
  await Promise.all(preloadPromises);
}

/**
 * 批量执行API请求
 * @param {Array} requests - 请求配置数组 [{ path, options, useCache, priority }]
 * @param {string} token - 认证 token
 * @param {string} lang - 语言
 */
export async function batchApiRequests(requests = [], token, lang = 'en') {
  const batchPromises = requests.map(async (req) => {
    try {
      const result = await apiCached(
        req.path,
        req.options || {},
        token,
        lang,
        req.useCache !== false,
        req.priority || 'medium'
      );
      return { path: req.path, success: true, data: result };
    } catch (error) {
      return { path: req.path, success: false, error: error.message };
    }
  });
  
  return Promise.all(batchPromises);
}

// ============================================
// Storage Keys | 本地存储 Key 统一管理
// ============================================
export const STORAGE_KEYS = {
  AUTH: 'crm_auth',
  LAST_TENANT: 'crm_last_tenant',
  FILTERS: 'crm_filters_v2',
  LANG: 'crm_lang',
  OIDC_STATE: 'crm_oidc_state',
  TOKEN: 'token',
  TENANT_ID: 'tenantId',
};

/**
 * 获取认证信息
 */
const getAuth = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.AUTH) || 'null');
  } catch {
    return null;
  }
};

/**
 * 获取认证 Token
 */
const getToken = () => {
  return localStorage.getItem(STORAGE_KEYS.TOKEN) || '';
};

/**
 * 获取租户 ID
 */
const getTenantId = () => {
  try {
    const auth = getAuth();
    const tenantFromAuth = String(auth?.tenantId || '').trim();
    if (tenantFromAuth) return tenantFromAuth;
  } catch {
    // ignore
  }
  const lastTenant = String(localStorage.getItem(STORAGE_KEYS.LAST_TENANT) || '').trim();
  if (lastTenant) return lastTenant;
  return '';
};

const TENANT_OPTIONAL_PATH_PREFIXES = ['/auth/', '/v1/auth/']

const requiresTenant = (path = '') => {
  const normalized = String(path || '').trim()
  if (!normalized) return true
  return !TENANT_OPTIONAL_PATH_PREFIXES.some((prefix) => normalized.startsWith(prefix))
}

/**
 * 获取语言设置
 */
export const getLang = () => {
  return localStorage.getItem(STORAGE_KEYS.LANG) || 'en';
};

/**
 * 获取通用请求 headers
 */
const getCommonHeaders = (lang = 'en') => {
  const headers = { 'Accept-Language': lang };
  const tenantId = getTenantId();
  if (tenantId) headers['X-Tenant-Id'] = tenantId;
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
};

/**
 * 文件下载 API 函数
 * 统一处理文件下载的认证和租户 headers
 * @param {string} path - API 路径（不含 /api 前缀）
 * @param {string} filename - 下载文件名
 */
export async function apiDownload(path, filename = 'download') {
  const lang = getLang();
  const headers = getCommonHeaders(lang);
  if (requiresTenant(path) && !String(headers['X-Tenant-Id'] || '').trim()) {
    const fallback = lang === 'zh'
      ? '缺少租户上下文，请重新选择租户后再试'
      : 'Missing tenant context; please select a tenant and retry'
    const err = new Error(fallback)
    err.code = 'TENANT_REQUIRED'
    err.status = 400
    err.path = path
    throw err
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: 'GET',
    credentials: 'include',
    headers
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const fallback = lang === 'zh' ? '下载失败' : 'Download failed';
    throw new Error(body.message || fallback);
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
  return blob;
}

/**
 * 文件上传 API 函数（使用 FormData）
 * @param {string} path - API 路径
 * @param {FormData} formData - 表单数据
 * @param {object} options - 其他请求选项
 */
export async function apiUpload(path, formData, options = {}) {
  const lang = getLang();
  const headers = getCommonHeaders(lang);
  if (requiresTenant(path) && !String(headers['X-Tenant-Id'] || '').trim()) {
    const fallback = lang === 'zh'
      ? '缺少租户上下文，请重新选择租户后再试'
      : 'Missing tenant context; please select a tenant and retry'
    const err = new Error(fallback)
    err.code = 'TENANT_REQUIRED'
    err.status = 400
    err.path = path
    throw err
  }
  // FormData 不需要设置 Content-Type，让浏览器自动处理
  delete headers['Content-Type'];

  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    credentials: 'include',
    headers,
    body: formData,
    ...options
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const fallback = lang === 'zh' ? '上传失败' : 'Upload failed';
    const err = new Error(body.message || fallback);
    err.code = body.code || '';
    err.details = body.details || {};
    err.validationErrors = body.validationErrors || null;
    throw err;
  }

  if (response.status === 204) return null;
  return response.json();
}





