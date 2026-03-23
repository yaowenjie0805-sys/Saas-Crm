import { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Timeline,
  Tag,
  Button,
  Space,
  Modal,
  message,
  Descriptions,
  Progress,
  Statistic,
  Row,
  Col,
  Empty,
  Spin,
  Alert,
  Tooltip,
  Divider
} from 'antd';
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  BranchesOutlined,
  NodeIndexOutlined,
  BugOutlined
} from '@ant-design/icons';

/**
 * 工作流执行监控组件
 * 展示工作流执行状态、节点执行结果、实时进度等
 */
export function WorkflowExecutionMonitor({
  executionId,
  workflowId,
  visible,
  onClose,
  onRetrySuccess
}) {
  const [execution, setExecution] = useState(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTab, setActiveTab] = useState('timeline');

  // 加载执行详情
  const loadExecutionDetail = useCallback(async () => {
    if (!executionId) return;

    setLoading(true);
    try {
      const response = await fetch(
        `/api/v2/workflows/executions/${executionId}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'X-Tenant-Id': localStorage.getItem('tenantId')
          }
        }
      );
      const data = await response.json();

      if (data.success) {
        setExecution(data);
      } else {
        message.error(data.message || '加载失败');
      }
    } catch (error) {
      message.error('加载失败');
    } finally {
      setLoading(false);
    }
  }, [executionId]);

  useEffect(() => {
    if (visible && executionId) {
      loadExecutionDetail();
    }
  }, [visible, executionId, loadExecutionDetail]);

  // 刷新
  const handleRefresh = async () => {
    setRefreshing(true);
    await loadExecutionDetail();
    setRefreshing(false);
  };

  // 取消执行
  const handleCancel = async () => {
    Modal.confirm({
      title: '确认取消',
      content: '确定要取消这个工作流执行吗？',
      onOk: async () => {
        try {
          const response = await fetch(
            `/api/v2/workflows/executions/${executionId}/cancel`,
            {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'X-Tenant-Id': localStorage.getItem('tenantId')
              },
              body: JSON.stringify({
                cancelledBy: localStorage.getItem('userId')
              })
            }
          );

          const data = await response.json();
          if (data.success) {
            message.success('已取消');
            loadExecutionDetail();
          } else {
            message.error(data.message || '取消失败');
          }
        } catch (error) {
          message.error('取消失败');
        }
      }
    });
  };

  // 重试执行
  const handleRetry = async () => {
    try {
      const response = await fetch(
        `/api/v2/workflows/executions/${executionId}/retry`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'X-Tenant-Id': localStorage.getItem('tenantId')
          }
        }
      );

      const data = await response.json();
      if (data.success) {
        message.success('已重新执行');
        onRetrySuccess?.(data.newExecutionId);
      } else {
        message.error(data.message || '重试失败');
      }
    } catch (error) {
      message.error('重试失败');
    }
  };

  // 获取状态配置
  const getStatusConfig = (status) => {
    const configs = {
      RUNNING: { color: 'processing', icon: <SyncOutlined spin />, text: '执行中' },
      COMPLETED: { color: 'success', icon: <CheckCircleOutlined />, text: '已完成' },
      FAILED: { color: 'error', icon: <CloseCircleOutlined />, text: '失败' },
      CANCELLED: { color: 'default', icon: <StopOutlined />, text: '已取消' },
      PENDING: { color: 'warning', icon: <ClockCircleOutlined />, text: '等待中' }
    };
    return configs[status] || configs.PENDING;
  };

  // 获取节点类型配置
  const getNodeTypeConfig = (nodeType) => {
    const configs = {
      TRIGGER: { color: 'blue', icon: <PlayCircleOutlined /> },
      ACTION: { color: 'green', icon: <NodeIndexOutlined /> },
      CONDITION: { color: 'orange', icon: <BranchesOutlined /> },
      NOTIFICATION: { color: 'purple', icon: <SyncOutlined /> },
      APPROVAL: { color: 'red', icon: <CheckCircleOutlined /> },
      WAIT: { color: 'cyan', icon: <ClockCircleOutlined /> },
      END: { color: 'gray', icon: <CheckCircleOutlined /> }
    };
    return configs[nodeType] || configs.ACTION;
  };

  // 渲染时间线
  const renderTimeline = () => {
    const nodeResults = execution?.context?.nodeResults || {};
    const nodes = Object.entries(nodeResults);

    if (nodes.length === 0) {
      return <Empty description="暂无节点执行记录" />;
    }

    return (
      <Timeline mode="left">
        {nodes.map(([nodeId, result]) => {
          const config = getNodeTypeConfig(result.nodeType);
          const statusColor = result.success ? 'green' : result.waiting ? 'blue' : 'red';

          return (
            <Timeline.Item
              key={nodeId}
              dot={result.success ? <CheckCircleOutlined style={{ fontSize: 16 }} /> :
                     result.waiting ? <ClockCircleOutlined style={{ fontSize: 16 }} /> :
                     <CloseCircleOutlined style={{ fontSize: 16 }} />}
              color={statusColor}
            >
              <Card size="small" style={{ marginBottom: 8 }}>
                <Space>
                  <Tag color={config.color} icon={config.icon}>
                    {result.nodeType} - {result.nodeSubtype || 'N/A'}
                  </Tag>
                  {result.success && <Tag color="success">成功</Tag>}
                  {result.waiting && <Tag color="processing">等待</Tag>}
                  {!result.success && !result.waiting && <Tag color="error">失败</Tag>}
                </Space>

                {result.errorMessage && (
                  <Alert
                    type="error"
                    message={result.errorMessage}
                    style={{ marginTop: 8 }}
                    showIcon
                  />
                )}

                {result.outputData && Object.keys(result.outputData).length > 0 && (
                  <Descriptions size="small" column={1} style={{ marginTop: 8 }}>
                    {Object.entries(result.outputData).slice(0, 5).map(([key, value]) => (
                      <Descriptions.Item key={key} label={key}>
                        {String(value)}
                      </Descriptions.Item>
                    ))}
                  </Descriptions>
                )}
              </Card>
            </Timeline.Item>
          );
        })}
      </Timeline>
    );
  };

  // 渲染变量上下文
  const renderVariables = () => {
    const variables = execution?.context?.variables || {};
    const triggerData = execution?.context?.triggerData || {};

    return (
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card title="触发数据" size="small">
            {Object.keys(triggerData).length === 0 ? (
              <Empty description="无触发数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Descriptions size="small" column={2}>
                {Object.entries(triggerData).map(([key, value]) => (
                  <Descriptions.Item key={key} label={key}>
                    {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                  </Descriptions.Item>
                ))}
              </Descriptions>
            )}
          </Card>
        </Col>

        <Col span={24}>
          <Card title="执行变量" size="small">
            {Object.keys(variables).length === 0 ? (
              <Empty description="无执行变量" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Descriptions size="small" column={2}>
                {Object.entries(variables).map(([key, value]) => (
                  <Descriptions.Item key={key} label={key}>
                    <Tooltip title={typeof value === 'object' ? JSON.stringify(value) : value}>
                      {typeof value === 'object' ? JSON.stringify(value).substring(0, 50) + '...' : String(value)}
                    </Tooltip>
                  </Descriptions.Item>
                ))}
              </Descriptions>
            )}
          </Card>
        </Col>
      </Row>
    );
  };

  // 渲染执行统计
  const renderStats = () => {
    const startTime = execution?.execution?.startedAt;
    const endTime = execution?.execution?.completedAt;
    const duration = startTime && endTime
      ? new Date(endTime) - new Date(startTime)
      : startTime
        ? Date.now() - new Date(startTime)
        : 0;

    const nodeResults = execution?.context?.nodeResults || {};
    const successCount = Object.values(nodeResults).filter(r => r.success).length;
    const failCount = Object.values(nodeResults).filter(r => !r.success).length;

    return (
      <Row gutter={16}>
        <Col span={8}>
          <Statistic
            title="执行状态"
            value={execution?.execution?.status}
            prefix={getStatusConfig(execution?.execution?.status)?.icon}
            valueStyle={{ color: getStatusConfig(execution?.execution?.status)?.color }}
          />
        </Col>
        <Col span={8}>
          <Statistic
            title="执行时长"
            value={Math.round(duration / 1000)}
            suffix="秒"
            prefix={<ClockCircleOutlined />}
          />
        </Col>
        <Col span={8}>
          <Statistic
            title="节点统计"
            value={`${successCount}/${failCount}`}
            suffix="成功/失败"
            prefix={<NodeIndexOutlined />}
          />
        </Col>
      </Row>
    );
  };

  if (!visible) return null;

  return (
    <Modal
      title={
        <Space>
          <BranchesOutlined />
          <span>工作流执行监控</span>
          {execution?.execution && (
            <Tag color={getStatusConfig(execution.execution.status)?.color}>
              {getStatusConfig(execution.execution.status)?.text}
            </Tag>
          )}
        </Space>
      }
      open={visible}
      onCancel={onClose}
      width={800}
      footer={
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
            loading={refreshing}
          >
            刷新
          </Button>

          {execution?.execution?.status === 'RUNNING' && (
            <Button
              danger
              icon={<StopOutlined />}
              onClick={handleCancel}
            >
              取消执行
            </Button>
          )}

          {execution?.execution?.status === 'FAILED' && (
            <Button
              type="primary"
              icon={<ReloadOutlined />}
              onClick={handleRetry}
            >
              重试
            </Button>
          )}

          <Button onClick={onClose}>关闭</Button>
        </Space>
      }
    >
      <Spin spinning={loading}>
        {execution ? (
          <div>
            {/* 执行统计 */}
            {renderStats()}

            <Divider />

            {/* Tab切换 */}
            <Space style={{ marginBottom: 16 }}>
              <Button
                type={activeTab === 'timeline' ? 'primary' : 'default'}
                onClick={() => setActiveTab('timeline')}
                icon={<NodeIndexOutlined />}
              >
                执行时间线
              </Button>
              <Button
                type={activeTab === 'variables' ? 'primary' : 'default'}
                onClick={() => setActiveTab('variables')}
                icon={<BugOutlined />}
              >
                执行上下文
              </Button>
            </Space>

            {/* 内容 */}
            <div style={{ maxHeight: 400, overflow: 'auto' }}>
              {activeTab === 'timeline' && renderTimeline()}
              {activeTab === 'variables' && renderVariables()}
            </div>

            {/* 当前节点 */}
            {execution?.execution?.currentNodeId && (
              <>
                <Divider />
                <Alert
                  type="info"
                  icon={<ClockCircleOutlined />}
                  message={
                    <Space>
                      <span>当前执行节点：</span>
                      <Tag>{execution.execution.currentNodeId}</Tag>
                    </Space>
                  }
                />
              </>
            )}

            {/* 错误信息 */}
            {execution?.execution?.errorMessage && (
              <>
                <Divider />
                <Alert
                  type="error"
                  title="执行错误"
                  message={execution.execution.errorMessage}
                  description={execution.execution.errorDetails}
                  showIcon
                />
              </>
            )}
          </div>
        ) : (
          <Empty description="加载执行详情..." />
        )}
      </Spin>
    </Modal>
  );
}

export default WorkflowExecutionMonitor;
