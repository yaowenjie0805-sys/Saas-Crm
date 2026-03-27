import { memo } from 'react';
import { Table, Space, Button } from 'antd';
import { FileTextOutlined, DownloadOutlined } from '@ant-design/icons';
import { StatusTag } from './StatusTag';

/**
 * 导出任务表格列定义
 */
const getExportColumns = (onDownload) => [
  {
    title: '文件名',
    dataIndex: 'fileName',
    key: 'fileName',
    render: (text) => (
      <Space>
        <FileTextOutlined />
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
    title: '数据量',
    dataIndex: 'totalRows',
    key: 'totalRows',
    render: (text) => `${text || 0} 条`
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
        {record.status === 'COMPLETED' && (
          <Button
            size="small"
            type="primary"
            icon={<DownloadOutlined />}
            onClick={() => onDownload(record.id)}
          >
            下载
          </Button>
        )}
      </Space>
    )
  }
];

/**
 * 导出任务表格组件
 */
export const ExportTable = memo(function ExportTable({ jobs, loading, onDownload }) {
  return (
    <Table
      columns={getExportColumns(onDownload)}
      dataSource={jobs}
      rowKey="id"
      pagination={{ pageSize: 10 }}
      loading={loading}
    />
  );
});
