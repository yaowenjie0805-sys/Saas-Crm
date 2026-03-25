import { describe, expect, it } from 'vitest'
import { clampSelectedIndex, flattenSearchResults } from '../components/search/CommandPalette'

describe('CommandPalette helpers', () => {
  it('flattens only array buckets and preserves result type', () => {
    const flat = flattenSearchResults({
      CUSTOMER: [{ id: 'c-1', snippet: 'Customer One' }],
      LEAD: null,
      TASK: [{ id: 't-1', snippet: 'Task One' }],
    })

    expect(flat).toEqual([
      { id: 'c-1', snippet: 'Customer One', type: 'CUSTOMER' },
      { id: 't-1', snippet: 'Task One', type: 'TASK' },
    ])
  })

  it('clamps the selected index into the available result range', () => {
    expect(clampSelectedIndex(-1, 2)).toBe(0)
    expect(clampSelectedIndex(3, 2)).toBe(1)
    expect(clampSelectedIndex(5, 0)).toBe(0)
  })
})
