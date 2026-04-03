import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import MainContent from '../components/MainContent'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

const mountedRoots = []

const refreshPageMock = vi.fn()
const onWorkbenchNavigateMock = vi.fn()

const baseState = {
  currentPageLabel: 'Products',
  lang: 'zh',
  setLang: vi.fn(),
  refreshPage: refreshPageMock,
  t: (key) => key,
  canWrite: true,
  role: 'ADMIN',
  error: '',
  loading: false,
  activePage: 'products',
  quoteOpportunityFilter: '',
  orderOpportunityFilter: '',
  quotePrefill: null,
  consumeQuotePrefill: vi.fn(),
  auth: { token: 'token-main', tenantId: 'tenant_demo', displayName: 'Admin', role: 'ADMIN' },
  onLogout: vi.fn(),
  onWorkbenchNavigate: onWorkbenchNavigateMock,
}

vi.mock('../shared', () => ({
  translateOwnerAlias: (_t, value) => value,
  translateRole: (_t, value) => value,
}))

vi.mock('../hooks/useGlobalSearchBridge', () => ({
  useGlobalSearchBridge: () => vi.fn(),
}))

vi.mock('../components/CrmErrorBoundary', () => ({
  default: ({ children }) => <>{children}</>,
}))

vi.mock('../components/MainContentPanels', () => ({
  default: () => <div data-testid="main-content-panels" />,
}))

vi.mock('../components/layout/TopBar', () => ({
  default: ({ onGoToAi }) => (
    <button data-testid="mock-topbar-ai-btn" onClick={() => onGoToAi?.()}>
      mock-ai
    </button>
  ),
}))

vi.mock('../hooks/pageModels', () => ({
  useBasePageModel: () => baseState,
  usePermissionsPageModel: () => ({}),
  useUsersPageModel: () => ({}),
  useSalesAutomationPageModel: () => ({}),
  useCommercePageModel: () => ({}),
  useCustomersPageModel: () => ({}),
  usePipelinePageModel: () => ({}),
  useFollowUpsPageModel: () => ({}),
  useContactsPageModel: () => ({}),
  useContractsPageModel: () => ({}),
  usePaymentsPageModel: () => ({}),
  useLeadsPageModel: () => ({}),
  useReportDesignerPageModel: () => ({}),
  useTasksPageModel: () => ({}),
  useAuditPageModel: () => ({}),
  useApprovalsPageModel: () => ({}),
  useTenantsPageModel: () => ({}),
}))

const renderMainContent = async () => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ container, root })

  await act(async () => {
    root.render(<MainContent />)
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
  refreshPageMock.mockReset()
  onWorkbenchNavigateMock.mockReset()
  baseState.activePage = 'products'
})

describe('MainContent AI shortcut bridge', () => {
  it('navigates to dashboard via refresh and workbench bridge when shortcut is clicked off-dashboard', async () => {
    const aiSection = document.createElement('section')
    aiSection.id = 'ai-followup-summary-section'
    aiSection.scrollIntoView = vi.fn()
    document.body.appendChild(aiSection)

    const { container } = await renderMainContent()
    const button = container.querySelector('[data-testid="mock-topbar-ai-btn"]')

    expect(button).not.toBeNull()
    await act(async () => {
      button.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    })

    expect(refreshPageMock).toHaveBeenCalledWith('dashboard', 'topbar_ai_shortcut')
    expect(onWorkbenchNavigateMock).toHaveBeenCalledWith('dashboard')
    expect(aiSection.scrollIntoView).toHaveBeenCalledTimes(1)

    aiSection.remove()
  })
})
