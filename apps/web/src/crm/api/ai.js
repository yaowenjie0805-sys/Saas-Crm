import { api } from '../shared'

const DEFAULT_FOLLOW_UP_SUMMARY_PATH =
  String(import.meta.env.VITE_AI_FOLLOWUP_SUMMARY_PATH || '').trim() || '/v1/ai/followUpSummary'
const DEFAULT_AI_COMMENT_REPLY_PATH =
  String(import.meta.env.VITE_AI_COMMENT_REPLY_PATH || '').trim() || '/v1/ai/commentReply'
const DEFAULT_AI_MARKETING_EMAIL_PATH =
  String(import.meta.env.VITE_AI_MARKETING_EMAIL_PATH || '').trim() || '/v1/ai/marketingEmail'
const DEFAULT_AI_SALES_ADVICE_PATH =
  String(import.meta.env.VITE_AI_SALES_ADVICE_PATH || '').trim() || '/v1/ai/salesAdvice'
const DEFAULT_AI_STATUS_PATH = String(import.meta.env.VITE_AI_STATUS_PATH || '').trim() || '/v1/ai/status'
const DEFAULT_AI_CONFIG_PATH = String(import.meta.env.VITE_AI_CONFIG_PATH || '').trim() || '/v1/ai/config'

const BOOLEAN_STATUS_KEYS = ['available', 'enabled', 'isAvailable', 'ready']
const STRING_STATUS_KEYS = ['status', 'state', 'aiStatus']
const AVAILABLE_STATUS_SET = new Set(['up', 'available', 'enabled', 'ready', 'ok', 'healthy', 'online'])
const UNAVAILABLE_STATUS_SET = new Set(['down', 'unavailable', 'disabled', 'error', 'offline'])

const readSummary = (response) => {
  const candidates = [
    response?.summary,
    response?.data?.summary,
    response?.result?.summary,
    response?.result,
    response?.content,
    response?.text,
  ]
  const summary = candidates.find((value) => typeof value === 'string' && value.trim())
  return summary ? summary.trim() : ''
}

const normalizeField = (value) => String(value || '').trim()
const normalizeText = (value) => String(value || '').trim()

const readErrorPayload = (error) => {
  const payload = error?.response?.data || error?.data || error?.body
  if (payload && typeof payload === 'object') return payload
  return {}
}

const formatErrorMessageWithContext = (message, { code = '', requestId = '' } = {}) => {
  const normalizedMessage = normalizeText(message) || 'AI request failed'
  if (/requestid\s*=|code\s*=/i.test(normalizedMessage)) return normalizedMessage
  const parts = []
  if (code) parts.push(`code=${code}`)
  if (requestId) parts.push(`requestId=${requestId}`)
  return parts.length ? `${normalizedMessage} (${parts.join(', ')})` : normalizedMessage
}

export const extractAiErrorDetails = (error, fallbackMessage = 'AI request failed') => {
  const payload = readErrorPayload(error)
  const message =
    normalizeText(payload.message) || normalizeText(error?.message) || normalizeText(fallbackMessage)
  const code = normalizeText(payload.code) || normalizeText(error?.code)
  const requestId = normalizeText(payload.requestId) || normalizeText(error?.requestId)
  const status = Number(error?.status || error?.response?.status || 0) || 0
  return { message, code, requestId, status }
}

export const extractAiErrorMessage = (error, fallbackMessage = 'AI request failed') => {
  const { message, code, requestId } = extractAiErrorDetails(error, fallbackMessage)
  return formatErrorMessageWithContext(message, { code, requestId })
}

const toAiError = (error, fallbackMessage) => {
  const details = extractAiErrorDetails(error, fallbackMessage)
  const normalizedError = new Error(
    formatErrorMessageWithContext(details.message, {
      code: details.code,
      requestId: details.requestId,
    }),
  )
  normalizedError.code = details.code
  normalizedError.requestId = details.requestId
  normalizedError.status = details.status
  normalizedError.cause = error
  return normalizedError
}

const readAvailability = (response) => {
  const roots = [response, response?.data, response?.result]
  for (const root of roots) {
    if (!root || typeof root !== 'object') continue
    for (const key of BOOLEAN_STATUS_KEYS) {
      if (typeof root[key] === 'boolean') return root[key]
    }
    for (const key of STRING_STATUS_KEYS) {
      const value = normalizeText(root[key]).toLowerCase()
      if (!value) continue
      if (AVAILABLE_STATUS_SET.has(value)) return true
      if (UNAVAILABLE_STATUS_SET.has(value)) return false
    }
  }
  return true
}

const readStatusMessage = (response) => {
  const candidates = [
    response?.message,
    response?.detail,
    response?.data?.message,
    response?.data?.detail,
    response?.result?.message,
    response?.result?.detail,
  ]
  return candidates.find((value) => typeof value === 'string' && value.trim())?.trim() || ''
}

export async function queryAiAvailability({
  token,
  lang = 'en',
  path = DEFAULT_AI_STATUS_PATH,
} = {}) {
  try {
    const response = await api(path, { method: 'GET' }, token, lang)
    return {
      available: readAvailability(response),
      message: readStatusMessage(response),
    }
  } catch (error) {
    throw toAiError(error, 'AI service is unavailable')
  }
}

export async function generateFollowUpSummary({
  customerName = '',
  channel = '',
  interactionDetails = '',
  model = '',
  baseUrl = '',
  apiKey = '',
  token,
  lang = 'en',
  path = DEFAULT_FOLLOW_UP_SUMMARY_PATH,
} = {}) {
  try {
    const response = await api(
      path,
      {
        method: 'POST',
        body: JSON.stringify({
          customerName: normalizeField(customerName),
          channel: normalizeField(channel),
          interactionDetails: normalizeField(interactionDetails),
          ...(normalizeField(model) ? { model: normalizeField(model) } : {}),
          ...(normalizeField(baseUrl) ? { baseUrl: normalizeField(baseUrl) } : {}),
          ...(normalizeField(apiKey) ? { apiKey: normalizeField(apiKey) } : {}),
        }),
      },
      token,
      lang,
    )

    const summary = readSummary(response)
    if (!summary) throw new Error('Invalid AI summary response')
    return summary
  } catch (error) {
    if (error?.message === 'Invalid AI summary response') throw error
    throw toAiError(error, 'Failed to generate AI follow-up summary')
  }
}

export async function fetchAiConfig({ token, lang = 'en', path = DEFAULT_AI_CONFIG_PATH } = {}) {
  try {
    const response = await api(path, { method: 'GET' }, token, lang)
    const roots = [response, response?.data, response?.result]
    const candidate = roots.find((item) => item && typeof item === 'object') || {}

    const availableModels = Array.isArray(candidate.availableModels)
      ? candidate.availableModels.map((item) => normalizeText(item)).filter(Boolean)
      : []
    const defaultModel = normalizeText(candidate.defaultModel)
    const canOverride = candidate.canOverride !== false
    const supportsCustomConnection = candidate.supportsCustomConnection !== false

    return { availableModels, defaultModel, canOverride, supportsCustomConnection }
  } catch (error) {
    throw toAiError(error, 'Failed to load AI config')
  }
}

export async function generateCommentReply({
  originalComment = '',
  context = '',
  model = '',
  baseUrl = '',
  apiKey = '',
  token,
  lang = 'en',
  path = DEFAULT_AI_COMMENT_REPLY_PATH,
} = {}) {
  try {
    const response = await api(
      path,
      {
        method: 'POST',
        body: JSON.stringify({
          originalComment: normalizeField(originalComment),
          context: normalizeField(context),
          ...(normalizeField(model) ? { model: normalizeField(model) } : {}),
          ...(normalizeField(baseUrl) ? { baseUrl: normalizeField(baseUrl) } : {}),
          ...(normalizeField(apiKey) ? { apiKey: normalizeField(apiKey) } : {}),
        }),
      },
      token,
      lang,
    )
    const reply = normalizeText(response?.reply || response?.data?.reply || response?.result?.reply)
    if (!reply) throw new Error('Invalid AI comment reply response')
    return reply
  } catch (error) {
    if (error?.message === 'Invalid AI comment reply response') throw error
    throw toAiError(error, 'Failed to generate AI comment reply')
  }
}

export async function generateMarketingEmail({
  customerName = '',
  productName = '',
  customerInterest = '',
  model = '',
  baseUrl = '',
  apiKey = '',
  token,
  lang = 'en',
  path = DEFAULT_AI_MARKETING_EMAIL_PATH,
} = {}) {
  try {
    const response = await api(
      path,
      {
        method: 'POST',
        body: JSON.stringify({
          customerName: normalizeField(customerName),
          productName: normalizeField(productName),
          customerInterest: normalizeField(customerInterest),
          ...(normalizeField(model) ? { model: normalizeField(model) } : {}),
          ...(normalizeField(baseUrl) ? { baseUrl: normalizeField(baseUrl) } : {}),
          ...(normalizeField(apiKey) ? { apiKey: normalizeField(apiKey) } : {}),
        }),
      },
      token,
      lang,
    )
    const email = normalizeText(response?.email || response?.data?.email || response?.result?.email)
    if (!email) throw new Error('Invalid AI marketing email response')
    return email
  } catch (error) {
    if (error?.message === 'Invalid AI marketing email response') throw error
    throw toAiError(error, 'Failed to generate AI marketing email')
  }
}

export async function generateSalesAdvice({
  opportunityName = '',
  stage = '',
  customerName = '',
  lastActivity = '',
  model = '',
  baseUrl = '',
  apiKey = '',
  token,
  lang = 'en',
  path = DEFAULT_AI_SALES_ADVICE_PATH,
} = {}) {
  try {
    const response = await api(
      path,
      {
        method: 'POST',
        body: JSON.stringify({
          opportunityName: normalizeField(opportunityName),
          stage: normalizeField(stage),
          customerName: normalizeField(customerName),
          lastActivity: normalizeField(lastActivity),
          ...(normalizeField(model) ? { model: normalizeField(model) } : {}),
          ...(normalizeField(baseUrl) ? { baseUrl: normalizeField(baseUrl) } : {}),
          ...(normalizeField(apiKey) ? { apiKey: normalizeField(apiKey) } : {}),
        }),
      },
      token,
      lang,
    )
    const advice = normalizeText(response?.advice || response?.data?.advice || response?.result?.advice)
    if (!advice) throw new Error('Invalid AI sales advice response')
    return advice
  } catch (error) {
    if (error?.message === 'Invalid AI sales advice response') throw error
    throw toAiError(error, 'Failed to generate AI sales advice')
  }
}

export const AI_FOLLOW_UP_SUMMARY_PATH = DEFAULT_FOLLOW_UP_SUMMARY_PATH
export const AI_COMMENT_REPLY_PATH = DEFAULT_AI_COMMENT_REPLY_PATH
export const AI_MARKETING_EMAIL_PATH = DEFAULT_AI_MARKETING_EMAIL_PATH
export const AI_SALES_ADVICE_PATH = DEFAULT_AI_SALES_ADVICE_PATH
export const AI_STATUS_PATH = DEFAULT_AI_STATUS_PATH
export const AI_CONFIG_PATH = DEFAULT_AI_CONFIG_PATH
