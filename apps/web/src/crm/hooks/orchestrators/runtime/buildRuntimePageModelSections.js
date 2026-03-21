function buildRuntimePageModelCustomerSection({ toggleTaskDone, crudDomainActions, corePaginationActions }) {
  return {
    toggleTaskDone,
    ...crudDomainActions,
    ...corePaginationActions,
  }
}

function buildRuntimePageModelGovernanceSection(domainActions) {
  return {
    ...domainActions,
  }
}

export {
  buildRuntimePageModelCustomerSection,
  buildRuntimePageModelGovernanceSection,
}
