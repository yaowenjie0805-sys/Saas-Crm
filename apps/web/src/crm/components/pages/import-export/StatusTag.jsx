import { memo } from 'react';
import { Tag } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined, SyncOutlined, DeleteOutlined, Alert } from '@ant-design/icons';

/**
 * 状态配置
 */
const STATUS_CONFIG = {
  PENDING: { color: 'default', icon: <ClockCircleOutlined />, text: '等待中' },
  RUNNING: { color: 'processing', icon: <SyncOutlined spin />, text: '处理中' },
  COMPLETED: { color: 'success', icon: <CheckCircleOutlined />, text: '已完成' },
  FAILED: { color: 'error', icon: <CloseCircleOutlined />, text: '失败' },
  PARTIAL_SUCCESS: { color: 'warning', icon: <Alert />, text: '部分成功' },
  CANCELLED: { color: 'default', icon: <DeleteOutlined />, text: '已取消' }
};

/**
 * 统一状态标签组件
 */
export const StatusTag = memo(function StatusTag({ status }) {
  const config = STATUS_CONFIG[status] || STATUS_CONFIG.PENDING;
  return (
    <Tag color={config.color} icon={config.icon}>
      {config.text}
    </Tag>
  );
});

export { STATUS_CONFIG };
