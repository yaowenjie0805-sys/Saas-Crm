import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { ensureI18nBase } from './crm/i18n'

const detectLang = () => {
  try {
    const value = String(localStorage.getItem('crm_lang') || '').trim().toLowerCase()
    return value === 'zh' ? 'zh' : 'en'
  } catch {
    return 'en'
  }
}

const bootstrap = () => {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  )

  void ensureI18nBase(detectLang()).catch(() => {})
}

bootstrap()
