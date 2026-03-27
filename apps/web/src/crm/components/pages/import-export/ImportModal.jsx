import { memo } from 'react';
import { Modal, Select, Space, Button, Divider, Upload, Alert } from 'antd';
import { FileExcelOutlined, FileTextOutlined, InboxOutlined } from '@ant-design/icons';

/**
 * 导入弹窗组件
 */
export const ImportModal = memo(function ImportModal({
  visible,
  entityType,
  uploading,
  onClose,
  onEntityTypeChange,
  onDownloadTemplate,
  onUpload
}) {
  return (
    <Modal
      title="新建导入任务"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        <div>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
            选择导入类型
          </label>
          <Select
            value={entityType}
            onChange={onEntityTypeChange}
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
              onClick={() => onDownloadTemplate('xlsx')}
            >
              Excel模板
            </Button>
            <Button
              icon={<FileTextOutlined />}
              onClick={() => onDownloadTemplate('csv')}
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
            beforeUpload={onUpload}
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
  );
});
