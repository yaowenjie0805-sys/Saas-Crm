import { useState, useCallback } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Button,
  Space,
  message,
  Divider,
  Tag,
  Tooltip,
  Empty
} from 'antd';
import {
  UserAddOutlined,
  SwapOutlined,
  SendOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';

/**
 * 审批委托组件
 * 支持委托、加签、转交三种操作
 */
export function ApprovalDelegationModal({
  visible,
  task,
  onClose,
  onDelegationSuccess,
  onAddSignSuccess,
  onTransferSuccess
}) {
  const [activeTab, setActiveTab] = useState('delegate');
  const [loading, setLoading] = useState(false);
  const [delegatableUsers, setDelegatableUsers] = useState([]);
  const [form] = Form.useForm();

  // 加载可委托用户
  const loadDelegatableUsers = useCallback(async () => {
    if (!visible) return;
    try {
      const response = await fetch('/api/v2/collaboration/approval/tasks/delegatable-users', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        }
      });
      const data = await response.json();
      if (data.success) {
        setDelegatableUsers(data.users || []);
      }
    } catch (error) {
      console.error('Failed to load delegatable users:', error);
    }
  }, [visible]);

  // 处理委托
  const handleDelegate = async (values) => {
    if (!task) return;
    setLoading(true);
    try {
      const response = await fetch(`/api/v2/collaboration/approval/tasks/${task.id}/delegate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        },
        body: JSON.stringify({
          fromUserId: localStorage.getItem('userId'),
          toUserId: values.toUserId,
          reason: values.reason
        })
      });

      const data = await response.json();
      if (data.success) {
        message.success('审批任务已委托');
        onDelegationSuccess?.(data.result);
        onClose();
      } else {
        message.error(data.message || '委托失败');
      }
    } catch (_error) {
      message.error('委托失败');
    } finally {
      setLoading(false);
    }
  };

  // 处理加签
  const handleAddSign = async (values) => {
    if (!task) return;
    setLoading(true);
    try {
      const response = await fetch(`/api/v2/collaboration/approval/tasks/${task.id}/add-sign`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        },
        body: JSON.stringify({
          approverId: localStorage.getItem('userId'),
          addSignUserId: values.addSignUserId,
          reason: values.reason,
          type: values.type
        })
      });

      const data = await response.json();
      if (data.success) {
        message.success('加签成功');
        onAddSignSuccess?.(data.result);
        onClose();
      } else {
        message.error(data.message || '加签失败');
      }
    } catch (_error) {
      message.error('加签失败');
    } finally {
      setLoading(false);
    }
  };

  // 处理转交
  const handleTransfer = async (values) => {
    if (!task) return;
    setLoading(true);
    try {
      const response = await fetch(`/api/v2/collaboration/approval/tasks/${task.id}/transfer`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        },
        body: JSON.stringify({
          fromUserId: localStorage.getItem('userId'),
          toUserId: values.toUserId,
          reason: values.reason
        })
      });

      const data = await response.json();
      if (data.success) {
        message.success('审批任务已转交');
        onTransferSuccess?.(data.result);
        onClose();
      } else {
        message.error(data.message || '转交失败');
      }
    } catch (_error) {
      message.error('转交失败');
    } finally {
      setLoading(false);
    }
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case 'delegate':
        return (
          <Form
            form={form}
            layout="vertical"
            onFinish={handleDelegate}
            initialValues={{ type: 'AFTER' }}
          >
            <Form.Item
              name="toUserId"
              label="委托给"
              rules={[{ required: true, message: '请选择委托人' }]}
            >
              <Select
                placeholder="选择委托人"
                showSearch
                optionFilterProp="children"
                onFocus={loadDelegatableUsers}
              >
                {delegatableUsers.map(user => (
                  <Select.Option key={user.userId} value={user.userId}>
                    <Space>
                      <span>{user.userName}</span>
                      <Tag color="blue">{user.department}</Tag>
                    </Space>
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="reason"
              label="委托原因"
              rules={[{ required: true, message: '请输入委托原因' }]}
            >
              <Input.TextArea
                rows={3}
                placeholder="请输入委托原因，例如：出差期间无法处理"
              />
            </Form.Item>

            <div style={{ background: '#f6f8fa', padding: 12, borderRadius: 6, marginBottom: 16 }}>
              <ExclamationCircleOutlined style={{ color: '#faad14', marginRight: 8 }} />
              <span style={{ color: '#666' }}>
                委托后您仍可查看审批进度，但实际审批操作由被委托人完成。
              </span>
            </div>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                icon={<SendOutlined />}
                block
              >
                确认委托
              </Button>
            </Form.Item>
          </Form>
        );

      case 'addSign':
        return (
          <Form
            layout="vertical"
            onFinish={handleAddSign}
            initialValues={{ type: 'AFTER' }}
          >
            <Form.Item
              name="type"
              label="加签方式"
              rules={[{ required: true }]}
            >
              <Select>
                <Select.Option value="BEFORE">
                  <Space>
                    <ClockCircleOutlined />
                    <span>前加签</span>
                    <Tooltip title="在当前审批人之前加签">
                      <Tag color="blue">?</Tag>
                    </Tooltip>
                  </Space>
                </Select.Option>
                <Select.Option value="AFTER">
                  <Space>
                    <ClockCircleOutlined />
                    <span>后加签</span>
                    <Tooltip title="在当前审批人之后加签">
                      <Tag color="green">?</Tag>
                    </Tooltip>
                  </Space>
                </Select.Option>
              </Select>
            </Form.Item>

            <Form.Item
              name="addSignUserId"
              label="加签给"
              rules={[{ required: true, message: '请选择加签人' }]}
            >
              <Select
                placeholder="选择加签人"
                showSearch
                optionFilterProp="children"
                onFocus={loadDelegatableUsers}
              >
                {delegatableUsers.map(user => (
                  <Select.Option key={user.userId} value={user.userId}>
                    <Space>
                      <span>{user.userName}</span>
                      <Tag color="blue">{user.department}</Tag>
                    </Space>
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="reason"
              label="加签原因"
              rules={[{ required: true, message: '请输入加签原因' }]}
            >
              <Input.TextArea
                rows={3}
                placeholder="请输入加签原因，例如：需要财务复核"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                icon={<UserAddOutlined />}
                block
              >
                确认加签
              </Button>
            </Form.Item>
          </Form>
        );

      case 'transfer':
        return (
          <Form
            layout="vertical"
            onFinish={handleTransfer}
          >
            <div style={{ background: '#fff1f0', padding: 12, borderRadius: 6, marginBottom: 16, border: '1px solid #ffccc7' }}>
              <ExclamationCircleOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
              <span style={{ color: '#cf1322' }}>
                转交后您将不再有该审批任务的任何权限。
              </span>
            </div>

            <Form.Item
              name="toUserId"
              label="转交给"
              rules={[{ required: true, message: '请选择转收人' }]}
            >
              <Select
                placeholder="选择转收人"
                showSearch
                optionFilterProp="children"
                onFocus={loadDelegatableUsers}
              >
                {delegatableUsers.map(user => (
                  <Select.Option key={user.userId} value={user.userId}>
                    <Space>
                      <span>{user.userName}</span>
                      <Tag color="blue">{user.department}</Tag>
                    </Space>
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="reason"
              label="转交原因"
              rules={[{ required: true, message: '请输入转交原因' }]}
            >
              <Input.TextArea
                rows={3}
                placeholder="请输入转交原因"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                danger
                htmlType="submit"
                loading={loading}
                icon={<SwapOutlined />}
                block
              >
                确认转交
              </Button>
            </Form.Item>
          </Form>
        );

      default:
        return null;
    }
  };

  if (!task) {
    return (
      <Modal
        title="审批操作"
        open={visible}
        onCancel={onClose}
        footer={null}
        width={480}
      >
        <Empty description="未选择审批任务" />
      </Modal>
    );
  }

  return (
    <Modal
      title={
        <Space>
          <span>审批操作</span>
          <Tag color="blue">{task.name || '审批任务'}</Tag>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      footer={null}
      width={480}
      destroyOnClose
    >
      <div style={{ marginBottom: 16 }}>
        <Space style={{ borderBottom: '2px solid #1890ff', paddingBottom: 8 }}>
          <Button
            type={activeTab === 'delegate' ? 'primary' : 'default'}
            onClick={() => setActiveTab('delegate')}
            icon={<SendOutlined />}
          >
            委托
          </Button>
          <Button
            type={activeTab === 'addSign' ? 'primary' : 'default'}
            onClick={() => setActiveTab('addSign')}
            icon={<UserAddOutlined />}
          >
            加签
          </Button>
          <Button
            type={activeTab === 'transfer' ? 'primary' : 'default'}
            danger
            onClick={() => setActiveTab('transfer')}
            icon={<SwapOutlined />}
          >
            转交
          </Button>
        </Space>
      </div>

      <Divider style={{ margin: '12px 0' }} />

      {renderTabContent()}
    </Modal>
  );
}

export default ApprovalDelegationModal;
