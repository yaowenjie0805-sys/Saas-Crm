import { memo } from 'react'
import { translateStatus } from '../../../shared'
import { DATE_FORMAT_OPTIONS, applyMarketDefaults } from './tenantsSectionShared'

function TenantRowEditor({
  row,
  t,
  tenants,
  MARKET_PROFILE_OPTIONS,
  TENANT_APPROVAL_MODE_OPTIONS,
}) {
  return (
    <div className="table-row table-row-5 compact tenant-row">
      <span>{row.id}</span>
      <span><input className="tool-input" value={row.name || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, name: e.target.value } : x))} /></span>
      <span>
        <select className="tool-input" value={row.status || 'ACTIVE'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, status: e.target.value } : x))}>
          <option value="ACTIVE">{translateStatus(t, 'ACTIVE')}</option>
          <option value="INACTIVE">{translateStatus(t, 'INACTIVE')}</option>
        </select>
      </span>
      <span><input className="tool-input" value={row.quotaUsers || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, quotaUsers: e.target.value } : x))} /></span>
      <span className="tenant-config-cell">
        <div className="tenant-config-grid">
          <input className="tool-input" placeholder={t('reportTimezone')} value={row.timezone || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, timezone: e.target.value } : x))} />
          <input className="tool-input" placeholder={t('reportCurrency')} value={row.currency || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, currency: e.target.value.toUpperCase() } : x))} />
          <select className="tool-input" value={row.marketProfile || 'CN'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? applyMarketDefaults(x, e.target.value) : x))}>
            {MARKET_PROFILE_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}
          </select>
          <input className="tool-input" placeholder={t('taxRule')} value={row.taxRule || 'VAT_CN'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, taxRule: e.target.value } : x))} />
          <select className="tool-input" value={row.approvalMode || 'STRICT'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, approvalMode: e.target.value } : x))}>
            {TENANT_APPROVAL_MODE_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}
          </select>
          <input className="tool-input" placeholder={t('channelsLabel')} value={row.channels || '["WECOM","DINGTALK"]'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, channels: e.target.value } : x))} />
          <input className="tool-input" placeholder={t('dataResidency')} value={row.dataResidency || 'CN'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, dataResidency: e.target.value } : x))} />
          <input className="tool-input" placeholder={t('maskLevel')} value={row.maskLevel || 'STANDARD'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, maskLevel: e.target.value } : x))} />
          <select className="tool-input" value={row.dateFormat || 'yyyy-MM-dd'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, dateFormat: e.target.value } : x))}>
            {DATE_FORMAT_OPTIONS.map((fmt) => <option key={fmt} value={fmt}>{t(`dateFormatOption_${fmt.replace(/\//g, '_').replace(/-/g, '_')}`)}</option>)}
          </select>
          <button className="mini-btn tenant-save-btn" onClick={() => tenants.updateTenant(row)}>{t('save')}</button>
        </div>
      </span>
    </div>
  )
}

export default memo(TenantRowEditor)
