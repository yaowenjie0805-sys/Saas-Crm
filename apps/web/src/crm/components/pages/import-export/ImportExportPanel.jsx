import { memo, useState, useEffect, useCallback } from 'react';
import { Card, Button, Space, Empty, message, Modal } from 'antd';
import { UploadOutlined, DownloadOutlined, InboxOutlined } from '@ant-design/icons';
import { api } from '../../../shared';
import { ImportTable } from './ImportTable';
import { ExportTable } from './ExportTable';
import { ImportModal } from './ImportModal';

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

  // 加载导入任务
  const loadImportJobs = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api('/v2/import/jobs');
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
      const data = await api('/v2/export/jobs');
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
      const data = await api('/v2/import/jobs', {
        method: 'POST',
        body: formData
      });

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

    return false;
  };

  // 下载模板
  const handleDownloadTemplate = async (format) => {
    try {
      const blob = await api(`/v2/import/template?entityType=${selectedEntityType}&format=${format}`, {
        responseType: 'blob'
      });
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
          await api(`/v2/import/jobs/${jobId}`, { method: 'DELETE' });
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
      const blob = await api(`/v2/export/jobs/${jobId}/download`, {
        responseType: 'blob'
      });
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
            <ImportTable
              jobs={importJobs}
              loading={loading}
              onCancel={handleCancelImport}
            />
          </>
        ) : (
          <ExportTable
            jobs={exportJobs}
            loading={loading}
            onDownload={handleDownloadExport}
          />
        )}
      </Card>

      <ImportModal
        visible={importModalVisible}
        entityType={selectedEntityType}
        uploading={uploading}
        onClose={() => setImportModalVisible(false)}
        onEntityTypeChange={setSelectedEntityType}
        onDownloadTemplate={handleDownloadTemplate}
        onUpload={handleUpload}
      />
    </div>
  );
}

export default memo(ImportExportPanel);
