import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

export function useCommerceRuntimeState({ persisted, readPageSize }) {
  const defaults = useMemo(() => ({
    contracts: [],
    payments: [],
    quoteOpportunityFilter: '',
    orderOpportunityFilter: '',
    quotePrefill: null,
    contractForm: { id: '', customerId: '', contractNo: '', title: '', amount: '', status: '', signDate: '' },
    paymentForm: { id: '', contractId: '', amount: '', receivedDate: '', method: '', status: '', remark: '' },
    contractQ: persisted.contractQ || '',
    contractStatus: persisted.contractStatus || '',
    contractPage: 1,
    contractTotalPages: 1,
    contractSize: () => readPageSize('crm_page_size_contracts'),
    paymentStatus: persisted.paymentStatus || '',
    paymentPage: 1,
    paymentTotalPages: 1,
    paymentSize: () => readPageSize('crm_page_size_payments'),
  }), [persisted, readPageSize])

  return useRuntimeSectionFields('commerceDomain', 'ui', defaults)
}
