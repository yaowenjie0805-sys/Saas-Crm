import { useState, useEffect, useRef } from 'react';
import { CUSTOMER_STATUS_OPTIONS, translateStatus } from '../../../../shared'

export default function CustomerFilterBar({
  t,
  customerQDraft,
  setCustomerQDraft,
  customerStatusDraft,
  setCustomerStatusDraft,
  applyFilters,
  resetFilters,
  onRefresh,
}) {
  const [localQ, setLocalQ] = useState(customerQDraft);
  const timerRef = useRef(null);

  // 同步外部值变化
  useEffect(() => {
    setLocalQ(customerQDraft);
  }, [customerQDraft]);

  // 搜索防抖 - 300ms后触发搜索
  const handleSearchChange = (value) => {
    setLocalQ(value);
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    timerRef.current = setTimeout(() => {
      setCustomerQDraft(value);
    }, 300);
  };

  // 清理定时器
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, []);

  return (
    <>
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input 
          data-testid="customers-search-input" 
          className="tool-input" 
          placeholder={t('search')} 
          value={localQ} 
          onChange={(e) => handleSearchChange(e.target.value)} 
          onKeyDown={(e) => { if (e.key === 'Enter') { if (timerRef.current) clearTimeout(timerRef.current); setCustomerQDraft(localQ); applyFilters(); } }} 
        />
        <select data-testid="customers-search-status" className="tool-input" value={customerStatusDraft} onChange={(e) => setCustomerStatusDraft(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') applyFilters() }}>
          <option value="">{t('allStatuses')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <button className="mini-btn" data-testid="customers-search-submit" onClick={() => { if (timerRef.current) clearTimeout(timerRef.current); setCustomerQDraft(localQ); applyFilters(); }}>{t('search')}</button>
        <button className="mini-btn" data-testid="customers-search-reset" onClick={resetFilters}>{t('reset')}</button>
        <button className="mini-btn" data-testid="customers-refresh" onClick={() => onRefresh && onRefresh()}>{t('refresh')}</button>
      </div>
    </>
  )
}
