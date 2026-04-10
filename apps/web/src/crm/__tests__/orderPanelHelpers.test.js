import { describe, expect, it } from 'vitest'
import { formatOrderActionError } from '../components/pages/orders/orderPanelHelpers'

describe('orderPanelHelpers.formatOrderActionError', () => {
  const t = (key) => key

  it('maps new stage-gate quote accepted code to localized message', () => {
    const message = formatOrderActionError(
      {
        code: 'order_stage_gate_quote_accepted_required',
        details: { requiredStatus: 'ACCEPTED' },
      },
      t,
    )

    expect(message).toContain('orderStageGateQuoteAcceptedRequired')
    expect(message).toContain('requiredStatusLabel')
    expect(message).toContain('ACCEPTED')
  })
})

