import { memo } from 'react'

/**
 * 统计卡片部分
 */
function StatsSection({ stats }) {
  if (!stats || stats.length === 0) return null

  return (
    <section className="stats-grid">
      {stats.map((s) => (
        <article key={s.label} className="stat-card">
          <p>{s.label}</p>
          <h3>{s.value}</h3>
          <span>{s.trend}</span>
        </article>
      ))}
    </section>
  )
}

export default memo(StatsSection)
