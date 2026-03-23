import { useState, useCallback } from 'react';

/**
 * API请求Hook封装
 * 提供统一的请求处理、错误处理、加载状态管理
 */
export function useApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const parseResponseBody = useCallback(async (response) => {
    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get('content-type') || '';
    if (!contentType.toLowerCase().includes('application/json')) {
      const text = await response.text();
      return text ? { raw: text } : null;
    }

    const text = await response.text();
    if (!text) {
      return null;
    }

    return JSON.parse(text);
  }, []);

  const getHeaders = useCallback(() => {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
      'X-Tenant-Id': localStorage.getItem('tenantId') || ''
    };
  }, []);

  const request = useCallback(async (url, options = {}) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(url, {
        ...options,
        headers: {
          ...getHeaders(),
          ...options.headers
        }
      });

      const data = await parseResponseBody(response);

      if (!response.ok) {
        throw new Error(data?.message || data?.error || '请求失败');
      }

      return data;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [getHeaders, parseResponseBody]);

  return { request, loading, error, setError };
}

/**
 * 工作流API Hook
 */
export function useWorkflow() {
  const { request, loading, error } = useApi();

  // 获取工作流列表
  const getWorkflows = useCallback(async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    return request(`/api/v2/workflows${query ? '?' + query : ''}`);
  }, [request]);

  // 获取工作流详情
  const getWorkflowDetail = useCallback(async (workflowId) => {
    return request(`/api/v2/workflows/${workflowId}`);
  }, [request]);

  // 创建工作流
  const createWorkflow = useCallback(async (data) => {
    return request('/api/v2/workflows', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 更新工作流
  const updateWorkflow = useCallback(async (workflowId, data) => {
    return request(`/api/v2/workflows/${workflowId}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 删除工作流
  const deleteWorkflow = useCallback(async (workflowId) => {
    return request(`/api/v2/workflows/${workflowId}`, {
      method: 'DELETE'
    });
  }, [request]);

  // 激活工作流
  const activateWorkflow = useCallback(async (workflowId, activatedBy) => {
    return request(`/api/v2/workflows/${workflowId}/activate`, {
      method: 'POST',
      body: JSON.stringify({ activatedBy })
    });
  }, [request]);

  // 停用工作流
  const deactivateWorkflow = useCallback(async (workflowId) => {
    return request(`/api/v2/workflows/${workflowId}/deactivate`, {
      method: 'POST'
    });
  }, [request]);

  // 验证工作流
  const validateWorkflow = useCallback(async (workflowId) => {
    return request(`/api/v2/workflows/${workflowId}/validate`, {
      method: 'POST'
    });
  }, [request]);

  // 启动执行
  const executeWorkflow = useCallback(async (workflowId, payload) => {
    return request(`/api/v2/workflows/${workflowId}/execute`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }, [request]);

  // 获取执行详情
  const getExecutionDetail = useCallback(async (executionId) => {
    return request(`/api/v2/workflows/executions/${executionId}`);
  }, [request]);

  // 获取执行历史
  const getExecutionHistory = useCallback(async (workflowId, params = {}) => {
    const query = new URLSearchParams(params).toString();
    return request(`/api/v2/workflows/${workflowId}/executions${query ? '?' + query : ''}`);
  }, [request]);

  // 取消执行
  const cancelExecution = useCallback(async (executionId, cancelledBy) => {
    return request(`/api/v2/workflows/executions/${executionId}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ cancelledBy })
    });
  }, [request]);

  // 重试执行
  const retryExecution = useCallback(async (executionId) => {
    return request(`/api/v2/workflows/executions/${executionId}/retry`, {
      method: 'POST'
    });
  }, [request]);

  // 获取节点类型
  const getNodeTypes = useCallback(async () => {
    return request('/api/v2/workflows/node-types');
  }, [request]);

  return {
    loading,
    error,
    getWorkflows,
    getWorkflowDetail,
    createWorkflow,
    updateWorkflow,
    deleteWorkflow,
    activateWorkflow,
    deactivateWorkflow,
    validateWorkflow,
    executeWorkflow,
    getExecutionDetail,
    getExecutionHistory,
    cancelExecution,
    retryExecution,
    getNodeTypes
  };
}

/**
 * 协作API Hook
 */
export function useCollaboration() {
  const { request, loading, error } = useApi();

  // 添加评论
  const addComment = useCallback(async (data) => {
    return request('/api/v2/collaboration/comments', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 回复评论
  const replyToComment = useCallback(async (commentId, data) => {
    return request(`/api/v2/collaboration/comments/${commentId}/reply`, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 删除评论
  const deleteComment = useCallback(async (commentId, userId) => {
    return request(`/api/v2/collaboration/comments/${commentId}?userId=${userId}`, {
      method: 'DELETE'
    });
  }, [request]);

  // 点赞评论
  const likeComment = useCallback(async (commentId, userId) => {
    return request(`/api/v2/collaboration/comments/${commentId}/like?userId=${userId}`, {
      method: 'POST'
    });
  }, [request]);

  // 编辑评论
  const editComment = useCallback(async (commentId, data) => {
    return request(`/api/v2/collaboration/comments/${commentId}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 获取评论列表
  const getComments = useCallback(async (entityType, entityId, params = {}) => {
    const query = new URLSearchParams(params).toString();
    return request(`/api/v2/collaboration/entities/${entityType}/${entityId}/comments${query ? '?' + query : ''}`);
  }, [request]);

  // 获取@提及
  const getMentions = useCallback(async (userId, params = {}) => {
    const query = new URLSearchParams({ userId, ...params }).toString();
    return request(`/api/v2/collaboration/mentions?${query}`);
  }, [request]);

  // 获取讨论列表
  const getDiscussions = useCallback(async (userId, limit = 20) => {
    return request(`/api/v2/collaboration/discussions?userId=${userId}&limit=${limit}`);
  }, [request]);

  // 搜索评论
  const searchComments = useCallback(async (keyword, params = {}) => {
    const query = new URLSearchParams({ keyword, ...params }).toString();
    return request(`/api/v2/collaboration/comments/search?${query}`);
  }, [request]);

  // 获取评论统计
  const getCommentStats = useCallback(async (entityType, entityId) => {
    return request(`/api/v2/collaboration/entities/${entityType}/${entityId}/comments/stats`);
  }, [request]);

  return {
    loading,
    error,
    addComment,
    replyToComment,
    deleteComment,
    likeComment,
    editComment,
    getComments,
    getMentions,
    getDiscussions,
    searchComments,
    getCommentStats
  };
}

/**
 * 审批API Hook
 */
export function useApproval() {
  const { request, loading, error } = useApi();

  // 委托审批
  const delegateTask = useCallback(async (taskId, data) => {
    return request(`/api/v2/collaboration/approval/tasks/${taskId}/delegate`, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 加签
  const addSign = useCallback(async (taskId, data) => {
    return request(`/api/v2/collaboration/approval/tasks/${taskId}/add-sign`, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 转交
  const transferTask = useCallback(async (taskId, data) => {
    return request(`/api/v2/collaboration/approval/tasks/${taskId}/transfer`, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 获取委托历史
  const getDelegationHistory = useCallback(async (taskId) => {
    return request(`/api/v2/collaboration/approval/tasks/${taskId}/delegations`);
  }, [request]);

  // 获取转交历史
  const getTransferHistory = useCallback(async (taskId) => {
    return request(`/api/v2/collaboration/approval/tasks/${taskId}/transfers`);
  }, [request]);

  // 撤回委托
  const recallDelegation = useCallback(async (delegationId, userId) => {
    return request(`/api/v2/collaboration/approval/delegations/${delegationId}/recall?userId=${userId}`, {
      method: 'POST'
    });
  }, [request]);

  // 获取可委托用户
  const getDelegatableUsers = useCallback(async (currentUserId) => {
    return request(`/api/v2/collaboration/approval/tasks/delegatable-users?currentUserId=${currentUserId}`);
  }, [request]);

  return {
    loading,
    error,
    delegateTask,
    addSign,
    transferTask,
    getDelegationHistory,
    getTransferHistory,
    recallDelegation,
    getDelegatableUsers
  };
}

/**
 * 团队API Hook
 */
export function useTeam() {
  const { request, loading, error } = useApi();

  // 创建团队
  const createTeam = useCallback(async (data) => {
    return request('/api/v2/collaboration/teams', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 获取团队列表
  const getTeams = useCallback(async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    return request(`/api/v2/collaboration/teams${query ? '?' + query : ''}`);
  }, [request]);

  // 获取团队详情
  const getTeamDetail = useCallback(async (teamId) => {
    return request(`/api/v2/collaboration/teams/${teamId}`);
  }, [request]);

  // 添加成员
  const addTeamMember = useCallback(async (teamId, data) => {
    return request(`/api/v2/collaboration/teams/${teamId}/members`, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 移除成员
  const removeTeamMember = useCallback(async (teamId, userId) => {
    return request(`/api/v2/collaboration/teams/${teamId}/members/${userId}`, {
      method: 'DELETE'
    });
  }, [request]);

  return {
    loading,
    error,
    createTeam,
    getTeams,
    getTeamDetail,
    addTeamMember,
    removeTeamMember
  };
}

/**
 * 数据导入导出API Hook
 */
export function useImportExport() {
  const { request, loading, error } = useApi();

  // 创建导入任务
  const createImportJob = useCallback(async (formData) => {
    const response = await fetch('/api/v2/import/jobs', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
        'X-Tenant-Id': localStorage.getItem('tenantId') || ''
      },
      body: formData
    });
    return response.json();
  }, []);

  // 获取导入任务状态
  const getImportJobStatus = useCallback(async (jobId) => {
    return request(`/api/v2/import/jobs/${jobId}`);
  }, [request]);

  // 取消导入任务
  const cancelImportJob = useCallback(async (jobId) => {
    return request(`/api/v2/import/jobs/${jobId}`, {
      method: 'DELETE'
    });
  }, [request]);

  // 获取导入模板
  const getImportTemplate = useCallback(async (entityType, format = 'xlsx') => {
    const response = await fetch(`/api/v2/import/templates/${entityType}?format=${format}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
        'X-Tenant-Id': localStorage.getItem('tenantId') || ''
      }
    });
    return response.blob();
  }, []);

  // 创建导出任务
  const createExportJob = useCallback(async (data) => {
    return request('/api/v2/export/jobs', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }, [request]);

  // 获取导出任务状态
  const getExportJobStatus = useCallback(async (jobId) => {
    return request(`/api/v2/export/jobs/${jobId}`);
  }, [request]);

  // 下载导出文件
  const downloadExportFile = useCallback(async (jobId) => {
    const response = await fetch(`/api/v2/export/jobs/${jobId}/download`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
        'X-Tenant-Id': localStorage.getItem('tenantId') || ''
      }
    });
    return response.blob();
  }, []);

  return {
    loading,
    error,
    createImportJob,
    getImportJobStatus,
    cancelImportJob,
    getImportTemplate,
    createExportJob,
    getExportJobStatus,
    downloadExportFile
  };
}

export default {
  useApi,
  useWorkflow,
  useCollaboration,
  useApproval,
  useTeam,
  useImportExport
};
