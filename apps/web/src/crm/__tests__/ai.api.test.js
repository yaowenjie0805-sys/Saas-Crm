import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  extractAiErrorMessage,
  fetchAiConfig,
  generateCommentReply,
  generateFollowUpSummary,
  generateMarketingEmail,
  generateSalesAdvice,
  queryAiAvailability,
} from '../api/ai'

const apiMock = vi.hoisted(() => vi.fn())

vi.mock('../shared', () => ({
  api: (...args) => apiMock(...args),
}))

afterEach(() => {
  apiMock.mockReset()
})

describe('generateFollowUpSummary', () => {
  it('calls AI summary endpoint with normalized payload', async () => {
    apiMock.mockResolvedValueOnce({ summary: 'Follow-up summary text' })

    const result = await generateFollowUpSummary({
      customerName: 'Acme',
      channel: 'Phone',
      interactionDetails: 'Customer requested proposal revision.',
      model: 'gpt-4o',
      token: 'token-x',
      lang: 'en',
    })

    expect(apiMock).toHaveBeenCalledWith(
      '/v1/ai/followUpSummary',
      {
        method: 'POST',
        body: JSON.stringify({
          customerName: 'Acme',
          channel: 'Phone',
          interactionDetails: 'Customer requested proposal revision.',
          model: 'gpt-4o',
        }),
      },
      'token-x',
      'en',
    )
    expect(result).toBe('Follow-up summary text')
  })

  it('includes custom baseUrl and apiKey when provided', async () => {
    apiMock.mockResolvedValueOnce({ summary: 'Follow-up summary text' })

    await generateFollowUpSummary({
      customerName: 'Acme',
      channel: 'Phone',
      interactionDetails: 'Customer requested proposal revision.',
      model: 'gpt-4o',
      baseUrl: 'http://localhost:11434/v1',
      apiKey: 'sk-local',
      token: 'token-x',
      lang: 'en',
    })

    expect(apiMock).toHaveBeenCalledWith(
      '/v1/ai/followUpSummary',
      {
        method: 'POST',
        body: JSON.stringify({
          customerName: 'Acme',
          channel: 'Phone',
          interactionDetails: 'Customer requested proposal revision.',
          model: 'gpt-4o',
          baseUrl: 'http://localhost:11434/v1',
          apiKey: 'sk-local',
        }),
      },
      'token-x',
      'en',
    )
  })

  it('extracts summary from nested data shape', async () => {
    apiMock.mockResolvedValueOnce({ data: { summary: 'Nested summary' } })

    const result = await generateFollowUpSummary({
      interactionDetails: 'Any notes',
    })

    expect(result).toBe('Nested summary')
  })

  it('throws when summary field is missing', async () => {
    apiMock.mockResolvedValueOnce({ ok: true })

    await expect(
      generateFollowUpSummary({
        interactionDetails: 'Any notes',
      }),
    ).rejects.toThrow('Invalid AI summary response')
  })

  it('surfaces server error context in thrown message', async () => {
    const error = new Error('Request failed')
    error.code = 'AI_TIMEOUT'
    error.requestId = 'req-1001'
    error.status = 503
    apiMock.mockRejectedValueOnce(error)

    await expect(
      generateFollowUpSummary({
        interactionDetails: 'Any notes',
      }),
    ).rejects.toMatchObject({
      message: 'Request failed (code=AI_TIMEOUT, requestId=req-1001)',
      code: 'AI_TIMEOUT',
      requestId: 'req-1001',
      status: 503,
    })
  })
})

describe('queryAiAvailability', () => {
  it('calls AI status endpoint and reads availability', async () => {
    apiMock.mockResolvedValueOnce({ data: { available: false, message: 'Maintenance window' } })

    const result = await queryAiAvailability({
      token: 'token-y',
      lang: 'zh',
    })

    expect(apiMock).toHaveBeenCalledWith('/v1/ai/status', { method: 'GET' }, 'token-y', 'zh')
    expect(result).toEqual({ available: false, message: 'Maintenance window' })
  })

  it('maps status text to available=true', async () => {
    apiMock.mockResolvedValueOnce({ status: 'UP' })

    const result = await queryAiAvailability()

    expect(result).toEqual({ available: true, message: '' })
  })
})

describe('fetchAiConfig', () => {
  it('calls AI config endpoint and normalizes response shape', async () => {
    apiMock.mockResolvedValueOnce({
      availableModels: ['gpt-4o', 'claude-3-5-sonnet'],
      defaultModel: 'gpt-4o',
      canOverride: true,
    })

    const result = await fetchAiConfig({ token: 'token-z', lang: 'en' })

    expect(apiMock).toHaveBeenCalledWith('/v1/ai/config', { method: 'GET' }, 'token-z', 'en')
    expect(result).toEqual({
      availableModels: ['gpt-4o', 'claude-3-5-sonnet'],
      defaultModel: 'gpt-4o',
      canOverride: true,
      supportsCustomConnection: true,
    })
  })
})

describe('generateCommentReply', () => {
  it('calls comment reply endpoint with custom model and connection fields', async () => {
    apiMock.mockResolvedValueOnce({ reply: 'Thanks for sharing your feedback.' })

    const result = await generateCommentReply({
      originalComment: 'Your product broke after update.',
      context: 'Need a calm support response',
      model: 'claude-3-5-sonnet',
      baseUrl: 'http://localhost:11434/v1',
      apiKey: 'sk-local',
      token: 'token-comment',
      lang: 'zh',
    })

    expect(apiMock).toHaveBeenCalledWith(
      '/v1/ai/commentReply',
      {
        method: 'POST',
        body: JSON.stringify({
          originalComment: 'Your product broke after update.',
          context: 'Need a calm support response',
          model: 'claude-3-5-sonnet',
          baseUrl: 'http://localhost:11434/v1',
          apiKey: 'sk-local',
        }),
      },
      'token-comment',
      'zh',
    )
    expect(result).toBe('Thanks for sharing your feedback.')
  })
})

describe('generateMarketingEmail', () => {
  it('calls marketing email endpoint with custom model and connection fields', async () => {
    apiMock.mockResolvedValueOnce({ email: 'Hello Alice, here is a tailored offer...' })

    const result = await generateMarketingEmail({
      customerName: 'Alice',
      productName: 'CRM Pro',
      customerInterest: 'Automation and analytics',
      model: 'gpt-4.1-mini',
      baseUrl: 'http://localhost:11434/v1',
      apiKey: 'sk-local',
      token: 'token-email',
      lang: 'en',
    })

    expect(apiMock).toHaveBeenCalledWith(
      '/v1/ai/marketingEmail',
      {
        method: 'POST',
        body: JSON.stringify({
          customerName: 'Alice',
          productName: 'CRM Pro',
          customerInterest: 'Automation and analytics',
          model: 'gpt-4.1-mini',
          baseUrl: 'http://localhost:11434/v1',
          apiKey: 'sk-local',
        }),
      },
      'token-email',
      'en',
    )
    expect(result).toBe('Hello Alice, here is a tailored offer...')
  })
})

describe('generateSalesAdvice', () => {
  it('calls sales advice endpoint with custom model and connection fields', async () => {
    apiMock.mockResolvedValueOnce({ advice: 'Prioritize executive alignment and next-step commitment.' })

    const result = await generateSalesAdvice({
      opportunityName: 'Acme Expansion',
      stage: 'Proposal',
      customerName: 'Acme',
      lastActivity: 'Demo completed yesterday, waiting for legal feedback.',
      model: 'qwen-plus',
      baseUrl: 'http://localhost:11434/v1',
      apiKey: 'sk-local',
      token: 'token-advice',
      lang: 'en',
    })

    expect(apiMock).toHaveBeenCalledWith(
      '/v1/ai/salesAdvice',
      {
        method: 'POST',
        body: JSON.stringify({
          opportunityName: 'Acme Expansion',
          stage: 'Proposal',
          customerName: 'Acme',
          lastActivity: 'Demo completed yesterday, waiting for legal feedback.',
          model: 'qwen-plus',
          baseUrl: 'http://localhost:11434/v1',
          apiKey: 'sk-local',
        }),
      },
      'token-advice',
      'en',
    )
    expect(result).toBe('Prioritize executive alignment and next-step commitment.')
  })
})

describe('extractAiErrorMessage', () => {
  it('prefers server payload message/code/requestId', () => {
    const message = extractAiErrorMessage({
      message: 'Request failed',
      code: 'CLIENT_GENERIC',
      requestId: 'client-1',
      response: {
        data: {
          message: 'AI overloaded',
          code: 'AI_BUSY',
          requestId: 'req-9',
        },
      },
    })

    expect(message).toBe('AI overloaded (code=AI_BUSY, requestId=req-9)')
  })
})
