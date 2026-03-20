import { memo } from 'react'
import { useWindowedRows } from '../hooks/useWindowedRows'

function VirtualListTable({ rows, rowHeight = 48, viewportHeight = 460, overscan = 8, renderRow, getRowKey }) {
  const { windowedRows, topSpacerHeight, bottomSpacerHeight, onScroll, viewportStyle } = useWindowedRows(rows || [], { rowHeight, viewportHeight, overscan })
  return (
    <div style={viewportStyle} onScroll={onScroll}>
      {topSpacerHeight > 0 && <div style={{ height: topSpacerHeight }} />}
      {(windowedRows || []).map((row, index) => renderRow(row, index, getRowKey ? getRowKey(row, index) : row?.id))}
      {bottomSpacerHeight > 0 && <div style={{ height: bottomSpacerHeight }} />}
    </div>
  )
}

export default memo(VirtualListTable)
