import { CUSTOMER_STATUS_OPTIONS, translateStatus } from '../../../../shared'

export default function CustomerFormBar({
  t,
  canWrite,
  customerForm,
  setCustomerForm,
  saveCustomer,
  fieldErrors,
  formError,
}) {
  return (
    <>
      <div className="inline-tools filter-row" style={{ marginBottom: 10 }}>
        <input data-testid="customer-form-name" className={fieldErrors?.name ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('companyName')} value={customerForm.name} onChange={(e) => setCustomerForm((p) => ({ ...p, name: e.target.value }))} />
        <input data-testid="customer-form-owner" className={fieldErrors?.owner ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('owner')} value={customerForm.owner} onChange={(e) => setCustomerForm((p) => ({ ...p, owner: e.target.value }))} />
        <select data-testid="customer-form-status" className={fieldErrors?.status ? 'tool-input input-invalid' : 'tool-input'} value={customerForm.status} onChange={(e) => setCustomerForm((p) => ({ ...p, status: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <input data-testid="customer-form-value" className={fieldErrors?.value ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('amount')} value={customerForm.value} onChange={(e) => setCustomerForm((p) => ({ ...p, value: e.target.value }))} />
        <button className="mini-btn" data-testid="customer-form-submit" disabled={!canWrite} onClick={saveCustomer}>{customerForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" data-testid="customer-form-reset" onClick={() => setCustomerForm({ id: '', name: '', owner: '', status: '', tag: '', value: '' })}>{t('reset')}</button>
      </div>
      {(fieldErrors?.name || fieldErrors?.owner || fieldErrors?.status || fieldErrors?.value) && <div className="field-error" style={{ marginBottom: 8 }}>{fieldErrors?.name || fieldErrors?.owner || fieldErrors?.status || fieldErrors?.value}</div>}
      {formError && <div className="field-error" style={{ marginBottom: 8 }}>{formError}</div>}
    </>
  )
}
