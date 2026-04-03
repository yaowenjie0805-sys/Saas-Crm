import { memo, useEffect, useState } from 'react'
import { extractAiErrorMessage, generateFollowUpSummary, queryAiAvailability } from '../../../api/ai'

const AI_UNAVAILABLE_FALLBACK = 'AI service is currently unavailable. Please try again later.'
const AI_GENERATE_ERROR_FALLBACK = 'Failed to generate summary'
const INTERACTION_DETAILS_MAX_LENGTH = 4000
const AI_INPUT_TOO_LONG_FALLBACK = `Interaction details must be ${INTERACTION_DETAILS_MAX_LENGTH} characters or less`

const readText = (t, key, fallback) => {
  const translated = typeof t === 'function' ? String(t(key) || '').trim() : ''
  if (!translated || translated === key) return fallback
  return translated
}

function AiFollowUpSummarySection({ t, apiContext, initialInteractionDetails = '' }) {
  const [customerName, setCustomerName] = useState('')
  const [channel, setChannel] = useState('')
  const [interactionDetails, setInteractionDetails] = useState(() =>
    String(initialInteractionDetails || ''),
  )
  const [result, setResult] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [checkingAvailability, setCheckingAvailability] = useState(true)
  const [aiAvailable, setAiAvailable] = useState(true)
  const [availabilityMessage, setAvailabilityMessage] = useState('')

  useEffect(() => {
    let active = true
    const checkAvailability = async () => {
      setCheckingAvailability(true)
      try {
        const status = await queryAiAvailability({
          token: apiContext?.token || '',
          lang: apiContext?.lang || 'en',
        })
        if (!active) return
        const available = status?.available !== false
        setAiAvailable(available)
        setAvailabilityMessage(
          available
            ? ''
            : status?.message ||
                readText(
                  t,
                  'aiFollowUpSummaryUnavailable',
                  AI_UNAVAILABLE_FALLBACK,
                ),
        )
      } catch (err) {
        if (!active) return
        setAiAvailable(false)
        setAvailabilityMessage(
          extractAiErrorMessage(
            err,
            readText(
              t,
              'aiFollowUpSummaryUnavailable',
              AI_UNAVAILABLE_FALLBACK,
            ),
          ),
        )
      } finally {
        if (active) setCheckingAvailability(false)
      }
    }
    checkAvailability()
    return () => {
      active = false
    }
  }, [apiContext?.lang, apiContext?.token, t])

  const handleGenerate = async () => {
    if (!aiAvailable) {
      setError(
        availabilityMessage ||
          readText(
            t,
            'aiFollowUpSummaryUnavailable',
            AI_UNAVAILABLE_FALLBACK,
          ),
      )
      return
    }

    if (!interactionDetails.trim()) {
      setError(t('aiFollowUpSummaryEmptyInput'))
      return
    }

    if (interactionDetails.length > INTERACTION_DETAILS_MAX_LENGTH) {
      setError(
        readText(
          t,
          'aiFollowUpSummaryInputTooLong',
          AI_INPUT_TOO_LONG_FALLBACK,
        ),
      )
      return
    }

    setLoading(true)
    setError('')
    try {
      const summary = await generateFollowUpSummary({
        customerName,
        channel,
        interactionDetails,
        token: apiContext?.token || '',
        lang: apiContext?.lang || 'en',
      })
      setResult(summary)
    } catch (err) {
      setError(
        extractAiErrorMessage(
          err,
          readText(t, 'aiFollowUpSummaryError', AI_GENERATE_ERROR_FALLBACK),
        ),
      )
      setResult('')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section
      className="panel"
      id="ai-followup-summary-section"
      data-testid="ai-followup-summary-panel"
    >
      <div className="panel-head">
        <h2>{t('aiFollowUpSummaryTitle')}</h2>
      </div>
      <div className="small-tip" style={{ marginBottom: 8 }}>
        {t('aiFollowUpSummaryHint')}
      </div>
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input
          className="tool-input"
          placeholder={t('aiFollowUpSummaryCustomerPlaceholder')}
          value={customerName}
          onChange={(event) => setCustomerName(event.target.value)}
        />
        <input
          className="tool-input"
          placeholder={t('aiFollowUpSummaryChannelPlaceholder')}
          value={channel}
          onChange={(event) => setChannel(event.target.value)}
        />
        <textarea
          className="tool-input"
          data-testid="ai-followup-summary-input"
          placeholder={t('aiFollowUpSummaryInputPlaceholder')}
          value={interactionDetails}
          onChange={(event) => setInteractionDetails(event.target.value)}
          onKeyDown={(event) => {
            if (
              event.key === 'Enter' &&
              (event.ctrlKey || event.metaKey) &&
              !loading &&
              !checkingAvailability &&
              aiAvailable
            ) {
              event.preventDefault()
              handleGenerate()
            }
          }}
          rows={4}
        />
        <button
          type="button"
          className="mini-btn"
          data-testid="ai-followup-summary-submit"
          onClick={handleGenerate}
          disabled={loading || checkingAvailability || !aiAvailable}
        >
          {loading ? t('loading') : t('aiFollowUpSummaryGenerate')}
        </button>
      </div>
      {!aiAvailable && availabilityMessage ? (
        <div className="error-banner" data-testid="ai-followup-summary-unavailable">
          {availabilityMessage}
        </div>
      ) : null}
      {error ? (
        <div className="error-banner" data-testid="ai-followup-summary-error">
          {error}
        </div>
      ) : null}
      {result ? (
        <div className="info-banner" data-testid="ai-followup-summary-result">
          {t('aiFollowUpSummaryResultLabel')}: {result}
        </div>
      ) : null}
    </section>
  )
}

export default memo(AiFollowUpSummarySection)
