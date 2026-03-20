function ListState({ loading, empty, emptyText = 'No data', rows = 4 }) {
  if (loading) {
    return (
      <div className="list-skeleton" aria-hidden="true">
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="table-row skeleton-row">
            <span></span><span></span><span></span><span></span><span></span>
          </div>
        ))}
      </div>
    )
  }

  if (empty) {
    return <div className="empty-tip">{emptyText}</div>
  }

  return null
}

export default ListState
