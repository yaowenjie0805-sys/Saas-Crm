import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AiFollowUpSummarySection from '../components/pages/dashboard/AiFollowUpSummarySection'

const queryAiAvailabilityMock = vi.hoisted(() => vi.fn())
const generateFollowUpSummaryMock = vi.hoisted(() => vi.fn())
const extractAiErrorMessageMock = vi.hoisted(() => vi.fn())

vi.mock('../api/ai', () => ({
  queryAiAvailability: (...args) => queryAiAvailabilityMock(...args),
  generateFollowUpSummary: (...args) => generateFollowUpSummaryMock(...args),
  extractAiErrorMessage: (...args) => extractAiErrorMessageMock(...args),
}))

const mountedRoots = []
globalThis.IS_REACT_ACT_ENVIRONMENT = true

const defaultT = (key) => key

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

const renderSection = async ({
  t = defaultT,
  apiContext = { token: 'token-default', lang: 'en' },
  initialInteractionDetails = '',
} = {}) => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ container, root })

  await act(async () => {
    root.render(
      <AiFollowUpSummarySection
        t={t}
        apiContext={apiContext}
        initialInteractionDetails={initialInteractionDetails}
      />,
    )
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
  generateFollowUpSummaryMock.mockReset()
  extractAiErrorMessageMock.mockReset()
})

describe('AiFollowUpSummarySection', () => {
  it('calls queryAiAvailability on initial load', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })

    await renderSection({ apiContext: { token: 'token-qa', lang: 'zh' } })

    await waitFor(() => {
      expect(queryAiAvailabilityMock).toHaveBeenCalledTimes(1)
    })

    expect(queryAiAvailabilityMock).toHaveBeenCalledWith({ token: 'token-qa', lang: 'zh' })
  })

  it('disables generate button and shows unavailable message when AI is unavailable', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: false, message: 'Maintenance window' })

    const { container } = await renderSection()

    await waitFor(() => {
      const unavailable = getByTestId(container, 'ai-followup-summary-unavailable')
      expect(unavailable).not.toBeNull()
      expect(unavailable.textContent).toContain('Maintenance window')
    })

    const submitButton = getByTestId(container, 'ai-followup-summary-submit')
    expect(submitButton).not.toBeNull()
    expect(submitButton.disabled).toBe(true)
  })

  it('uses aiFollowUpSummaryUnavailable translation when unavailable response has no message', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: false, message: '' })
    const t = (key) => {
      if (key === 'aiFollowUpSummaryUnavailable') return 'AI is temporarily offline'
      return key
    }

    const { container } = await renderSection({ t })

    await waitFor(() => {
      const unavailable = getByTestId(container, 'ai-followup-summary-unavailable')
      expect(unavailable).not.toBeNull()
      expect(unavailable.textContent).toContain('AI is temporarily offline')
    })
  })

  it('shows generated result when input is valid and submit is clicked', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    generateFollowUpSummaryMock.mockResolvedValueOnce('Follow-up summary generated')

    const { container } = await renderSection({
      apiContext: { token: 'token-gen', lang: 'en' },
      initialInteractionDetails: 'Customer asked for updated pricing.',
    })
    const submitButton = getByTestId(container, 'ai-followup-summary-submit')

    await waitFor(() => {
      expect(submitButton.disabled).toBe(false)
    })

    await act(async () => {
      submitButton.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    })

    await waitFor(() => {
      expect(generateFollowUpSummaryMock).toHaveBeenCalledTimes(1)
    })

    expect(generateFollowUpSummaryMock).toHaveBeenCalledWith({
      customerName: '',
      channel: '',
      interactionDetails: 'Customer asked for updated pricing.',
      token: 'token-gen',
      lang: 'en',
    })

    await waitFor(() => {
      const result = getByTestId(container, 'ai-followup-summary-result')
      expect(result).not.toBeNull()
      expect(result.textContent).toContain('Follow-up summary generated')
    })
  })

  it('renders interaction details input as textarea', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })

    const { container } = await renderSection()
    const interactionInput = getByTestId(container, 'ai-followup-summary-input')

    expect(interactionInput).not.toBeNull()
    expect(interactionInput.tagName).toBe('TEXTAREA')
  })

  it('does not submit when pressing Enter without modifiers in interaction textarea', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    generateFollowUpSummaryMock.mockResolvedValueOnce('should not submit with Enter only')

    const { container } = await renderSection({
      initialInteractionDetails: 'Follow up with product update.',
    })
    const submitButton = getByTestId(container, 'ai-followup-summary-submit')
    const interactionInput = getByTestId(container, 'ai-followup-summary-input')

    await waitFor(() => {
      expect(submitButton.disabled).toBe(false)
    })

    await act(async () => {
      interactionInput.dispatchEvent(
        new KeyboardEvent('keydown', {
          key: 'Enter',
          bubbles: true,
        }),
      )
    })

    expect(generateFollowUpSummaryMock).not.toHaveBeenCalled()
  })

  it('submits when pressing Ctrl+Enter in interaction textarea', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    generateFollowUpSummaryMock.mockResolvedValueOnce('Generated with Ctrl+Enter')

    const { container } = await renderSection({
      initialInteractionDetails: 'Need a summary via shortcut.',
    })
    const submitButton = getByTestId(container, 'ai-followup-summary-submit')
    const interactionInput = getByTestId(container, 'ai-followup-summary-input')

    await waitFor(() => {
      expect(submitButton.disabled).toBe(false)
    })

    await act(async () => {
      interactionInput.dispatchEvent(
        new KeyboardEvent('keydown', {
          key: 'Enter',
          ctrlKey: true,
          bubbles: true,
        }),
      )
    })

    await waitFor(() => {
      expect(generateFollowUpSummaryMock).toHaveBeenCalledTimes(1)
    })
  })

  it('submits when pressing Cmd+Enter in interaction textarea', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    generateFollowUpSummaryMock.mockResolvedValueOnce('Generated with Cmd+Enter')

    const { container } = await renderSection({
      initialInteractionDetails: 'Need a summary via mac shortcut.',
    })
    const submitButton = getByTestId(container, 'ai-followup-summary-submit')
    const interactionInput = getByTestId(container, 'ai-followup-summary-input')

    await waitFor(() => {
      expect(submitButton.disabled).toBe(false)
    })

    await act(async () => {
      interactionInput.dispatchEvent(
        new KeyboardEvent('keydown', {
          key: 'Enter',
          metaKey: true,
          bubbles: true,
        }),
      )
    })

    await waitFor(() => {
      expect(generateFollowUpSummaryMock).toHaveBeenCalledTimes(1)
    })
  })

  it('does not submit on Ctrl+Enter while availability check is in progress', async () => {
    queryAiAvailabilityMock.mockImplementation(
      () => new Promise(() => {}),
    )
    generateFollowUpSummaryMock.mockResolvedValueOnce('should not submit while checking')

    const { container } = await renderSection({
      initialInteractionDetails: 'Shortcut should be blocked while checking availability.',
    })
    const submitButton = getByTestId(container, 'ai-followup-summary-submit')
    const interactionInput = getByTestId(container, 'ai-followup-summary-input')

    expect(submitButton.disabled).toBe(true)

    await act(async () => {
      interactionInput.dispatchEvent(
        new KeyboardEvent('keydown', {
          key: 'Enter',
          ctrlKey: true,
          bubbles: true,
        }),
      )
    })

    expect(generateFollowUpSummaryMock).not.toHaveBeenCalled()
  })

  it('shows error message with code and requestId when generation fails', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    const aiError = Object.assign(new Error('Gateway timeout'), {
      code: 'AI_TIMEOUT',
      requestId: 'req-1001',
    })

    generateFollowUpSummaryMock.mockRejectedValueOnce(aiError)
    extractAiErrorMessageMock.mockImplementation((error, fallbackMessage) => {
      return `${fallbackMessage} (code=${error.code}, requestId=${error.requestId})`
    })

    const { container } = await renderSection({
      initialInteractionDetails: 'Need to follow up about outage incident.',
    })
    const submitButton = getByTestId(container, 'ai-followup-summary-submit')

    await waitFor(() => {
      expect(submitButton.disabled).toBe(false)
    })

    await act(async () => {
      submitButton.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    })

    await waitFor(() => {
      const errorBanner = getByTestId(container, 'ai-followup-summary-error')
      expect(errorBanner).not.toBeNull()
      expect(errorBanner.textContent).toContain('code=AI_TIMEOUT')
      expect(errorBanner.textContent).toContain('requestId=req-1001')
    })
  })

  it('blocks submit and shows translated error when interaction details exceed max length', async () => {
    queryAiAvailabilityMock.mockResolvedValueOnce({ available: true, message: '' })
    generateFollowUpSummaryMock.mockResolvedValueOnce('should not be generated')

    const t = (key) => {
      if (key === 'aiFollowUpSummaryInputTooLong') return 'Interaction details must be 4000 characters or less'
      return key
    }

    const { container } = await renderSection({
      t,
      initialInteractionDetails: 'x'.repeat(4001),
    })
    const submitButton = getByTestId(container, 'ai-followup-summary-submit')

    await waitFor(() => {
      expect(submitButton.disabled).toBe(false)
    })

    await act(async () => {
      submitButton.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    })

    await waitFor(() => {
      const errorBanner = getByTestId(container, 'ai-followup-summary-error')
      expect(errorBanner).not.toBeNull()
      expect(errorBanner.textContent).toContain(
        'Interaction details must be 4000 characters or less',
      )
    })

    expect(generateFollowUpSummaryMock).not.toHaveBeenCalled()
  })
})
