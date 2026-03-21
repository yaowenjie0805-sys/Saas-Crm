import { Component } from 'react'

const ERROR_REPORT_ENDPOINT = '/api/v1/ops/client-errors'
const MAX_ERROR_REPORTS_PER_SESSION = 10
let sessionErrorCount = 0

function reportErrorToServer(error, errorInfo) {
  if (sessionErrorCount >= MAX_ERROR_REPORTS_PER_SESSION) return
  sessionErrorCount++
  try {
    const payload = {
      message: String(error?.message || 'Unknown error'),
      stack: String(error?.stack || '').slice(0, 2000),
      componentStack: String(errorInfo?.componentStack || '').slice(0, 2000),
      url: window.location.href,
      userAgent: navigator.userAgent,
      timestamp: new Date().toISOString(),
    }
    if (navigator.sendBeacon) {
      navigator.sendBeacon(ERROR_REPORT_ENDPOINT, JSON.stringify(payload))
    } else {
      fetch(ERROR_REPORT_ENDPOINT, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        keepalive: true,
      }).catch(() => {})
    }
  } catch (_e) {
    // Swallow reporting errors to prevent cascading failures
  }
}

class CrmErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    if (import.meta.env.DEV) {
      console.error('[CRM_PAGE_ERROR_BOUNDARY]', error, errorInfo)
    } else {
      reportErrorToServer(error, errorInfo)
    }
  }

  componentDidUpdate(prevProps) {
    if (this.state.hasError && prevProps.resetKey !== this.props.resetKey) {
      this.setState({ hasError: false, error: null })
    }
  }

  render() {
    const { hasError, error } = this.state
    const { t, onRetry, onBackToDashboard, children } = this.props
    if (!hasError) return children

    return (
      <section className="panel error-fallback-card" role="alert" data-testid="error-boundary">
        <h3>{t('requestFailed')}</h3>
        <p className="small-tip">{error?.message || t('requestFailed')}</p>
        <div className="inline-tools">
          <button className="primary-btn" type="button" onClick={onRetry}>
            {t('refresh')}
          </button>
          <button className="mini-btn" type="button" onClick={onBackToDashboard}>
            {t('dashboard')}
          </button>
        </div>
      </section>
    )
  }
}

export default CrmErrorBoundary
