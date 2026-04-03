function OrderEditorModal({
  t,
  canWrite,
  form,
  setForm,
  setOpenModal,
  save,
}) {
  return (
    <div className="modal-mask">
      <div className="modal-card modal-card-commerce">
        <div className="panel-head">
          <h2>{form.id ? t('editOrder') : t('createOrder')}</h2>
          <button className="mini-btn" onClick={() => setOpenModal(false)}>{t('closeModal')}</button>
        </div>
        <div className="forms-grid">
          <input className="tool-input" placeholder={t('customerId')} value={form.customerId} onChange={(e) => setForm((p) => ({ ...p, customerId: e.target.value }))} />
          <input className="tool-input" placeholder={t('opportunityId')} value={form.opportunityId} onChange={(e) => setForm((p) => ({ ...p, opportunityId: e.target.value }))} />
          <input className="tool-input" placeholder={t('quoteId')} value={form.quoteId} onChange={(e) => setForm((p) => ({ ...p, quoteId: e.target.value }))} />
          <input className="tool-input" placeholder={t('owner')} value={form.owner} onChange={(e) => setForm((p) => ({ ...p, owner: e.target.value }))} />
          <input className="tool-input" placeholder={t('amount')} value={form.amount} onChange={(e) => setForm((p) => ({ ...p, amount: e.target.value }))} />
          <input className="tool-input" placeholder={t('signDate')} value={form.signDate} onChange={(e) => setForm((p) => ({ ...p, signDate: e.target.value }))} />
        </div>
        <div className="inline-tools" style={{ marginTop: 12 }}>
          <button className="primary-btn" disabled={!canWrite} onClick={save}>{t('save')}</button>
        </div>
      </div>
    </div>
  )
}

export default OrderEditorModal
