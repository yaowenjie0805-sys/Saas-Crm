import { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { useAppStore, selectSearchDomainSlice } from '../../store/appStore'

/**
 * Command Palette - 全局搜索命令面板
 * 支持快捷键 Cmd/Ctrl+K 唤起
 * 支持拼音搜索
 */
function CommandPaletteComponent({ isOpen, onClose, onResultSelect }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState({})
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const inputRef = useRef(null)
  const searchRequestIdRef = useRef(0)
  const searchAbortControllerRef = useRef(null)

  // 搜索结果
  const searchDomain = useAppStore(selectSearchDomainSlice)
  const searchHistory = searchDomain?.data?.history || []
  const flatResults = useMemo(() => flattenSearchResults(results), [results])

  const cancelSearchRequest = useCallback(() => {
    searchAbortControllerRef.current?.abort()
    searchAbortControllerRef.current = null
  }, [])

  // 搜索处理
  const handleSearch = useCallback(async (keyword) => {
    const requestId = ++searchRequestIdRef.current
    const trimmedKeyword = keyword.trim()
    cancelSearchRequest()
    if (!trimmedKeyword) {
      setResults({})
      setIsLoading(false)
      return
    }

    const controller = new AbortController()
    searchAbortControllerRef.current = controller
    setIsLoading(true)
    try {
      const response = await fetch(
        `/api/v2/search?q=${encodeURIComponent(trimmedKeyword)}&limit=20`,
        {
          headers: {
            'X-Tenant-Id': 'tenant_default',
          },
          signal: controller.signal,
        }
      )

      if (response.ok && searchRequestIdRef.current === requestId && !controller.signal.aborted) {
        const data = await response.json()
        setResults(data.results || {})
      }
    } catch (error) {
      if (error?.name !== 'AbortError') {
        console.error('Search error:', error)
      }
    } finally {
      if (searchRequestIdRef.current === requestId) {
        setIsLoading(false)
      }
      if (searchAbortControllerRef.current === controller) {
        searchAbortControllerRef.current = null
      }
    }
  }, [cancelSearchRequest])

  // 防抖搜索

  useEffect(() => {
    if (isOpen) return
    searchRequestIdRef.current += 1
    cancelSearchRequest()
    setIsLoading(false)
  }, [cancelSearchRequest, isOpen])

  // 聚焦输入框
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus()
    }
  }, [isOpen])

  useEffect(() => {
    setSelectedIndex((prev) => clampSelectedIndex(prev, flatResults.length))
  }, [flatResults.length])

  useEffect(() => {
    if (!isOpen) return undefined
    const timer = setTimeout(() => {
      handleSearch(query)
    }, 300)

    return () => clearTimeout(timer)
  }, [isOpen, query, handleSearch])

  // 键盘导航
  useEffect(() => () => {
    searchRequestIdRef.current += 1
    cancelSearchRequest()
  }, [cancelSearchRequest])

  const handleKeyDown = (e) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setSelectedIndex((prev) => clampSelectedIndex(prev + 1, flatResults.length))
        break
      case 'ArrowUp':
        e.preventDefault()
        setSelectedIndex((prev) => clampSelectedIndex(prev - 1, flatResults.length))
        break
      case 'Enter':
        e.preventDefault()
        if (flatResults[selectedIndex]) {
          handleSelect(flatResults[selectedIndex])
        }
        break
      case 'Escape':
        e.preventDefault()
        handleClose()
        break
    }
  }

  // 扁平化搜索结果
  const handleClose = useCallback(() => {
    searchRequestIdRef.current += 1
    cancelSearchRequest()
    setQuery('')
    setResults({})
    setSelectedIndex(0)
    setIsLoading(false)
    onClose?.()
  }, [cancelSearchRequest, onClose])

  // 选择结果
  const handleSelect = (item) => {
    if (onResultSelect) {
      onResultSelect(item)
    }
    handleClose()
  }

  // 获取类型标签
  const getTypeLabel = (type) => {
    const labels = {
      CUSTOMER: '客户',
      LEAD: '线索',
      OPPORTUNITY: '商机',
      CONTACT: '联系人',
      QUOTE: '报价',
      ORDER: '订单',
      CONTRACT: '合同',
      TASK: '任务',
    }
    return labels[type] || type
  }

  // 获取类型图标
  const getTypeIcon = (type) => {
    const icons = {
      CUSTOMER: '👥',
      LEAD: '🎯',
      OPPORTUNITY: '💰',
      CONTACT: '📧',
      QUOTE: '📄',
      ORDER: '🛒',
      CONTRACT: '📝',
      TASK: '✅',
    }
    return icons[type] || '📌'
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-[15vh]">
      {/* 背景遮罩 */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={handleClose}
      />

      {/* 搜索面板 */}
      <div className="relative w-full max-w-2xl bg-white rounded-xl shadow-2xl overflow-hidden">
        {/* 搜索输入 */}
        <div className="flex items-center border-b border-gray-200 px-4">
          <span className="text-gray-400 mr-3">🔍</span>
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="搜索客户、线索、商机... (支持拼音首字母)"
            className="flex-1 py-4 text-lg outline-none placeholder-gray-400"
          />
          {isLoading && (
            <div className="animate-spin text-gray-400">⏳</div>
          )}
          <button
            onClick={handleClose}
            className="ml-3 px-2 py-1 text-sm text-gray-500 bg-gray-100 rounded"
          >
            ESC
          </button>
        </div>

        {/* 搜索结果 */}
        <div className="max-h-[50vh] overflow-y-auto">
          {query && flatResults.length === 0 && !isLoading && (
            <div className="px-4 py-8 text-center text-gray-500">
              未找到 "{query}" 相关结果
            </div>
          )}

          {flatResults.length > 0 && (
            <div className="py-2">
              {Object.entries(results).map(([type, items]) => {
                const safeItems = Array.isArray(items) ? items : []
                return (
                <div key={type}>
                  <div className="px-4 py-2 text-xs font-semibold text-gray-500 uppercase bg-gray-50">
                    {getTypeLabel(type)} ({safeItems.length})
                  </div>
                  {safeItems.map((item) => {
                    const globalIndex = flatResults.findIndex(
                      (r) => r.id === item.id && r.type === type
                    )
                    return (
                      <div
                        key={`${type}-${item.id}`}
                        onClick={() =>
                          handleSelect({ ...item, type })
                        }
                        className={`px-4 py-3 cursor-pointer flex items-center gap-3 ${
                          globalIndex === selectedIndex
                            ? 'bg-primary-50'
                            : 'hover:bg-gray-50'
                        }`}
                      >
                        <span className="text-xl">{getTypeIcon(type)}</span>
                        <div className="flex-1">
                          <div className="font-medium text-gray-900">
                            {item.snippet?.split(' ')[0] || item.id}
                          </div>
                          <div className="text-sm text-gray-500 truncate">
                            {item.snippet}
                          </div>
                        </div>
                        <div className="text-xs text-gray-400">
                          #{globalIndex + 1}
                        </div>
                      </div>
                    )
                  })}
                </div>
              )})}
            </div>
          )}

          {/* 搜索历史 */}
          {!query && searchHistory.length > 0 && (
            <div className="py-2">
              <div className="px-4 py-2 text-xs font-semibold text-gray-500 uppercase bg-gray-50">
                最近搜索
              </div>
              {searchHistory.map((item) => (
                <div
                  key={item.id}
                  onClick={() => setQuery(item.query)}
                  className="px-4 py-3 cursor-pointer hover:bg-gray-50 flex items-center gap-3"
                >
                  <span className="text-gray-400">🕒</span>
                  <div className="flex-1">
                    <div className="font-medium text-gray-900">{item.name}</div>
                    <div className="text-sm text-gray-500">{item.query}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 底部提示 */}
        <div className="px-4 py-3 border-t border-gray-200 bg-gray-50 text-xs text-gray-500 flex items-center gap-4">
          <span>↑↓ 导航</span>
          <span>Enter 选择</span>
          <span>ESC 关闭</span>
        </div>
      </div>
    </div>
  )
}

/**
 * 搜索钩子
 */
// eslint-disable-next-line react-refresh/only-export-components
export function clampSelectedIndex(selectedIndex, flatResultsLength) {
  if (flatResultsLength <= 0) return 0
  return Math.min(Math.max(selectedIndex, 0), flatResultsLength - 1)
}

// eslint-disable-next-line react-refresh/only-export-components
export function flattenSearchResults(results) {
  const flat = []
  Object.entries(results || {}).forEach(([type, items]) => {
    ;(Array.isArray(items) ? items : []).forEach((item) => {
      flat.push({ ...item, type })
    })
  })
  return flat
}

// 导出组件
export function CommandPalette(props) {
  return <CommandPaletteComponent {...props} />
}
