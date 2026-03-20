import { lazy, Suspense } from 'react'
import { translateDataset, translateOwnerAlias, translateVisibility } from '../../shared'

const ReportDesignerPreviewSection = lazy(() => import('./reportDesigner/ReportDesignerPreviewSection'))

function ReportDesignerPanel({
  activePage,
  t,
  canDesign,
  templateForm,
  setTemplateForm,
  createTemplate,
  templates,
  updateTemplate,
  runTemplate,
  runResult,
  loadTemplates,
}) {
  if (activePage !== 'reportDesigner') return null

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('reportDesigner')}</h2>
        <button className="mini-btn" onClick={loadTemplates}>{t('refresh')}</button>
      </div>

      {canDesign && (
        <div className="inline-tools">
          <input className="tool-input" placeholder={t('title')} value={templateForm.name} onChange={(e) => setTemplateForm((p) => ({ ...p, name: e.target.value }))} />
          <select className="tool-input" value={templateForm.dataset} onChange={(e) => setTemplateForm((p) => ({ ...p, dataset: e.target.value }))}>
            <option value="CUSTOMERS">{t('customers')}</option>
            <option value="OPPORTUNITIES">{t('pipeline')}</option>
            <option value="CONTRACTS">{t('contracts')}</option>
            <option value="PAYMENTS">{t('payments')}</option>
            <option value="LEADS">{t('leads')}</option>
          </select>
          <select className="tool-input" value={templateForm.visibility} onChange={(e) => setTemplateForm((p) => ({ ...p, visibility: e.target.value }))}>
            <option value="PRIVATE">{t('reportVisibilityPrivate')}</option>
            <option value="DEPARTMENT">{t('reportVisibilityDepartment')}</option>
            <option value="TENANT">{t('reportVisibilityTenant')}</option>
          </select>
          <input className="tool-input" placeholder={t('reportLimit')} value={templateForm.limit} onChange={(e) => setTemplateForm((p) => ({ ...p, limit: e.target.value }))} />
          <button className="mini-btn" onClick={createTemplate}>{t('create')}</button>
        </div>
      )}

      <div className="table-row table-head-row" style={{ marginTop: 12 }}>
        <span>{t('title')}</span><span>{t('dataset')}</span><span>{t('status')}</span><span>{t('owner')}</span><span>{t('action')}</span>
      </div>
      {(templates || []).map((row) => (
        <div key={row.id} className="table-row">
          <span>{row.name}</span>
          <span>{translateDataset(t, row.dataset)}</span>
          <span>{translateVisibility(t, row.visibility)}</span>
          <span>{translateOwnerAlias(t, row.owner)}</span>
          <span>
            <div className="inline-tools">
              <button className="mini-btn" onClick={() => runTemplate(row.id)}>{t('run')}</button>
              {canDesign && <button className="mini-btn" onClick={() => updateTemplate(row)}>{t('save')}</button>}
            </div>
          </span>
        </div>
      ))}
      {(templates || []).length === 0 && <div className="empty-tip">{t('noData')}</div>}

      <Suspense fallback={<div className="loading">{t('loading')}</div>}>
        <ReportDesignerPreviewSection t={t} runResult={runResult} />
      </Suspense>
    </section>
  )
}

export default ReportDesignerPanel
