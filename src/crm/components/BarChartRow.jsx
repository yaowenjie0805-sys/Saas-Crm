import { formatMoney } from '../shared'

function BarChartRow({ label, value, money = false }) {
  const width = Math.max(6, Math.min(100, Number(value || 0)))
  return (
    <div className="bar-row">
      <span>{label}</span>
      <div className="bar-track"><i style={{ width: `${width}%` }} /></div>
      <b>{money ? formatMoney(value) : value}</b>
    </div>
  )
}

export default BarChartRow