import { beforeEach, describe, expect, it, vi } from 'vitest'

const createRootMock = vi.hoisted(() => vi.fn())
const renderMock = vi.hoisted(() => vi.fn())
const ensureI18nBaseMock = vi.hoisted(() => vi.fn())

vi.mock('react-dom/client', () => ({
  createRoot: (...args) => createRootMock(...args),
}))

vi.mock('./crm/i18n', () => ({
  ensureI18nBase: (...args) => ensureI18nBaseMock(...args),
}))

vi.mock('./App.jsx', () => ({
  default: () => null,
}))

vi.mock('./index.css', () => ({}))

describe('main bootstrap', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    document.body.innerHTML = '<div id="root"></div>'
    createRootMock.mockReturnValue({ render: renderMock })
  })

  it('renders app immediately even when ensureI18nBase is pending', async () => {
    ensureI18nBaseMock.mockReturnValue(new Promise(() => {}))

    await import('./main.jsx')

    expect(createRootMock).toHaveBeenCalledTimes(1)
    expect(renderMock).toHaveBeenCalledTimes(1)
    expect(ensureI18nBaseMock).toHaveBeenCalledTimes(1)
  })
})
