import { useState, useEffect } from 'react'

/**
 * 全局搜索快捷键钩子
 * 支持 Cmd/Ctrl+K 唤起搜索面板
 */
export function useGlobalSearch() {
  const [isOpen, setIsOpen] = useState(false)

  // 快捷键监听
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        setIsOpen(true)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  return { isOpen, setIsOpen }
}
