import { useMemo } from 'react'

export function useCommerceMapper(params) {
  const {
    commerceDomain,
    refreshContracts,
    refreshPayments,
    contractForm, setContractForm, saveContract, crudErrors, crudFieldErrors, contracts, editContract, removeContract,
    contractQ, setContractQ, contractStatus, setContractStatus, contractPage, contractTotalPages, contractSize,
    onContractPageChange, onContractSizeChange,
    paymentForm, setPaymentForm, savePayment, payments, editPayment, removePayment, paymentStatus, setPaymentStatus,
    paymentPage, paymentTotalPages, paymentSize, onPaymentPageChange, onPaymentSizeChange,
  } = params

  const mainCommerce = useMemo(() => (commerceDomain || {}), [commerceDomain])

  const mainContracts = useMemo(() => ({
    contractForm, setContractForm, saveContract, formError: crudErrors.contract, fieldErrors: crudFieldErrors.contract, contracts, editContract, removeContract, contractQ, setContractQ, contractStatus, setContractStatus, pagination: { page: contractPage, totalPages: contractTotalPages, size: contractSize }, onPageChange: onContractPageChange, onSizeChange: onContractSizeChange, onRefresh: refreshContracts,
  }), [contractForm, setContractForm, saveContract, crudErrors.contract, crudFieldErrors.contract, contracts, editContract, removeContract, contractQ, setContractQ, contractStatus, setContractStatus, contractPage, contractTotalPages, contractSize, onContractPageChange, onContractSizeChange, refreshContracts])

  const mainPayments = useMemo(() => ({
    paymentForm, setPaymentForm, savePayment, formError: crudErrors.payment, fieldErrors: crudFieldErrors.payment, payments, editPayment, removePayment, paymentStatus, setPaymentStatus, pagination: { page: paymentPage, totalPages: paymentTotalPages, size: paymentSize }, onPageChange: onPaymentPageChange, onSizeChange: onPaymentSizeChange, onRefresh: refreshPayments,
  }), [paymentForm, setPaymentForm, savePayment, crudErrors.payment, crudFieldErrors.payment, payments, editPayment, removePayment, paymentStatus, setPaymentStatus, paymentPage, paymentTotalPages, paymentSize, onPaymentPageChange, onPaymentSizeChange, refreshPayments])

  return {
    mainCommerce,
    mainContracts,
    mainPayments,
  }
}
