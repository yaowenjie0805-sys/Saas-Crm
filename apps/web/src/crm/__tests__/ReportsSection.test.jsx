import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it } from 'vitest'
import ReportsSection from '../components/pages/dashboard/ReportsSection'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

const mountedRoots = []

const renderSection = async (props) => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ container, root })
  await act(async () => {
    root.render(<ReportsSection {...props} />)
  })
  return container
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

describe('ReportsSection', () => {
  const t = (key) => key

  it('renders forecastAccuracy and prefers summary pipelineHealth over localizedMetrics', async () => {
    const container = await renderSection({
      t,
      reportCurrency: 'CNY',
      reports: {
        summary: {
          customers: 5,
          revenue: 120000,
          opportunities: 4,
          taskDoneRate: 75,
          winRate: 50,
          forecastAccuracy: 88.8,
          pipelineHealth: 66.6,
        },
        localizedMetrics: {
          pipelineHealth: 12.3,
          arrLike: 90000,
        },
        customerByOwner: {},
        revenueByStatus: {},
        opportunityByStage: {},
        followUpByChannel: {},
      },
    })

    expect(container.textContent).toContain('forecastAccuracy')
    expect(container.textContent).toContain('88.8%')
    expect(container.textContent).toContain('pipelineHealth')
    expect(container.textContent).toContain('66.6%')
    expect(container.textContent).not.toContain('12.3%')
  })
})

