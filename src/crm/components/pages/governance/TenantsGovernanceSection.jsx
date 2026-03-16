import { memo, useEffect, useMemo } from 'react'
import { translateStatus } from '../../../shared'
import { MARKET_PROFILE_OPTIONS, TENANT_APPROVAL_MODE_OPTIONS } from '../../../shared'
import ServerPager from '../../ServerPager'
import VirtualListTable from '../../VirtualListTable'
import { useGovernanceTableState } from '../../../hooks/useGovernanceTableState'

const PAGE_SIZES = [5, 8, 12, 20]
const DATE_FORMAT_OPTIONS = ['yyyy-MM-dd', 'dd/MM/yyyy', 'MM-dd-yyyy']

function TenantsGovernanceSection({ t, tenants, onRefresh }) {
  const tableState = useGovernanceTableState({ page: 1, size: 8 })
  const tenantQuery = tableState.query
  const setTenantQuery = tableState.setQuery
  const tenantStatusFilter = tableState.filter
  const setTenantStatusFilter = tableState.setFilter
  const tenantPage = tableState.page
  const setTenantPage = tableState.setPage
  const tenantSize = tableState.size
  const setTenantSize = tableState.setSize

  const tenantsFiltered = useMemo(() => {
    const q = String(tenantQuery || '').trim().toLowerCase()
    return (tenants.rows || []).filter((row) => {
      if (tenantStatusFilter && String(row.status || '') !== tenantStatusFilter) return false
      if (!q) return true
      return String(row.id || '').toLowerCase().includes(q)
        || String(row.name || '').toLowerCase().includes(q)
    })
  }, [tenants.rows, tenantQuery, tenantStatusFilter])

  const tenantTotalPages = Math.max(1, Math.ceil(tenantsFiltered.length / tenantSize))
  const effectiveTenantPage = Math.min(tenantPage, tenantTotalPages)
  const tenantRows = tenantsFiltered.slice((effectiveTenantPage - 1) * tenantSize, effectiveTenantPage * tenantSize)

  useEffect(() => {
    setTenantPage(1)
  }, [tenantQuery, tenantStatusFilter, tenantSize, setTenantPage])

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('tenantsAdmin')}</h2><button className="mini-btn" onClick={onRefresh}>{t('refresh')}</button></div>
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input className="tool-input" placeholder={t('tenantName')} value={tenants.form.name} onChange={(e) => tenants.setForm((p) => ({ ...p, name: e.target.value }))} />
        <input className="tool-input" placeholder={t('tenantQuota')} value={tenants.form.quotaUsers} onChange={(e) => tenants.setForm((p) => ({ ...p, quotaUsers: e.target.value }))} />
        <input className="tool-input" placeholder={t('reportTimezone')} value={tenants.form.timezone} onChange={(e) => tenants.setForm((p) => ({ ...p, timezone: e.target.value }))} />
        <input className="tool-input" placeholder={t('reportCurrency')} value={tenants.form.currency} onChange={(e) => tenants.setForm((p) => ({ ...p, currency: e.target.value }))} />
        <select className="tool-input" value={tenants.form.marketProfile || 'CN'} onChange={(e) => tenants.setForm((p) => ({ ...p, marketProfile: e.target.value }))}>
          {MARKET_PROFILE_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}
        </select>
        <input className="tool-input" placeholder={t('taxRule')} value={tenants.form.taxRule || 'VAT_CN'} onChange={(e) => tenants.setForm((p) => ({ ...p, taxRule: e.target.value }))} />
        <select className="tool-input" value={tenants.form.approvalMode || 'STRICT'} onChange={(e) => tenants.setForm((p) => ({ ...p, approvalMode: e.target.value }))}>
          {TENANT_APPROVAL_MODE_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}
        </select>
        <input className="tool-input" placeholder={t('channelsLabel')} value={tenants.form.channels || '["WECOM","DINGTALK"]'} onChange={(e) => tenants.setForm((p) => ({ ...p, channels: e.target.value }))} />
        <input className="tool-input" placeholder={t('dataResidency')} value={tenants.form.dataResidency || 'CN'} onChange={(e) => tenants.setForm((p) => ({ ...p, dataResidency: e.target.value }))} />
        <input className="tool-input" placeholder={t('maskLevel')} value={tenants.form.maskLevel || 'STANDARD'} onChange={(e) => tenants.setForm((p) => ({ ...p, maskLevel: e.target.value }))} />
        <select className="tool-input" value={tenants.form.dateFormat || 'yyyy-MM-dd'} onChange={(e) => tenants.setForm((p) => ({ ...p, dateFormat: e.target.value }))}>
          {DATE_FORMAT_OPTIONS.map((fmt) => <option key={fmt} value={fmt}>{t(`dateFormatOption_${fmt.replace(/\//g, '_').replace(/-/g, '_')}`)}</option>)}
        </select>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 8 }}>
        <button className="mini-btn" onClick={tenants.createTenant}>{t('create')}</button>
        <input className="tool-input" placeholder={t('search')} value={tenantQuery} onChange={(e) => setTenantQuery(e.target.value)} />
        <select className="tool-input" value={tenantStatusFilter} onChange={(e) => setTenantStatusFilter(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          <option value="ACTIVE">{translateStatus(t, 'ACTIVE')}</option>
          <option value="INACTIVE">{translateStatus(t, 'INACTIVE')}</option>
        </select>
        <button className="mini-btn" onClick={tableState.reset}>{t('reset')}</button>
      </div>
      {tenants.lastCreated && <div className="info-banner" style={{ marginBottom: 10 }}>{t('tenantCreated')}: {tenants.lastCreated.id}</div>}
      <div className="table-row table-head-row table-row-5 compact">
        <span>{t('idLabel')}</span>
        <span>{t('tenantName')}</span>
        <span>{t('status')}</span>
        <span>{t('tenantQuota')}</span>
        <span>{t('tenantConfig')}</span>
      </div>
      <VirtualListTable
        rows={tenantRows}
        rowHeight={78}
        viewportHeight={Math.min(480, Math.max(170, tenantRows.length * 78))}
        getRowKey={(row) => row.id}
        renderRow={(row) => (
          <div key={row.id} className="table-row table-row-5 compact">
            <span>{row.id}</span>
            <span><input className="tool-input" value={row.name || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, name: e.target.value } : x))} /></span>
            <span>
              <select className="tool-input" value={row.status || 'ACTIVE'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, status: e.target.value } : x))}>
                <option value="ACTIVE">{translateStatus(t, 'ACTIVE')}</option>
                <option value="INACTIVE">{translateStatus(t, 'INACTIVE')}</option>
              </select>
            </span>
            <span><input className="tool-input" value={row.quotaUsers || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, quotaUsers: e.target.value } : x))} /></span>
            <span>
              <div className="inline-tools">
                <input className="tool-input" placeholder={t('reportTimezone')} value={row.timezone || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, timezone: e.target.value } : x))} />
                <input className="tool-input" placeholder={t('reportCurrency')} value={row.currency || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, currency: e.target.value.toUpperCase() } : x))} />
                <select className="tool-input" value={row.marketProfile || 'CN'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, marketProfile: e.target.value } : x))}>
                  {MARKET_PROFILE_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}
                </select>
                <input className="tool-input" placeholder={t('taxRule')} value={row.taxRule || 'VAT_CN'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, taxRule: e.target.value } : x))} />
                <select className="tool-input" value={row.approvalMode || 'STRICT'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, approvalMode: e.target.value } : x))}>
                  {TENANT_APPROVAL_MODE_OPTIONS.map((x) => <option key={x} value={x}>{x}</option>)}
                </select>
                <input className="tool-input" placeholder={t('channelsLabel')} value={row.channels || '[\"WECOM\",\"DINGTALK\"]'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, channels: e.target.value } : x))} />
                <input className="tool-input" placeholder={t('dataResidency')} value={row.dataResidency || 'CN'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, dataResidency: e.target.value } : x))} />
                <input className="tool-input" placeholder={t('maskLevel')} value={row.maskLevel || 'STANDARD'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, maskLevel: e.target.value } : x))} />
                <select className="tool-input" value={row.dateFormat || 'yyyy-MM-dd'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, dateFormat: e.target.value } : x))}>
                  {DATE_FORMAT_OPTIONS.map((fmt) => <option key={fmt} value={fmt}>{t(`dateFormatOption_${fmt.replace(/\//g, '_').replace(/-/g, '_')}`)}</option>)}
                </select>
                <button className="mini-btn" onClick={() => tenants.updateTenant(row)}>{t('save')}</button>
              </div>
            </span>
          </div>
        )}
      />
      {tenantsFiltered.length === 0 && <div className="empty-tip">{t('noData')}</div>}
      {tenantsFiltered.length > 0 && <ServerPager t={t} page={effectiveTenantPage} totalPages={tenantTotalPages} size={tenantSize} onPageChange={setTenantPage} onSizeChange={setTenantSize} sizeOptions={PAGE_SIZES} />}
    </section>
  )
}

export default memo(TenantsGovernanceSection)
