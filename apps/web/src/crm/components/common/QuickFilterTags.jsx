import { useState, useEffect, useCallback, useRef } from 'react'
import { api } from '../../shared'

/**
 * 快捷筛选标签组件 - 国内特色
 * 支持一键切换常用筛选条件
 */
export function QuickFilterTags({ entityType, onFilterChange }) {
  const [filters, setFilters] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const loadRequestIdRef = useRef(0)
  const loadAbortControllerRef = useRef(null)

  const cancelLoadQuickFilters = useCallback(() => {
    loadAbortControllerRef.current?.abort()
    loadAbortControllerRef.current = null
  }, [])

  const loadQuickFilters = useCallback(async (currentEntityType) => {
    const requestId = ++loadRequestIdRef.current
    cancelLoadQuickFilters()

    if (!currentEntityType) {
      setFilters([])
      setSelectedId(null)
      setIsLoading(false)
      return
    }

    setFilters([])
    setSelectedId(null)
    const controller = new AbortController()
    loadAbortControllerRef.current = controller
    setIsLoading(true)
    try {
      const data = await api(
        `/v2/filters/quick?entityType=${currentEntityType}`,
        { signal: controller.signal },
      )

      if (loadRequestIdRef.current === requestId && !controller.signal.aborted) {
        setFilters(data.filters || [])
      }
    } catch (error) {
      if (error?.name !== 'AbortError') {
        console.error('Load quick filters error:', error)
      }
    } finally {
      if (loadRequestIdRef.current === requestId) {
        setIsLoading(false)
      }
      if (loadAbortControllerRef.current === controller) {
        loadAbortControllerRef.current = null
      }
    }
  }, [cancelLoadQuickFilters])

  useEffect(() => {
    setSelectedId(null)
    loadQuickFilters(entityType)
  }, [entityType, loadQuickFilters])

  useEffect(() => () => {
    loadRequestIdRef.current += 1
    cancelLoadQuickFilters()
  }, [cancelLoadQuickFilters])

  // 选择筛选
  const handleSelect = (filter) => {
    if (selectedId === filter.id) {
      // 取消选择
      setSelectedId(null)
      if (onFilterChange) {
        onFilterChange(null)
      }
    } else {
      setSelectedId(filter.id)
      if (onFilterChange) {
        onFilterChange(filter)
      }
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center gap-2">
        <div className="animate-pulse h-8 w-20 bg-gray-200 rounded-full" />
        <div className="animate-pulse h-8 w-20 bg-gray-200 rounded-full" />
        <div className="animate-pulse h-8 w-20 bg-gray-200 rounded-full" />
      </div>
    )
  }

  return (
    <div className="flex items-center gap-2 flex-wrap">
      {filters.map((filter) => (
        <button
          key={filter.id}
          onClick={() => handleSelect(filter)}
          className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium transition-all ${
            selectedId === filter.id
              ? 'bg-primary-600 text-white shadow-sm'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          <span>{filter.icon || '🏷️'}</span>
          <span>{filter.name}</span>
          {selectedId === filter.id && <span>✓</span>}
        </button>
      ))}

      {filters.length === 0 && (
        <span className="text-sm text-gray-400">
          暂无快捷筛选，点击右侧添加
        </span>
      )}
    </div>
  )
}

/**
 * 快捷筛选管理弹窗
 */
export function QuickFilterManager({ _entityType, currentFilter, onSave }) {
  const [name, setName] = useState('')
  const [icon, setIcon] = useState('🏷️')

  const commonIcons = ['🏷️', '🔥', '⭐', '💰', '📊', '✅', '🎯', '💼', '📈', '🚀']

  const handleSave = () => {
    if (!name.trim()) {
      alert('请输入筛选名称')
      return
    }

    if (onSave) {
      onSave({
        name: name.trim(),
        icon,
        filterConfig: currentFilter,
      })
    }

    setName('')
    setIcon('🏷️')
  }

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          筛选名称
        </label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="例如：我的客户、重点商机"
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          选择图标
        </label>
        <div className="flex gap-2 flex-wrap">
          {commonIcons.map((emoji) => (
            <button
              key={emoji}
              onClick={() => setIcon(emoji)}
              className={`w-10 h-10 text-xl rounded-lg border-2 transition-all ${
                icon === emoji
                  ? 'border-primary-500 bg-primary-50'
                  : 'border-gray-200 hover:border-gray-300'
              }`}
            >
              {emoji}
            </button>
          ))}
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-4 border-t">
        <button
          onClick={() => onSave && onSave(null)}
          className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
        >
          取消
        </button>
        <button
          onClick={handleSave}
          className="px-4 py-2 text-white bg-primary-600 rounded-lg hover:bg-primary-700"
        >
          保存筛选
        </button>
      </div>
    </div>
  )
}

const DEFAULT_QUICK_FILTERS = {
  CUSTOMER: [
    { name: '全部', icon: '👥', filterConfig: {} },
    { name: '我的客户', icon: '👤', filterConfig: { owner: 'current' } },
    { name: '重点客户', icon: '⭐', filterConfig: { tag: 'VIP' } },
    { name: '待跟进', icon: '📋', filterConfig: { status: 'PENDING' } },
  ],
  LEAD: [
    { name: '全部', icon: '🎯', filterConfig: {} },
    { name: '今日新增', icon: '🆕', filterConfig: { createdAt: 'today' } },
    { name: '待分配', icon: '⏳', filterConfig: { owner: 'unassigned' } },
    { name: '重点线索', icon: '⭐', filterConfig: { priority: 'HIGH' } },
  ],
  OPPORTUNITY: [
    { name: '全部', icon: '💰', filterConfig: {} },
    { name: '我的商机', icon: '👤', filterConfig: { owner: 'current' } },
    { name: '赢单', icon: '✅', filterConfig: { stage: 'CLOSED_WON' } },
    { name: '跟进中', icon: '🔥', filterConfig: { stage: 'PROPOSAL' } },
  ],
}
