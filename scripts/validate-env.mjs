const mode = String(process.argv[2] || 'dev').trim().toLowerCase()

function readEnv(name) {
  const value = process.env[name]
  return typeof value === 'string' ? value.trim() : ''
}

function check(name, options = {}) {
  const value = readEnv(name)
  const required = !!options.required
  const insecureValues = Array.isArray(options.insecureValues) ? options.insecureValues : []
  const minLength = Number(options.minLength || 0)
  const rejectPlaceholders = options.rejectPlaceholders !== false
  const hasValue = value.length > 0

  if (required && !hasValue) {
    return { name, ok: false, level: 'error', detail: 'missing required env' }
  }
  if (hasValue && insecureValues.includes(value)) {
    return { name, ok: false, level: 'error', detail: `insecure value: ${value}` }
  }
  if (hasValue && rejectPlaceholders && isPlaceholderValue(value)) {
    return { name, ok: false, level: 'error', detail: 'placeholder value must be replaced' }
  }
  if (hasValue && minLength > 0 && value.length < minLength) {
    return { name, ok: false, level: 'error', detail: `value is shorter than ${minLength} characters` }
  }
  if (!hasValue) {
    return { name, ok: true, level: 'warn', detail: 'not set (using default/fallback)' }
  }
  return { name, ok: true, level: 'ok', detail: 'set' }
}

function isPlaceholderValue(value) {
  const normalized = String(value || '').trim().toLowerCase()
  if (!normalized) return false
  return (
    normalized.includes('replace-with') ||
    normalized.includes('change-me') ||
    normalized.includes('change_this') ||
    normalized.includes('change-this') ||
    normalized.includes('example.com') ||
    normalized === 'example' ||
    normalized === 'password' ||
    normalized === 'root'
  )
}

function buildChecks() {
  if (mode === 'prod' || mode === 'production') {
    return [
      check('AUTH_TOKEN_SECRET', { required: true, insecureValues: ['crm-secret-change-me'], minLength: 32 }),
      check('SECURITY_MFA_STATIC_CODE', { required: true, insecureValues: ['000000'] }),
      check('AUTH_BOOTSTRAP_DEFAULT_PASSWORD', { required: true, insecureValues: ['admin123'] }),
      check('SECURITY_SSO_MODE', { required: true, insecureValues: ['mock'] }),
      check('DB_URL', { required: true }),
      check('DB_USER', { required: true }),
      check('DB_PASSWORD', { required: true, minLength: 12 }),
    ]
  }

  if (mode === 'staging') {
    return [
      check('STAGING_HOST', { required: true }),
      check('STAGING_USER', { required: true }),
      check('STAGING_SSH_PORT'),
      check('STAGING_BASE_URL'),
      check('ARTIFACT_VERSION'),
      check('STAGING_ENV_FILE', { required: true }),
    ]
  }

  return [
    check('DB_URL'),
    check('DB_USER'),
    check('DB_PASSWORD'),
    check('VITE_API_BASE_URL'),
  ]
}

function main() {
  const checks = buildChecks()
  const errors = checks.filter((item) => !item.ok)
  const warnings = checks.filter((item) => item.ok && item.level === 'warn')

  console.log(`ENV_VALIDATION mode=${mode}`)
  for (const item of checks) {
    const tag = item.ok ? (item.level === 'warn' ? 'WARN' : 'OK') : 'FAIL'
    console.log(`[${tag}] ${item.name} - ${item.detail}`)
  }

  if (errors.length) {
    console.error(`ENV_VALIDATION_FAIL count=${errors.length}`)
    process.exit(1)
  }
  console.log(`ENV_VALIDATION_OK warnings=${warnings.length}`)
}

main()
