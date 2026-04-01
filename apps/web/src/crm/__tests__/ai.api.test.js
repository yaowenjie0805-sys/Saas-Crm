import { afterEach, describe, expect, it, vi } from 'vitest'
import { extractAiErrorMessage, generateFollowUpSummary, queryAiAvailability } from '../api/ai'

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
        }),
      },
      'token-x',
      'en',
    )
    expect(result).toBe('Follow-up summary text')
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
