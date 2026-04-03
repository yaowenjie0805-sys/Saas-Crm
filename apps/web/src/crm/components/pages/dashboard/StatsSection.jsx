import { memo } from 'react'

/**
 * 统计卡片部分
 */
function StatsSection({ stats }) {
  if (!stats || stats.length === 0) return null

  return (
    <section className="stats-grid" data-testid="dashboard-stats">
      {stats.map((s) => (
        <article key={s.label} className="stat-card" data-testid="dashboard-stat-card">
          <p data-testid="dashboard-stat-label">{s.label}</p>
          <h3 data-testid="dashboard-stat-value">{s.value}</h3>
          <span>{s.trend}</span>
        </article>
      ))}
    </section>
  )
}

export default memo(StatsSection)
