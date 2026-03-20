import { translateDataset } from '../../../shared'

function ReportDesignerPreviewSection({ t, runResult }) {
  if (!runResult) return null
  return (
    <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
      <div className="panel-head"><h2>{t('preview')}</h2></div>
      <div className="info-banner">
        {t('dataset')}: {translateDataset(t, runResult.dataset)} | {t('count')}: {runResult.count} | {t('version')}: {runResult.templateVersion || '-'}
      </div>
      <div className="table-row table-head-row" style={{ marginTop: 10 }}>
        {(runResult.fields || []).slice(0, 6).map((f) => <span key={f}>{f}</span>)}
      </div>
      {(runResult.rows || []).slice(0, 8).map((r, idx) => (
        <div key={`designer-row-${idx}`} className="table-row">
          {(runResult.fields || []).slice(0, 6).map((f) => <span key={`${idx}-${f}`}>{String(r?.[f] ?? '-')}</span>)}
        </div>
      ))}
    </div>
  )
}

export default ReportDesignerPreviewSection
