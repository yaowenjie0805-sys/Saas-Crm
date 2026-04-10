import { memo, useEffect, useState } from 'react'
import {
  extractAiErrorMessage,
  fetchAiConfig,
  generateSalesAdvice,
  queryAiAvailability,
} from '../../../api/ai'

const AI_UNAVAILABLE_FALLBACK = 'AI service is currently unavailable. Please try again later.'
const AI_GENERATE_ERROR_FALLBACK = 'Failed to generate sales advice'
const AI_BASE_URL_STORAGE_PREFIX = 'crm.ai.salesadvice.baseurl.'

const readText = (t, key, fallback) => {
  const translated = typeof t === 'function' ? String(t(key) || '').trim() : ''
  if (!translated || translated === key) return fallback
  return translated
}

function AiSalesAdviceSection({ t, apiContext }) {
  const [opportunityName, setOpportunityName] = useState('')
  const [stage, setStage] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [lastActivity, setLastActivity] = useState('')
  const [result, setResult] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [checkingAvailability, setCheckingAvailability] = useState(true)
  const [aiAvailable, setAiAvailable] = useState(true)
  const [availabilityMessage, setAvailabilityMessage] = useState('')
  const [availableModels, setAvailableModels] = useState([])
  const [selectedModel, setSelectedModel] = useState('')
  const [canOverrideModel, setCanOverrideModel] = useState(true)
  const [supportsCustomConnection, setSupportsCustomConnection] = useState(true)
  const [baseUrl, setBaseUrl] = useState('')
  const [apiKey, setApiKey] = useState('')

  const baseUrlStorageKey = `${AI_BASE_URL_STORAGE_PREFIX}${apiContext?.tenantId || '__NO_TENANT__'}`

  useEffect(() => {
    let active = true
    const checkAvailability = async () => {
      setCheckingAvailability(true)
      try {
        const [status, config] = await Promise.all([
          queryAiAvailability({ token: apiContext?.token || '', lang: apiContext?.lang || 'en' }),
          fetchAiConfig({ token: apiContext?.token || '', lang: apiContext?.lang || 'en' }).catch(() => ({
            availableModels: [],
            defaultModel: '',
            canOverride: false,
          })),
        ])
        if (!active) return
        const configuredModels = Array.isArray(config?.availableModels) ? config.availableModels : []
        const persistedBaseUrl = String(localStorage.getItem(baseUrlStorageKey) || '').trim()
        setAvailableModels(configuredModels)
        setCanOverrideModel(config?.canOverride !== false)
        setSupportsCustomConnection(config?.supportsCustomConnection !== false)
        setSelectedModel('')
        setBaseUrl(persistedBaseUrl)
        const available = status?.available !== false
        setAiAvailable(available)
        setAvailabilityMessage(available ? '' : status?.message || readText(t, 'aiSalesAdviceUnavailable', AI_UNAVAILABLE_FALLBACK))
      } catch (err) {
        if (!active) return
        setAiAvailable(false)
        setAvailabilityMessage(extractAiErrorMessage(err, readText(t, 'aiSalesAdviceUnavailable', AI_UNAVAILABLE_FALLBACK)))
      } finally {
        if (active) setCheckingAvailability(false)
      }
    }
    checkAvailability()
    return () => {
      active = false
    }
  }, [apiContext?.lang, apiContext?.token, baseUrlStorageKey, t])

  useEffect(() => {
    const normalizedBaseUrl = String(baseUrl || '').trim()
    if (!normalizedBaseUrl) {
      localStorage.removeItem(baseUrlStorageKey)
      return
    }
    localStorage.setItem(baseUrlStorageKey, normalizedBaseUrl)
  }, [baseUrl, baseUrlStorageKey])

  const handleGenerate = async () => {
    if (!aiAvailable) {
      setError(availabilityMessage || readText(t, 'aiSalesAdviceUnavailable', AI_UNAVAILABLE_FALLBACK))
      return
    }
    if (!opportunityName.trim() || !stage.trim() || !customerName.trim() || !lastActivity.trim()) {
      setError(t('aiSalesAdviceEmptyInput'))
      return
    }
    setLoading(true)
    setError('')
    try {
      const advice = await generateSalesAdvice({
        opportunityName,
        stage,
        customerName,
        lastActivity,
        model: selectedModel,
        ...(String(baseUrl || '').trim() ? { baseUrl } : {}),
        ...(String(apiKey || '').trim() ? { apiKey } : {}),
        token: apiContext?.token || '',
        lang: apiContext?.lang || 'en',
      })
      setResult(advice)
    } catch (err) {
      setError(extractAiErrorMessage(err, readText(t, 'aiSalesAdviceError', AI_GENERATE_ERROR_FALLBACK)))
      setResult('')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="panel" id="ai-sales-advice-section" data-testid="ai-sales-advice-panel">
      <div className="panel-head">
        <h2>{t('aiSalesAdviceTitle')}</h2>
      </div>
      <div className="small-tip" style={{ marginBottom: 8 }}>
        {t('aiSalesAdviceHint')}
      </div>
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input
          className="tool-input"
          data-testid="ai-sales-advice-model"
          list="ai-sales-advice-model-list"
          placeholder={readText(t, 'aiModelIdPlaceholder', 'Model ID (required)')}
          value={selectedModel}
          onChange={(event) => setSelectedModel(event.target.value)}
          disabled={!canOverrideModel}
        />
        <datalist id="ai-sales-advice-model-list">
          {(availableModels.length > 0 ? availableModels : ['gpt-4o']).map((model) => (
            <option key={model} value={model} />
          ))}
        </datalist>
        <input
          className="tool-input"
          placeholder={t('aiSalesAdviceOpportunityPlaceholder')}
          value={opportunityName}
          onChange={(event) => setOpportunityName(event.target.value)}
        />
        <input
          className="tool-input"
          placeholder={t('aiSalesAdviceStagePlaceholder')}
          value={stage}
          onChange={(event) => setStage(event.target.value)}
        />
        <input
          className="tool-input"
          placeholder={t('aiSalesAdviceCustomerPlaceholder')}
          value={customerName}
          onChange={(event) => setCustomerName(event.target.value)}
        />
        {supportsCustomConnection ? (
          <input
            className="tool-input"
            data-testid="ai-sales-advice-base-url"
            placeholder={readText(t, 'aiBaseUrlPlaceholder', 'Base URL (optional)')}
            value={baseUrl}
            onChange={(event) => setBaseUrl(event.target.value)}
          />
        ) : null}
        {supportsCustomConnection ? (
          <input
            className="tool-input"
            data-testid="ai-sales-advice-api-key"
            type="password"
            autoComplete="off"
            placeholder={readText(t, 'aiApiKeyPlaceholder', 'API Key (optional, session only)')}
            value={apiKey}
            onChange={(event) => setApiKey(event.target.value)}
          />
        ) : null}
        <textarea
          className="tool-input"
          data-testid="ai-sales-advice-last-activity"
          placeholder={t('aiSalesAdviceLastActivityPlaceholder')}
          value={lastActivity}
          onChange={(event) => setLastActivity(event.target.value)}
          rows={3}
        />
        <button
          type="button"
          className="mini-btn"
          data-testid="ai-sales-advice-submit"
          onClick={handleGenerate}
          disabled={loading || checkingAvailability || !aiAvailable}
        >
          {loading ? t('loading') : t('aiSalesAdviceGenerate')}
        </button>
      </div>
      {!aiAvailable && availabilityMessage ? (
        <div className="error-banner" data-testid="ai-sales-advice-unavailable">
          {availabilityMessage}
        </div>
      ) : null}
      {error ? <div className="error-banner">{error}</div> : null}
      {result ? (
        <div className="info-banner">
          {t('aiSalesAdviceResultLabel')}: {result}
        </div>
      ) : null}
    </section>
  )
}

export default memo(AiSalesAdviceSection)
