function QuoteEditorModal({
  t,
  canWrite,
  form,
  setForm,
  setOpenModal,
  saveQuote,
}) {
  return (
    <div className="modal-mask">
      <div className="modal-card">
        <div className="panel-head">
          <h2>{form.id ? t('editQuote') : t('createQuote')}</h2>
          <button className="mini-btn" onClick={() => setOpenModal(false)}>{t('closeModal')}</button>
        </div>
        <div className="forms-grid">
          <input className="tool-input" placeholder={t('customerId')} value={form.customerId} onChange={(e) => setForm((p) => ({ ...p, customerId: e.target.value }))} />
          <input className="tool-input" placeholder={t('opportunityId')} value={form.opportunityId} onChange={(e) => setForm((p) => ({ ...p, opportunityId: e.target.value }))} />
          <input className="tool-input" placeholder={t('owner')} value={form.owner} onChange={(e) => setForm((p) => ({ ...p, owner: e.target.value }))} />
          <input className="tool-input" placeholder={t('validUntil')} value={form.validUntil} onChange={(e) => setForm((p) => ({ ...p, validUntil: e.target.value }))} />
        </div>
        <div className="inline-tools" style={{ marginTop: 12 }}>
          <button className="primary-btn" disabled={!canWrite} onClick={saveQuote}>{t('save')}</button>
        </div>
      </div>
    </div>
  )
}

export default QuoteEditorModal
