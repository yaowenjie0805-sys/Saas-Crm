import { expect } from '@playwright/test'

export function collectPageErrors(page) {
  const errors = []
  page.on('pageerror', (error) => {
    errors.push(error?.message || '')
  })
  return errors
}

function isKnownBoundaryIssue(text) {
  const content = String(text || '')
  return content.includes('requestFailed') && content.includes('Cannot convert object to primitive value')
}

function toRegex(pattern) {
  return pattern instanceof RegExp ? pattern : new RegExp(String(pattern))
}

export async function expectHealthyPage(page, pageErrors, options = {}) {
  const {
    allowKnownBoundaryIssue = true,
    ignoreErrorMessages = [],
    ignoreErrorPatterns = [],
  } = options

  const boundary = page.getByTestId('error-boundary')
  const boundaryCount = await boundary.count()
  if (boundaryCount > 0) {
    const boundaryText = await boundary.first().textContent().catch(() => '')
    if (!allowKnownBoundaryIssue || !isKnownBoundaryIssue(boundaryText)) {
      await expect(boundary).toHaveCount(0)
    }
  }

  const patterns = ignoreErrorPatterns.map(toRegex)
  const ignoredMessages = new Set(ignoreErrorMessages)
  const unexpectedErrors = pageErrors.filter((message) => (
    !ignoredMessages.has(message) && !patterns.some((pattern) => pattern.test(message))
  ))
  expect(unexpectedErrors, `Unexpected page errors:\n${unexpectedErrors.join('\n')}`).toEqual([])
}
