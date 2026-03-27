import { useRef } from 'react'
import ReactECharts from 'echarts-for-react'

/**
 * 图表渲染组件
 * 支持柱状图、折线图、饼图、漏斗图等多种图表类型
 */
export function ChartRenderer({
  type = 'BAR',
  data = {},
  height = 300,
  loading = false,
  onRefresh,
  showLegend = true,
  showTooltip = true,
  title,
  subtitle,
}) {
  const chartRef = useRef(null)

  // 获取图表配置
  const getChartOption = () => {
    const labels = data.labels || []
    const datasets = data.datasets || []

    const baseOption = {
      title: title ? {
        text: title,
        subtext: subtitle,
        left: 'center',
        textStyle: {
          fontSize: 16,
          fontWeight: 'normal',
        },
        subtextStyle: {
          fontSize: 12,
          color: '#666',
        },
      } : undefined,
      tooltip: showTooltip ? {
        trigger: type === 'PIE' || type === 'DOUGHNUT' ? 'item' : 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e0e0e0',
        borderWidth: 1,
        textStyle: {
          color: '#333',
        },
        formatter: (params) => {
          if (type === 'PIE' || type === 'DOUGHNUT') {
            const total = params.data.value
            const percent = params.percent
            return `${params.name}: ${total.toLocaleString()} (${percent}%)`
          }
          return `${params.name}: ${params.value.toLocaleString()}`
        },
      } : undefined,
      legend: showLegend ? {
        bottom: 10,
        left: 'center',
        type: 'scroll',
      } : undefined,
      grid: {
        left: '3%',
        right: '4%',
        bottom: showLegend ? '15%' : '3%',
        top: title ? '20%' : '3%',
        containLabel: true,
      },
    }

    switch (type) {
      case 'BAR':
        return getBarOption(labels, datasets, baseOption)
      case 'LINE':
        return getLineOption(labels, datasets, baseOption)
      case 'PIE':
        return getPieOption(labels, datasets, baseOption)
      case 'DOUGHNUT':
        return getDoughnutOption(labels, datasets, baseOption)
      case 'FUNNEL':
        return getFunnelOption(labels, data, baseOption)
      default:
        return getBarOption(labels, datasets, baseOption)
    }
  }

  // 柱状图配置
  const getBarOption = (labels, datasets, base) => ({
    ...base,
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: {
        rotate: labels.length > 5 ? 30 : 0,
        fontSize: 12,
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value) => {
          if (value >= 100000000) return (value / 100000000).toFixed(1) + '亿'
          if (value >= 10000) return (value / 10000).toFixed(1) + '万'
          return value
        },
      },
    },
    series: datasets.map((ds, index) => ({
      name: ds.label || `数据${index + 1}`,
      type: 'bar',
      data: ds.data || [],
      itemStyle: {
        color: ds.backgroundColor || getDefaultColor(index),
        borderRadius: [4, 4, 0, 0],
      },
      barWidth: '60%',
      label: {
        show: true,
        position: 'top',
        formatter: (params) => {
          const value = params.value
          if (value >= 100000000) return (value / 100000000).toFixed(1) + '亿'
          if (value >= 10000) return (value / 10000).toFixed(1) + '万'
          return value
        },
        fontSize: 10,
        color: '#666',
      },
    })),
  })

  // 折线图配置
  const getLineOption = (labels, datasets, base) => ({
    ...base,
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisLabel: {
        rotate: labels.length > 5 ? 30 : 0,
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value) => {
          if (value >= 100000000) return (value / 100000000).toFixed(1) + '亿'
          if (value >= 10000) return (value / 10000).toFixed(1) + '万'
          return value
        },
      },
    },
    series: datasets.map((ds, index) => ({
      name: ds.label || `数据${index + 1}`,
      type: 'line',
      data: ds.data || [],
      smooth: true,
      itemStyle: {
        color: ds.borderColor || getDefaultColor(index),
      },
      lineStyle: {
        width: 2,
      },
      areaStyle: ds.fill ? {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: (ds.backgroundColor || getDefaultColor(index)) + '40' },
            { offset: 1, color: (ds.backgroundColor || getDefaultColor(index)) + '05' },
          ],
        },
      } : undefined,
      label: {
        show: true,
        position: 'top',
        formatter: (params) => {
          const value = params.value
          if (value >= 100000000) return (value / 100000000).toFixed(1) + '亿'
          if (value >= 10000) return (value / 10000).toFixed(1) + '万'
          return value
        },
        fontSize: 10,
      },
    })),
  })

  // 饼图配置
  const getPieOption = (labels, datasets, base) => {
    const dataArray = (datasets[0]?.data || []).map((value, index) => ({
      name: labels[index] || `项目${index + 1}`,
      value,
    }))

    return {
      ...base,
      series: [{
        name: '数据',
        type: 'pie',
        radius: '60%',
        center: ['50%', '50%'],
        data: dataArray,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
        label: {
          show: true,
          formatter: '{b}: {c} ({d}%)',
        },
        labelLine: {
          show: true,
        },
      }],
    }
  }

  // 环形图配置
  const getDoughnutOption = (labels, datasets, base) => {
    const dataArray = (datasets[0]?.data || []).map((value, index) => ({
      name: labels[index] || `项目${index + 1}`,
      value,
    }))

    return {
      ...base,
      series: [{
        name: '数据',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: dataArray,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
        label: {
          show: true,
          formatter: '{b}: {c}\n{d}%',
        },
      }],
    }
  }

  // 漏斗图配置（国内特色）
  const getFunnelOption = (labels, data, base) => {
    const values = data.values || []
    const rates = data.rates || []

    const funnelData = labels.map((label, index) => ({
      name: `${label} (${rates[index] || 100}%)`,
      value: values[index] || 0,
    }))

    return {
      ...base,
      series: [{
        name: '销售漏斗',
        type: 'funnel',
        left: '10%',
        top: 60,
        bottom: 60,
        width: '80%',
        min: 0,
        max: funnelData[0]?.value || 100,
        minSize: '0%',
        maxSize: '100%',
        gap: 2,
        label: {
          show: true,
          position: 'inside',
          formatter: (params) => {
            return `${params.name}\n${params.value.toLocaleString()}`
          },
        },
        labelLine: {
          show: false,
        },
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 1,
        },
        emphasis: {
          label: {
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data: funnelData,
        color: ['#5470C6', '#91CC75', '#FAC858', '#EE6666'],
      }],
      graphic: rates.length > 1 ? [{
        type: 'text',
        left: '75%',
        top: 'middle',
        style: {
          text: '转化率',
          fill: '#666',
          fontSize: 12,
        },
      }] : undefined,
    }
  }

  // 默认颜色
  const getDefaultColor = (index) => {
    const colors = ['#5470C6', '#91CC75', '#FAC858', '#EE6666', '#73C0DE', '#3BA272', '#FC8452', '#9A60B4']
    return colors[index % colors.length]
  }

  // 刷新图表
  const handleRefresh = () => {
    if (chartRef.current) {
      chartRef.current.getEchartsInstance().resize()
    }
    if (onRefresh) {
      onRefresh()
    }
  }

  return (
    <div className="relative">
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-white/80 z-10">
          <div className="flex flex-col items-center gap-2">
            <div className="animate-spin w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full" />
            <span className="text-sm text-gray-500">加载中...</span>
          </div>
        </div>
      )}

      <ReactECharts
        ref={chartRef}
        option={getChartOption()}
        style={{ height: `${height}px`, width: '100%' }}
        opts={{ renderer: 'canvas' }}
        notMerge={true}
      />

      {onRefresh && (
        <button
          onClick={handleRefresh}
          className="absolute top-2 right-2 p-1.5 bg-white rounded-full shadow-md hover:bg-gray-50 transition-colors"
          title="刷新图表"
        >
          <svg className="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
        </button>
      )}
    </div>
  )
}

/**
 * 图表卡片组件
 */
export function ChartCard({ title, subtitle, children, actions }) {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
      <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div>
          <h3 className="text-sm font-medium text-gray-900">{title}</h3>
          {subtitle && <p className="text-xs text-gray-500 mt-0.5">{subtitle}</p>}
        </div>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>
      <div className="p-4">
        {children}
      </div>
    </div>
  )
}

/**
 * KPI卡片组件
 */
export function KpiCard({ title, value, change, changeType = 'up', icon }) {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-2xl font-semibold text-gray-900 mt-1">
            {typeof value === 'number' ? value.toLocaleString() : value}
          </p>
          {change !== undefined && (
            <p className={`text-xs mt-1 ${changeType === 'up' ? 'text-green-600' : 'text-red-600'}`}>
              {changeType === 'up' ? '↑' : '↓'} {Math.abs(change)}%
            </p>
          )}
        </div>
        {icon && <div className="text-3xl opacity-50">{icon}</div>}
      </div>
    </div>
  )
}

/**
 * 图表加载器
 */
export function ChartLoader({ height = 300 }) {
  return (
    <div className="animate-pulse" style={{ height: `${height}px` }}>
      <div className="h-full bg-gray-200 rounded" />
    </div>
  )
}
