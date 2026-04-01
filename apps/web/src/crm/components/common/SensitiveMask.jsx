import { useState, useEffect } from 'react'
import { api } from '../../shared'

/**
 * 敏感字段脱敏显示组件 - 国内特色
 * 支持手机号、身份证、银行卡等敏感字段的脱敏显示
 */
export function SensitiveField({ value, fieldType, unmasked = false, onRequestUnmask }) {
  const [isRevealed, setIsRevealed] = useState(unmasked)

  // 默认脱敏规则
  const getMaskedValue = (val, type) => {
    if (!val) return ''

    switch (type?.toUpperCase()) {
      case 'PHONE':
      case 'MOBILE':
        // 手机号：138****5678
        if (val.length >= 11) {
          return val.substring(0, 3) + '****' + val.substring(val.length - 4)
        }
        return '***'

      case 'ID_CARD':
      case 'IDENTITY':
        // 身份证：430***********1234
        if (val.length >= 18) {
          return val.substring(0, 3) + '***********' + val.substring(val.length - 4)
        }
        return '*****************'

      case 'BANK_CARD':
      case 'BANKCARD':
        // 银行卡：6222 **** **** 1234
        if (val.length >= 16) {
          return val.substring(0, 4) + ' **** **** ' + val.substring(val.length - 4)
        }
        return '**** **** **** ****'

      case 'EMAIL': {
        // 邮箱：t***@example.com
        const atIndex = val.indexOf('@')
        if (atIndex > 1) {
          return val.substring(0, 1) + '***' + val.substring(atIndex)
        }
        return '***'
      }

      case 'ADDRESS':
        // 地址：只显示省市
        if (val.length > 10) {
          return val.substring(0, 10) + '...'
        }
        return val

      case 'NAME':
        // 姓名：只显示姓
        if (val.length >= 2) {
          return val.substring(0, 1) + '*'
        }
        return '*'

      default:
        // 默认脱敏：保留首尾字符
        if (val.length > 4) {
          const start = val.substring(0, 2)
          const end = val.substring(val.length - 2)
          const mask = '*'.repeat(Math.min(val.length - 4, 6))
          return start + mask + end
        }
        return '*'.repeat(val.length)
    }
  }

  const maskedValue = getMaskedValue(value, fieldType)

  // 点击显示/隐藏
  const handleToggle = (e) => {
    e.stopPropagation()
    if (isRevealed) {
      setIsRevealed(false)
    } else if (onRequestUnmask) {
      // 调用回调请求解蔽（可能需要验证）
      onRequestUnmask()
    } else {
      setIsRevealed(true)
    }
  }

  return (
    <span className="inline-flex items-center gap-1">
      <span className={isRevealed ? 'text-gray-900' : 'text-gray-600'}>
        {isRevealed ? value : maskedValue}
      </span>
      {value && (
        <button
          onClick={handleToggle}
          className="p-0.5 text-gray-400 hover:text-gray-600 transition-colors"
          title={isRevealed ? '隐藏' : '显示'}
        >
          {isRevealed ? (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
            </svg>
          ) : (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
          )}
        </button>
      )}
    </span>
  )
}

/**
 * 敏感数据表格单元格
 */
export function SensitiveTableCell({ value, fieldType, columnKey }) {
  // 自动识别敏感字段类型
  const detectFieldType = (key) => {
    const keyLower = key?.toLowerCase() || ''
    if (keyLower.includes('phone') || keyLower.includes('mobile') || keyLower.includes('tel')) {
      return 'PHONE'
    }
    if (keyLower.includes('idcard') || keyLower.includes('id_card') || keyLower.includes('identity')) {
      return 'ID_CARD'
    }
    if (keyLower.includes('bank') || keyLower.includes('card')) {
      return 'BANK_CARD'
    }
    if (keyLower.includes('email')) {
      return 'EMAIL'
    }
    return fieldType
  }

  const type = detectFieldType(columnKey)

  if (!value) return <span className="text-gray-400">-</span>

  return <SensitiveField value={value} fieldType={type} />
}

/**
 * 敏感字段配置面板
 */
export function SensitiveFieldConfigPanel() {
  const [configs, setConfigs] = useState([])
  const [, setLoading] = useState(false)

  // 预定义的敏感字段类型
  const fieldTypes = [
    { value: 'PHONE', label: '手机号', pattern: '138****5678', icon: '📱' },
    { value: 'ID_CARD', label: '身份证', pattern: '430***********1234', icon: '🪪' },
    { value: 'BANK_CARD', label: '银行卡', pattern: '6222 **** **** 1234', icon: '💳' },
    { value: 'EMAIL', label: '邮箱', pattern: 't***@example.com', icon: '📧' },
    { value: 'ADDRESS', label: '地址', pattern: '北京市朝阳区...', icon: '📍' },
  ]

  // 加载配置
  useEffect(() => {
    loadConfigs()
  }, [])

  const loadConfigs = async () => {
    setLoading(true)
    try {
      const data = await api('/v2/permissions/sensitive-fields')
      setConfigs(data.configs || [])
    } catch (error) {
      console.error('Load configs error:', error)
    } finally {
      setLoading(false)
    }
  }

  // 保存配置
  const handleSave = async (entityType, fieldName, maskType) => {
    try {
      await api('/v2/permissions/sensitive-fields', {
        method: 'POST',
        body: JSON.stringify({ entityType, fieldName, maskType }),
      })
      await loadConfigs()
    } catch (error) {
      console.error('Save config error:', error)
    }
  }

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-medium text-gray-900">敏感字段配置</h3>
      <p className="text-sm text-gray-500">
        配置需要脱敏显示的字段，系统将自动对手机号、身份证、银行卡等敏感信息进行脱敏处理。
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {fieldTypes.map((type) => (
          <div
            key={type.value}
            className="p-4 bg-gray-50 rounded-lg border border-gray-200"
          >
            <div className="flex items-center gap-2 mb-2">
              <span className="text-xl">{type.icon}</span>
              <span className="font-medium text-gray-900">{type.label}</span>
            </div>
            <div className="text-sm text-gray-500 mb-3">
              示例：{type.pattern}
            </div>
            <div className="flex items-center justify-between">
              <span className="text-xs text-gray-400">
                {type.value}
              </span>
              <button
                onClick={() => handleSave('CUSTOMER', type.value, type.value)}
                className="px-3 py-1 text-sm bg-primary-600 text-white rounded hover:bg-primary-700"
              >
                启用
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* 已配置列表 */}
      {configs.length > 0 && (
        <div className="mt-6">
          <h4 className="text-sm font-medium text-gray-700 mb-3">已配置字段</h4>
          <div className="border rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">实体类型</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">字段名</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">脱敏方式</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">操作</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {configs.map((config) => (
                  <tr key={config.id}>
                    <td className="px-4 py-2 text-sm text-gray-900">{config.entityType}</td>
                    <td className="px-4 py-2 text-sm text-gray-900">{config.fieldName}</td>
                    <td className="px-4 py-2 text-sm text-gray-500">{config.maskType}</td>
                    <td className="px-4 py-2">
                      <button className="text-red-600 hover:text-red-800 text-sm">删除</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
