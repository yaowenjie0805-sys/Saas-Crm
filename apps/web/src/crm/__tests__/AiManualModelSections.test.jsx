import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AiCommentReplySection from '../components/pages/dashboard/AiCommentReplySection'
import AiMarketingEmailSection from '../components/pages/dashboard/AiMarketingEmailSection'
import AiSalesAdviceSection from '../components/pages/dashboard/AiSalesAdviceSection'

const queryAiAvailabilityMock = vi.hoisted(() => vi.fn())
const fetchAiConfigMock = vi.hoisted(() => vi.fn())
const generateCommentReplyMock = vi.hoisted(() => vi.fn())
const generateMarketingEmailMock = vi.hoisted(() => vi.fn())
const generateSalesAdviceMock = vi.hoisted(() => vi.fn())
const extractAiErrorMessageMock = vi.hoisted(() => vi.fn())

vi.mock('../api/ai', () => ({
  queryAiAvailability: (...args) => queryAiAvailabilityMock(...args),
  fetchAiConfig: (...args) => fetchAiConfigMock(...args),
  generateCommentReply: (...args) => generateCommentReplyMock(...args),
  generateMarketingEmail: (...args) => generateMarketingEmailMock(...args),
  generateSalesAdvice: (...args) => generateSalesAdviceMock(...args),
  extractAiErrorMessage: (...args) => extractAiErrorMessageMock(...args),
}))

globalThis.IS_REACT_ACT_ENVIRONMENT = true

const mountedRoots = []
const defaultT = (key) => key
const defaultApiContext = { token: 'token-default', lang: 'en' }

const getByTestId = (container, testId) => container.querySelector(`[data-testid="${testId}"]`)

const waitFor = async (assertion, timeoutMs = 1500) => {
  const startedAt = Date.now()
  let lastError

  while (Date.now() - startedAt < timeoutMs) {
    try {
      assertion()
      return
    } catch (error) {
      lastError = error
      await new Promise((resolve) => setTimeout(resolve, 10))
    }
  }

  throw lastError
}

const renderSection = async (Component) => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ container, root })

  await act(async () => {
    root.render(<Component t={defaultT} apiContext={defaultApiContext} />)
  })

  return { container }
}

afterEach(async () => {
  while (mountedRoots.length) {
    const mounted = mountedRoots.pop()
    await act(async () => {
      mounted.root.unmount()
    })
    mounted.container.remove()
  }

  queryAiAvailabilityMock.mockReset()
  fetchAiConfigMock.mockReset()
  generateCommentReplyMock.mockReset()
  generateMarketingEmailMock.mockReset()
  generateSalesAdviceMock.mockReset()
  extractAiErrorMessageMock.mockReset()
})

describe('AI manual model inputs', () => {
  it('keeps comment reply model input empty by default even when config returns a default model', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    fetchAiConfigMock.mockResolvedValueOnce({
      availableModels: ['gpt-4o', 'claude-3-5-sonnet'],
      defaultModel: 'gpt-4o',
      canOverride: true,
    })

    const { container } = await renderSection(AiCommentReplySection)

    await waitFor(() => {
      expect(fetchAiConfigMock).toHaveBeenCalledTimes(1)
    })

    expect(getByTestId(container, 'ai-comment-reply-model')?.value).toBe('')
  })

  it('keeps marketing email model input empty by default even when config returns a default model', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    fetchAiConfigMock.mockResolvedValueOnce({
      availableModels: ['gpt-4o', 'claude-3-5-sonnet'],
      defaultModel: 'gpt-4o',
      canOverride: true,
    })

    const { container } = await renderSection(AiMarketingEmailSection)

    await waitFor(() => {
      expect(fetchAiConfigMock).toHaveBeenCalledTimes(1)
    })

    expect(getByTestId(container, 'ai-marketing-email-model')?.value).toBe('')
  })

  it('keeps sales advice model input empty by default even when config returns a default model', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    fetchAiConfigMock.mockResolvedValueOnce({
      availableModels: ['gpt-4o', 'claude-3-5-sonnet'],
      defaultModel: 'gpt-4o',
      canOverride: true,
    })

    const { container } = await renderSection(AiSalesAdviceSection)

    await waitFor(() => {
      expect(fetchAiConfigMock).toHaveBeenCalledTimes(1)
    })

    expect(getByTestId(container, 'ai-sales-advice-model')?.value).toBe('')
  })
})
