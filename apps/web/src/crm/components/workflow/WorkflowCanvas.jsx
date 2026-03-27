import { useState, useEffect, useRef, useCallback } from 'react'

/**
 * 工作流可视化编辑器
 * 支持拖拽式节点编辑、连线和配置
 */
export function WorkflowCanvas({
  _workflowId,
  nodes = [],
  connections = [],
  onNodesChange,
  onConnectionsChange,
  onNodeSelect,
  selectedNodeId,
  readOnly = false,
}) {
  const canvasRef = useRef(null)
  const [dragState, setDragState] = useState(null)
  const [connecting, setConnecting] = useState(null)

  // 节点拖拽
  const handleNodeMouseDown = (e, node) => {
    if (readOnly) return
    e.stopPropagation()

    setDragState({
      nodeId: node.id,
      startX: e.clientX,
      startY: e.clientY,
      nodeStartX: node.positionX,
      nodeStartY: node.positionY,
    })

    if (onNodeSelect) {
      onNodeSelect(node.id)
    }
  }

  // 画布鼠标移动
  const handleMouseMove = useCallback((e) => {
    if (!dragState) return

    const dx = e.clientX - dragState.startX
    const dy = e.clientY - dragState.startY

    const newNodes = nodes.map(n => {
      if (n.id === dragState.nodeId) {
        return {
          ...n,
          positionX: Math.max(0, dragState.nodeStartX + dx),
          positionY: Math.max(0, dragState.nodeStartY + dy),
        }
      }
      return n
    })

    if (onNodesChange) {
      onNodesChange(newNodes)
    }
  }, [dragState, nodes, onNodesChange])

  // 画布鼠标释放
  const handleMouseUp = useCallback(() => {
    setDragState(null)
  }, [])

  // 添加监听
  useEffect(() => {
    if (dragState) {
      window.addEventListener('mousemove', handleMouseMove)
      window.addEventListener('mouseup', handleMouseUp)
      return () => {
        window.removeEventListener('mousemove', handleMouseMove)
        window.removeEventListener('mouseup', handleMouseUp)
      }
    }
  }, [dragState, handleMouseMove, handleMouseUp])

  // 开始连线
  const handleConnectionStart = (e, node) => {
    if (readOnly) return
    e.stopPropagation()
    setConnecting({
      sourceNodeId: node.id,
      startX: node.positionX + 100,
      startY: node.positionY + 30,
      currentX: e.clientX,
      currentY: e.clientY,
    })
  }

  // 生成唯一ID
  const generateId = useCallback(() => {
    return `conn-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }, [])

  // 结束连线
  const handleConnectionEnd = useCallback((node) => {
    if (connecting && connecting.sourceNodeId !== node.id) {
      const newConnection = {
        id: generateId(),
        sourceNodeId: connecting.sourceNodeId,
        targetNodeId: node.id,
        connectionType: 'DEFAULT',
        label: '',
      }

      if (onConnectionsChange) {
        onConnectionsChange([...connections, newConnection])
      }
    }
    setConnecting(null)
  }, [connecting, connections, generateId, onConnectionsChange])

  // 获取节点颜色
  const getNodeColor = (nodeType) => {
    const colors = {
      TRIGGER: { bg: '#3B82F6', border: '#2563EB', icon: '▶️' },
      ACTION: { bg: '#10B981', border: '#059669', icon: '⚡' },
      CONDITION: { bg: '#F59E0B', border: '#D97706', icon: '❓' },
      NOTIFICATION: { bg: '#8B5CF6', border: '#7C3AED', icon: '🔔' },
      APPROVAL: { bg: '#EF4444', border: '#DC2626', icon: '✅' },
      WAIT: { bg: '#6B7280', border: '#4B5563', icon: '⏰' },
      CC: { bg: '#EC4899', border: '#DB2777', icon: '📧' },
      START: { bg: '#22C55E', border: '#16A34A', icon: '▶️' },
      END: { bg: '#EF4444', border: '#DC2626', icon: '🏁' },
    }
    return colors[nodeType] || { bg: '#6B7280', border: '#4B5563', icon: '📦' }
  }

  // 渲染连线
  const renderConnections = () => {
    return connections.map(conn => {
      const sourceNode = nodes.find(n => n.id === conn.sourceNodeId)
      const targetNode = nodes.find(n => n.id === conn.targetNodeId)

      if (!sourceNode || !targetNode) return null

      const x1 = sourceNode.positionX + 100
      const y1 = sourceNode.positionY + 30
      const x2 = targetNode.positionX
      const y2 = targetNode.positionY + 30

      return (
        <g key={conn.id}>
          <path
            d={`M ${x1} ${y1} C ${x1 + 50} ${y1}, ${x2 - 50} ${y2}, ${x2} ${y2}`}
            fill="none"
            stroke="#94A3B8"
            strokeWidth={2}
            markerEnd="url(#arrowhead)"
          />
          {conn.label && (
            <text
              x={(x1 + x2) / 2}
              y={(y1 + y2) / 2 - 10}
              textAnchor="middle"
              className="text-xs fill-gray-500"
            >
              {conn.label}
            </text>
          )}
        </g>
      )
    })
  }

  // 计算连接线的坐标偏移
  const [canvasOffset, setCanvasOffset] = useState({ left: 0, top: 0 })

  useEffect(() => {
    if (canvasRef.current) {
      const rect = canvasRef.current.getBoundingClientRect()
      setCanvasOffset({ left: rect.left, top: rect.top })
    }
  }, [connecting])

  // 渲染正在连线的临时路径
  const renderConnectingLine = () => {
    if (!connecting) return null

    const sourceNode = nodes.find(n => n.id === connecting.sourceNodeId)
    if (!sourceNode) return null

    return (
      <path
        d={`M ${sourceNode.positionX + 100} ${sourceNode.positionY + 30} L ${connecting.currentX - canvasOffset.left} ${connecting.currentY - canvasOffset.top}`}
        fill="none"
        stroke="#3B82F6"
        strokeWidth={2}
        strokeDasharray="5,5"
      />
    )
  }

  return (
    <div
      ref={canvasRef}
      className="relative w-full h-full min-h-[500px] bg-gray-50 overflow-auto"
      onMouseMove={(e) => {
        if (connecting) {
          setConnecting({
            ...connecting,
            currentX: e.clientX,
            currentY: e.clientY,
          })
        }
      }}
      onMouseUp={() => setConnecting(null)}
    >
      <svg className="absolute inset-0 w-full h-full pointer-events-none">
        <defs>
          <marker
            id="arrowhead"
            markerWidth="10"
            markerHeight="7"
            refX="9"
            refY="3.5"
            orient="auto"
          >
            <polygon points="0 0, 10 3.5, 0 7" fill="#94A3B8" />
          </marker>
        </defs>

        {renderConnections()}
        {renderConnectingLine()}
      </svg>

      {nodes.map(node => {
        const colors = getNodeColor(node.nodeType)
        const isSelected = selectedNodeId === node.id

        return (
          <div
            key={node.id}
            className={`absolute cursor-pointer transition-shadow ${
              isSelected ? 'z-10' : 'z-0'
            }`}
            style={{
              left: node.positionX,
              top: node.positionY,
            }}
            onMouseDown={(e) => handleNodeMouseDown(e, node)}
            onMouseUp={() => connecting && handleConnectionEnd(node)}
          >
            <div
              className={`flex items-center gap-2 px-4 py-2 rounded-lg border-2 shadow-sm min-w-[120px] ${
                isSelected ? 'ring-2 ring-primary-500 ring-offset-2' : ''
              }`}
              style={{
                backgroundColor: colors.bg,
                borderColor: colors.border,
              }}
            >
              <span className="text-lg">{colors.icon}</span>
              <div className="text-white">
                <div className="text-sm font-medium">{node.name}</div>
                {node.nodeSubtype && (
                  <div className="text-xs opacity-80">{node.nodeSubtype}</div>
                )}
              </div>

              {/* 连接点 */}
              {!readOnly && (
                <>
                  <div
                    className="absolute -right-1 top-1/2 -translate-y-1/2 w-3 h-3 bg-white rounded-full border-2 border-gray-400 cursor-crosshair hover:scale-125 transition-transform"
                    onMouseDown={(e) => handleConnectionStart(e, node)}
                  />
                  <div className="absolute -left-1 top-1/2 -translate-y-1/2 w-3 h-3 bg-white rounded-full border-2 border-gray-400 hover:scale-125 transition-transform" />
                </>
              )}
            </div>
          </div>
        )
      })}

      {nodes.length === 0 && !readOnly && (
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="text-center text-gray-400">
            <div className="text-4xl mb-2">📋</div>
            <p>从左侧拖拽节点开始设计工作流</p>
          </div>
        </div>
      )}
    </div>
  )
}

/**
 * 节点组件面板
 */
export function NodePalette({ onDragStart }) {
  const categories = [
    {
      name: '触发器',
      icon: '▶️',
      nodes: [
        { type: 'TRIGGER', subtype: 'RECORD_CREATED', label: '记录创建' },
        { type: 'TRIGGER', subtype: 'RECORD_UPDATED', label: '记录更新' },
        { type: 'TRIGGER', subtype: 'FIELD_CHANGED', label: '字段变更' },
      ],
    },
    {
      name: '动作',
      icon: '⚡',
      nodes: [
        { type: 'ACTION', subtype: 'CREATE_TASK', label: '创建任务' },
        { type: 'ACTION', subtype: 'UPDATE_FIELD', label: '更新字段' },
        { type: 'ACTION', subtype: 'SEND_EMAIL', label: '发送邮件' },
      ],
    },
    {
      name: '条件',
      icon: '❓',
      nodes: [
        { type: 'CONDITION', subtype: 'IF', label: '条件判断' },
        { type: 'CONDITION', subtype: 'SWITCH', label: '多条件分支' },
      ],
    },
    {
      name: '审批',
      icon: '✅',
      nodes: [
        { type: 'APPROVAL', subtype: 'SINGLE', label: '单人审批' },
        { type: 'APPROVAL', subtype: 'SERIAL', label: '逐级审批' },
        { type: 'APPROVAL', subtype: 'PARALLEL', label: '会签审批' },
      ],
    },
    {
      name: '通知',
      icon: '🔔',
      nodes: [
        { type: 'NOTIFICATION', subtype: 'WECHAT_WORK', label: '企业微信' },
        { type: 'NOTIFICATION', subtype: 'DINGTALK', label: '钉钉通知' },
        { type: 'NOTIFICATION', subtype: 'IN_APP', label: '站内通知' },
      ],
    },
    {
      name: '辅助',
      icon: '📦',
      nodes: [
        { type: 'AUXILIARY', subtype: 'START', label: '开始' },
        { type: 'AUXILIARY', subtype: 'END', label: '结束' },
        { type: 'WAIT', subtype: 'DELAY', label: '延时等待' },
      ],
    },
  ]

  return (
    <div className="w-64 bg-white border-r border-gray-200 overflow-y-auto">
      <div className="p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-4">节点组件</h3>

        {categories.map(category => (
          <div key={category.name} className="mb-4">
            <div className="flex items-center gap-2 mb-2">
              <span>{category.icon}</span>
              <span className="text-sm font-medium text-gray-600">{category.name}</span>
            </div>

            <div className="space-y-1">
              {category.nodes.map(node => (
                <div
                  key={`${node.type}-${node.subtype}`}
                  className="px-3 py-2 bg-gray-50 rounded border border-gray-200 text-sm cursor-grab hover:bg-gray-100 active:cursor-grabbing transition-colors"
                  draggable
                  onDragStart={(e) => {
                    e.dataTransfer.setData('application/json', JSON.stringify({
                      nodeType: node.type,
                      nodeSubtype: node.subtype,
                      name: node.label,
                    }))
                    if (onDragStart) onDragStart(node)
                  }}
                >
                  {node.label}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

/**
 * 工作流编辑器容器
 */
export function WorkflowEditor({ _workflowId, initialData }) {
  const [nodes, setNodes] = useState(initialData?.nodes || [])
  const [connections, setConnections] = useState(initialData?.connections || [])
  const [selectedNode, setSelectedNode] = useState(null)

  const handleDrop = (e) => {
    e.preventDefault()
    const data = e.dataTransfer.getData('application/json')
    if (!data) return

    try {
      const nodeData = JSON.parse(data)
      const rect = e.currentTarget.getBoundingClientRect()

      const newNode = {
        id: `node-${Date.now()}`,
        nodeType: nodeData.nodeType,
        nodeSubtype: nodeData.nodeSubtype,
        name: nodeData.name,
        positionX: e.clientX - rect.left,
        positionY: e.clientY - rect.top,
        configJson: '{}',
      }

      setNodes([...nodes, newNode])
    } catch (error) {
      console.error('Drop error:', error)
    }
  }

  return (
    <div className="flex h-[600px] border border-gray-200 rounded-lg overflow-hidden">
      <NodePalette />
      <div
        className="flex-1"
        onDrop={handleDrop}
        onDragOver={(e) => e.preventDefault()}
      >
        <WorkflowCanvas
          nodes={nodes}
          connections={connections}
          onNodesChange={setNodes}
          onConnectionsChange={setConnections}
          selectedNodeId={selectedNode}
          onNodeSelect={setSelectedNode}
        />
      </div>
    </div>
  )
}
