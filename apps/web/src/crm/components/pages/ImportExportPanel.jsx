import { memo, useState, useEffect, useCallback } from 'react';
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Modal,
  Upload,
  Select,
  Progress,
  message,
  Empty,
  Spin,
  Timeline,
  Tooltip,
  Divider,
  Alert
} from 'antd';
import {
  UploadOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  FileTextOutlined,
  DeleteOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  InboxOutlined
} from '@ant-design/icons';
import { useImportExport } from '../../hooks/useApi';

/**
 * 导入导出管理面板
 */
export function ImportExportPanel() {
  const [activeTab, setActiveTab] = useState('import');
  const [importJobs, setImportJobs] = useState([]);
  const [exportJobs, setExportJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [importModalVisible, setImportModalVisible] = useState(false);
  const [selectedEntityType, setSelectedEntityType] = useState('Customer');

  const {
    cancelImportJob,
    getImportTemplate,
    downloadExportFile
  } = useImportExport();

  // 加载导入任务
  const loadImportJobs = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/v2/import/jobs', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        }
      });
      const data = await response.json();
      if (data.items) {
        setImportJobs(data.items);
      }
    } catch (_error) {
      console.error('Failed to load import jobs:', _error);
    } finally {
      setLoading(false);
    }
  }, []);

  // 加载导出任务
  const loadExportJobs = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/v2/export/jobs', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        }
      });
      const data = await response.json();
      if (data.items) {
        setExportJobs(data.items);
      }
    } catch (_error) {
      console.error('Failed to load export jobs:', _error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === 'import') {
      loadImportJobs();
    } else {
      loadExportJobs();
    }
  }, [activeTab, loadImportJobs, loadExportJobs]);

  // 处理文件上传
  const handleUpload = async (file) => {
    if (!file) return false;

    const formData = new FormData();
    formData.append('file', file);
    formData.append('entityType', selectedEntityType);
    formData.append('operator', localStorage.getItem('userId'));

    setUploading(true);
    try {
      const response = await fetch('/api/v2/import/jobs', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        },
        body: formData
      });

      const data = await response.json();
      if (data.success || data.jobId) {
        message.success('导入任务已创建');
        setImportModalVisible(false);
        loadImportJobs();
      } else {
        message.error(data.message || '导入失败');
      }
    } catch (_error) {
      message.error('导入失败');
    } finally {
      setUploading(false);
    }

    return false; // 阻止默认上传
  };

  // 下载模板
  const handleDownloadTemplate = async (format) => {
    try {
      const blob = await getImportTemplate(selectedEntityType, format);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${selectedEntityType}_导入模板.${format}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      message.success('模板下载成功');
    } catch (_error) {
      message.error('模板下载失败');
    }
  };

  // 取消导入任务
  const handleCancelImport = async (jobId) => {
    Modal.confirm({
      title: '确认取消',
      content: '确定要取消这个导入任务吗？',
      onOk: async () => {
        try {
          await cancelImportJob(jobId);
          message.success('任务已取消');
          loadImportJobs();
        } catch (_error) {
          message.error('取消失败');
        }
      }
    });
  };

  // 下载导出文件
  const handleDownloadExport = async (jobId) => {
    try {
      const blob = await downloadExportFile(jobId);
      if (blob) {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `导出文件_${jobId}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        message.success('文件下载成功');
      }
    } catch (_error) {
      message.error('文件下载失败');
    }
  };

  // 获取状态配置
  const getStatusConfig = (status) => {
    const configs = {
      PENDING: { color: 'default', icon: <ClockCircleOutlined />, text: '等待中' },
      RUNNING: { color: 'processing', icon: <SyncOutlined spin />, text: '处理中' },
      COMPLETED: { color: 'success', icon: <CheckCircleOutlined />, text: '已完成' },
      FAILED: { color: 'error', icon: <CloseCircleOutlined />, text: '失败' },
      PARTIAL_SUCCESS: { color: 'warning', icon: <Alert />, text: '部分成功' },
      CANCELLED: { color: 'default', icon: <DeleteOutlined />, text: '已取消' }
    };
    return configs[status] || configs.PENDING;
  };

  // 导入任务列
  const importColumns = [
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
      render: (status) => {
        const config = getStatusConfig(status);
        return (
          <Tag color={config.color} icon={config.icon}>
            {config.text}
          </Tag>
        );
      }
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
          {record.status === 'PENDING' || record.status === 'RUNNING' ? (
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleCancelImport(record.id)}
            >
              取消
            </Button>
          ) : null}
        </Space>
      )
    }
  ];

  // 导出任务列
  const exportColumns = [
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
      render: (status) => {
        const config = getStatusConfig(status);
        return (
          <Tag color={config.color} icon={config.icon}>
            {config.text}
          </Tag>
        );
      }
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
              onClick={() => handleDownloadExport(record.id)}
            >
              下载
            </Button>
          )}
        </Space>
      )
    }
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={
          <Space>
            <InboxOutlined />
            <span>数据导入导出管理</span>
          </Space>
        }
        extra={
          <Space>
            <Button
              type={activeTab === 'import' ? 'primary' : 'default'}
              onClick={() => setActiveTab('import')}
              icon={<UploadOutlined />}
            >
              导入
            </Button>
            <Button
              type={activeTab === 'export' ? 'primary' : 'default'}
              onClick={() => setActiveTab('export')}
              icon={<DownloadOutlined />}
            >
              导出
            </Button>
          </Space>
        }
      >
        {activeTab === 'import' ? (
          <>
            <div style={{ marginBottom: 16 }}>
              <Button
                type="primary"
                icon={<UploadOutlined />}
                onClick={() => setImportModalVisible(true)}
              >
                新建导入任务
              </Button>
            </div>

            <Spin spinning={loading}>
              <Table
                columns={importColumns}
                dataSource={importJobs}
                rowKey="id"
                pagination={{ pageSize: 10 }}
                locale={{ emptyText: <Empty description="暂无导入任务" /> }}
              />
            </Spin>
          </>
        ) : (
          <Spin spinning={loading}>
            <Table
              columns={exportColumns}
              dataSource={exportJobs}
              rowKey="id"
              pagination={{ pageSize: 10 }}
              locale={{ emptyText: <Empty description="暂无导出任务" /> }}
            />
          </Spin>
        )}
      </Card>

      {/* 导入Modal */}
      <Modal
        title="新建导入任务"
        open={importModalVisible}
        onCancel={() => setImportModalVisible(false)}
        footer={null}
        width={600}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <div>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
              选择导入类型
            </label>
            <Select
              value={selectedEntityType}
              onChange={setSelectedEntityType}
              style={{ width: '100%' }}
              options={[
                { value: 'Customer', label: '客户' },
                { value: 'Contact', label: '联系人' },
                { value: 'Lead', label: '线索' },
                { value: 'Product', label: '产品' }
              ]}
            />
          </div>

          <Divider style={{ margin: '8px 0' }} />

          <div>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
              下载模板
            </label>
            <Space>
              <Button
                icon={<FileExcelOutlined />}
                onClick={() => handleDownloadTemplate('xlsx')}
              >
                Excel模板
              </Button>
              <Button
                icon={<FileTextOutlined />}
                onClick={() => handleDownloadTemplate('csv')}
              >
                CSV模板
              </Button>
            </Space>
          </div>

          <Divider style={{ margin: '8px 0' }} />

          <div>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
              上传文件
            </label>
            <Upload.Dragger
              accept=".xlsx,.xls,.csv,.json"
              beforeUpload={handleUpload}
              showUploadList={false}
              disabled={uploading}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">点击或拖拽文件到此区域</p>
              <p className="ant-upload-hint">
                支持 .xlsx, .xls, .csv, .json 格式，单文件不超过20MB
              </p>
            </Upload.Dragger>
            {uploading && (
              <Alert
                type="info"
                message="正在处理..."
                style={{ marginTop: 16 }}
                showIcon
              />
            )}
          </div>

          <Alert
            type="warning"
            message={
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                <li>请确保文件格式正确，参考模板填写数据</li>
                <li>必填字段不能为空</li>
                <li>重复数据将自动跳过</li>
                <li>导入完成后可在历史记录中查看失败行</li>
              </ul>
            }
          />
        </Space>
      </Modal>
    </div>
  );
}

export default memo(ImportExportPanel);
