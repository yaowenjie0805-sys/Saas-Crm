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

const bootstrap = async () => {
  await ensureI18nBase(detectLang())
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  )
}

bootstrap().catch(() => {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  )
})
