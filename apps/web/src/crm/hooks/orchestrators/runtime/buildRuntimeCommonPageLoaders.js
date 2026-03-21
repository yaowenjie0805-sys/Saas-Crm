export function buildRuntimeCommonPageLoaders(args) {
  const {
    pageSignature,
    leadQ,
    leadStatus,
    leadImportStatusFilter,
    leadImportPage,
    leadImportSize,
    leadImportExportStatusFilter,
    leadImportExportPage,
    leadImportExportSize,
    leadImportJobId,
    leadPage,
    leadSize,
    loadLeads,
    loadLeadImportJobs,
    loadLeadImportFailedRows,
    loadLeadImportExportJobs,
    loadLeadImportMetrics,
    canViewOpsMetrics,
    customerQ,
    customerStatus,
    customerPage,
    customerSize,
    loadCustomers,
    oppStage,
    opportunityPage,
    opportunitySize,
    loadOpportunities,
    contactQ,
    contactPage,
    contactSize,
    loadContacts,
    contractQ,
    contractStatus,
    contractPage,
    contractSize,
    loadContracts,
    paymentStatus,
    paymentPage,
    paymentSize,
    loadPayments,
    commerceDomain,
    quoteOpportunityFilter,
    orderOpportunityFilter,
    followCustomerId,
    followQ,
    loadFollowUps,
    loadTasks,
  } = args

  return {
    leads: {
      signature: pageSignature(
        'leads',
        {
          q: leadQ,
          status: leadStatus,
          importStatus: leadImportStatusFilter,
          importPage: leadImportPage,
          importSize: leadImportSize,
          importExportStatus: leadImportExportStatusFilter,
          importExportPage: leadImportExportPage,
          importExportSize: leadImportExportSize,
          importJobId: leadImportJobId || '',
        },
        leadPage,
        leadSize,
      ),
      delay: 220,
      run: async (controller) => {
        const options = { signal: controller?.signal }
        const tasks = [
          loadLeads(leadPage, leadSize, options),
          loadLeadImportJobs(leadImportPage, leadImportSize, options),
        ]
        if (leadImportJobId) {
          tasks.push(loadLeadImportFailedRows(leadImportJobId, options))
          tasks.push(loadLeadImportExportJobs(leadImportJobId, leadImportExportPage, leadImportExportSize, options))
        }
        if (canViewOpsMetrics) tasks.push(loadLeadImportMetrics(options))
        await Promise.all(tasks)
      },
    },
    customers: {
      signature: pageSignature('customers', { q: customerQ, status: customerStatus }, customerPage, customerSize),
      delay: 220,
      run: (controller) => loadCustomers(customerPage, customerSize, { signal: controller?.signal }),
    },
    pipeline: {
      signature: pageSignature('pipeline', { stage: oppStage }, opportunityPage, opportunitySize),
      delay: 220,
      run: () => loadOpportunities(opportunityPage, opportunitySize),
    },
    contacts: {
      signature: pageSignature('contacts', { q: contactQ }, contactPage, contactSize),
      delay: 220,
      run: () => loadContacts(contactPage, contactSize),
    },
    contracts: {
      signature: pageSignature('contracts', { q: contractQ, status: contractStatus }, contractPage, contractSize),
      delay: 220,
      run: () => loadContracts(contractPage, contractSize),
    },
    payments: {
      signature: pageSignature('payments', { status: paymentStatus }, paymentPage, paymentSize),
      delay: 220,
      run: () => loadPayments(paymentPage, paymentSize),
    },
    priceBooks: {
      signature: pageSignature(
        'priceBooks',
        {
          status: commerceDomain.signatures.priceBooks.filters.status,
          name: commerceDomain.signatures.priceBooks.filters.name,
        },
        commerceDomain.signatures.priceBooks.pageNo,
        commerceDomain.signatures.priceBooks.pageSize,
      ),
      delay: 180,
      run: (controller) => commerceDomain.loaders.loadPriceBooks({ signal: controller?.signal }),
    },
    products: {
      signature: pageSignature(
        'products',
        {
          status: commerceDomain.signatures.products.filters.status,
          code: commerceDomain.signatures.products.filters.code,
          name: commerceDomain.signatures.products.filters.name,
          category: commerceDomain.signatures.products.filters.category,
        },
        commerceDomain.signatures.products.pageNo,
        commerceDomain.signatures.products.pageSize,
      ),
      delay: 180,
      run: (controller) => commerceDomain.loaders.loadProducts({ signal: controller?.signal }),
    },
    quotes: {
      signature: pageSignature(
        'quotes',
        {
          status: commerceDomain.signatures.quotes.filters.status,
          owner: commerceDomain.signatures.quotes.filters.owner,
          opportunityId: commerceDomain.signatures.quotes.filters.opportunityId || quoteOpportunityFilter,
        },
        commerceDomain.signatures.quotes.pageNo,
        commerceDomain.signatures.quotes.pageSize,
      ),
      delay: 180,
      run: (controller) => commerceDomain.loaders.loadQuotes({ signal: controller?.signal }),
    },
    orders: {
      signature: pageSignature(
        'orders',
        {
          status: commerceDomain.signatures.orders.filters.status,
          owner: commerceDomain.signatures.orders.filters.owner,
          opportunityId: commerceDomain.signatures.orders.filters.opportunityId || orderOpportunityFilter,
        },
        commerceDomain.signatures.orders.pageNo,
        commerceDomain.signatures.orders.pageSize,
      ),
      delay: 180,
      run: (controller) => commerceDomain.loaders.loadOrders({ signal: controller?.signal }),
    },
    followUps: {
      signature: pageSignature('followUps', { customerId: followCustomerId, q: followQ }, 1, 0),
      delay: 250,
      run: () => loadFollowUps(),
    },
    tasks: {
      signature: pageSignature('tasks', {}, 1, 0),
      delay: 0,
      run: () => loadTasks(),
    },
  }
}
