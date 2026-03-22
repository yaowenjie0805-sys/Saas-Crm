// Test data generators for consistent, unique test data

export function generateUniqueCustomerName() {
  return `E2E Customer ${Date.now()}-${Math.random().toString(36).substring(7)}`
}

export function generateUniqueOwnerName() {
  return `Owner ${Date.now()}-${Math.random().toString(36).substring(7)}`
}

export function generateUniqueQuoteName() {
  return `Quote ${Date.now()}-${Math.random().toString(36).substring(7)}`
}

export function generateUniqueLeadName() {
  return `Lead ${Date.now()}-${Math.random().toString(36).substring(7)}`
}

export function generateUniqueOpportunityName() {
  return `Opportunity ${Date.now()}-${Math.random().toString(36).substring(7)}`
}

export function generateUniqueContractName() {
  return `Contract ${Date.now()}-${Math.random().toString(36).substring(7)}`
}

export function generateUniqueProductName() {
  return `Product ${Date.now()}-${Math.random().toString(36).substring(7)}`
}

export function generateNumericValue(min = 1000, max = 100000) {
  return String(Math.floor(Math.random() * (max - min + 1)) + min)
}

export const TEST_STATUSES = {
  customer: ['Active', 'Prospect', 'Churned', 'Inactive'],
  lead: ['New', 'Contacted', 'Qualified', 'Lost'],
  opportunity: ['Qualification', 'Needs Analysis', 'Proposal', 'Negotiation', 'Closed Won', 'Closed Lost'],
  quote: ['Draft', 'Sent', 'Accepted', 'Rejected', 'Expired'],
  contract: ['Draft', 'Active', 'Expired', 'Terminated'],
}

export const TEST_OWNERS = ['Admin', 'Sales Rep 1', 'Sales Rep 2']

export function getRandomItem(array) {
  return array[Math.floor(Math.random() * array.length)]
}
