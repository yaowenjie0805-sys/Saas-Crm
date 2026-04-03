import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import TopBar from '../components/layout/TopBar'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

const mountedRoots = []

const renderTopBar = async (props = {}) => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ container, root })

  const t = (key) => {
    if (key === 'aiShortcut') return 'AI功能'
    return key
  }

  const defaultProps = {
    t,
    currentPageLabel: '产品',
    lang: 'zh',
    setLang: () => {},
    accountLabel: '系统管理员',
    tenantId: 'tenant_demo',
    onLogout: () => {},
    onRefreshCurrentPage: () => {},
    onSearchSubmit: () => {},
    onGoToAi: () => {},
  }

  await act(async () => {
    root.render(<TopBar {...defaultProps} {...props} />)
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
})

describe('TopBar AI shortcut', () => {
  it('renders AI shortcut button with translated label', async () => {
    const { container } = await renderTopBar()

    const button = container.querySelector('[data-testid="topbar-ai-shortcut"]')
    expect(button).not.toBeNull()
    expect(button.textContent).toContain('AI功能')
  })

  it('triggers onGoToAi when AI shortcut button is clicked', async () => {
    const onGoToAi = vi.fn()
    const { container } = await renderTopBar({ onGoToAi })
    const button = container.querySelector('[data-testid="topbar-ai-shortcut"]')

    expect(button).not.toBeNull()
    await act(async () => {
      button.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    })

    expect(onGoToAi).toHaveBeenCalledTimes(1)
  })
})
