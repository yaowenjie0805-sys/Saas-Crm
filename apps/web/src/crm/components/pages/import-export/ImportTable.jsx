import { memo } from 'react';
import { Table, Tag, Space, Button, Progress } from 'antd';
import { FileExcelOutlined, DeleteOutlined } from '@ant-design/icons';
import { StatusTag } from './StatusTag';

/**
 * 导入任务表格列定义
 */
const getImportColumns = (onCancel) => [
  {
    title: '文件名',
    dataIndex: 'fileName',
    key: 'fileName',
    render: (text) => (
      <Space>
        <FileExcelOutlined />
        <span>{text}</span>
      </Space>
    )
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (status) => <StatusTag status={status} />
  },
  {
    title: '进度',
    key: 'progress',
    render: (_, record) => (
      <Progress
        percent={record.percent || 0}
        size="small"
        status={record.status === 'FAILED' ? 'exception' : undefined}
      />
    )
  },
  {
    title: '成功/失败',
    key: 'counts',
    render: (_, record) => (
      <span>
        <Tag color="success">{record.successCount || 0}</Tag> /
        <Tag color="error">{record.failCount || 0}</Tag>
      </span>
    )
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    render: (text) => text ? new Date(text).toLocaleString() : '-'
  },
  {
    title: '操作',
    key: 'action',
    render: (_, record) => (
      <Space>
        {(record.status === 'PENDING' || record.status === 'RUNNING') && (
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => onCancel(record.id)}
          >
            取消
          </Button>
        )}
      </Space>
    )
  }
];

/**
 * 导入任务表格组件
 */
export const ImportTable = memo(function ImportTable({ jobs, loading, onCancel }) {
  return (
    <Table
      columns={getImportColumns(onCancel)}
      dataSource={jobs}
      rowKey="id"
      pagination={{ pageSize: 10 }}
      loading={loading}
    />
  );
});
