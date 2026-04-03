import { useState } from 'react'

function ServerPager({ t, page, totalPages, size, onPageChange, onSizeChange, sizeOptions = [5, 8, 12, 20], testId = 'server-pager' }) {
  const [jump, setJump] = useState('')

  const doJump = () => {
    const n = Number(jump || page)
    if (!Number.isFinite(n)) return
    const target = Math.min(Math.max(1, Math.floor(n)), Math.max(1, totalPages || 1))
    onPageChange(target)
    setJump(String(target))
  }

  return (
    <div className="pager" data-testid={testId}>
      <button className="mini-btn" disabled={page <= 1} onClick={() => onPageChange(Math.max(1, page - 1))}>{t('pagePrev')}</button>
      <span>{page}/{totalPages}</span>
      <button className="mini-btn" disabled={page >= totalPages} onClick={() => onPageChange(Math.min(totalPages, page + 1))}>{t('pageNext')}</button>
      <input className="tool-input pager-jump" value={jump} placeholder={String(page)} onChange={(e) => setJump(e.target.value)} />
      <button className="mini-btn" onClick={doJump}>{t('go')}</button>
      <select className="tool-input pager-size" value={size} onChange={(e) => onSizeChange(Number(e.target.value))}>
        {sizeOptions.map((n) => <option key={n} value={n}>{`${t('pageSize')}: ${n}`}</option>)}
      </select>
    </div>
  )
}

export default ServerPager
