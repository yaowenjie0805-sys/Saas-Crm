export default function ApprovalStatsCards({ t, stats }) {
  return (
    <section className="stats-grid" style={{ marginBottom: 12 }}>
      <article className="stat-card"><p>{t('approvalTemplatesCount')}</p><h3>{stats?.summary?.templates || 0}</h3><span>{t('approvalTemplatesCount')}</span></article>
      <article className="stat-card"><p>{t('approvalInstancesCount')}</p><h3>{stats?.summary?.instances || 0}</h3><span>{t('approvalInstancesCount')}</span></article>
      <article className="stat-card"><p>{t('approvalTasksCount')}</p><h3>{stats?.summary?.tasks || 0}</h3><span>{t('approvalTasksCount')}</span></article>
      <article className="stat-card"><p>{t('approvalPendingCount')}</p><h3>{stats?.summary?.pendingTasks || 0}</h3><span>{t('approvalPendingCount')}</span></article>
      <article className="stat-card"><p>{t('approvalSlaOverdue')}</p><h3>{stats?.sla?.overdueCount || 0}</h3><span>{t('approvalOverdue')}</span></article>
      <article className="stat-card"><p>{t('approvalSlaEscalated')}</p><h3>{stats?.sla?.escalatedCount || 0}</h3><span>{t('approvalEscalated')}</span></article>
    </section>
  )
}
