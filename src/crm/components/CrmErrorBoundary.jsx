import { Component } from 'react'

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
      // Keep detailed diagnostics in dev without breaking the full app shell.
      console.error('[CRM_PAGE_ERROR_BOUNDARY]', error, errorInfo)
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
