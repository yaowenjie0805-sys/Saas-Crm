import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  useApiClient,
  useApprovalApi,
  useImportExportApi,
  useTeamApi,
  useWorkflowApi,
} from '../hooks/useApi'

const apiMock = vi.hoisted(() => vi.fn())
const apiDownloadMock = vi.hoisted(() => vi.fn())
const apiUploadMock = vi.hoisted(() => vi.fn())

vi.mock('../shared', () => ({
  api: (...args) => apiMock(...args),
  apiDownload: (...args) => apiDownloadMock(...args),
  apiUpload: (...args) => apiUploadMock(...args),
  API_BASE: 'http://localhost:8080/api',
  STORAGE_KEYS: {},
}))

const mountedRoots = []

function mountHook(hook) {
  const result = { current: null }
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)

  function Probe() {
    result.current = hook()
    return null
  }

  act(() => {
    root.render(<Probe />)
  })

  mountedRoots.push({ root, container })
  return result
}

afterEach(async () => {
  while (mountedRoots.length) {
    const mounted = mountedRoots.pop()
    await act(async () => {
      mounted.root.unmount()
    })
    mounted.container.remove()
  }
  apiMock.mockReset()
  apiDownloadMock.mockReset()
  apiUploadMock.mockReset()
})

describe('useApiClient', () => {
  it('normalizes /api prefixes and tracks loading state', async () => {
    let resolveRequest
    apiMock.mockImplementationOnce(() => new Promise((resolve) => {
      resolveRequest = resolve
    }))

    const result = mountHook(() => useApiClient())
    expect(result.current.loading).toBe(false)

    let requestPromise
    await act(async () => {
      requestPromise = result.current.request('/api/test', { method: 'GET' })
    })

    expect(apiMock).toHaveBeenCalledWith('/test', { method: 'GET' })
    expect(result.current.loading).toBe(true)

    await act(async () => {
      resolveRequest({ ok: true })
      await requestPromise
    })

    expect(result.current.loading).toBe(false)
    expect(result.current.error).toBe(null)
  })

  it('captures request errors', async () => {
    let rejectRequest
    const error = new Error('API Error')
    apiMock.mockImplementationOnce(() => new Promise((resolve, reject) => {
      rejectRequest = reject
      void resolve
    }))

    const result = mountHook(() => useApiClient())

    let requestPromise
    await act(async () => {
      requestPromise = result.current.request('/api/error', {})
    })

    await act(async () => {
      rejectRequest(error)
      try {
        await requestPromise
      } catch {
        // expected
      }
    })

    expect(result.current.error).toBe('API Error')
  })
})

describe('domain useXxxApi hooks', () => {
  it('useWorkflowApi maps workflow endpoints through the core client', async () => {
    apiMock.mockResolvedValueOnce({ items: [] })

    const result = mountHook(() => useWorkflowApi())
    await act(async () => {
      await result.current.getWorkflows({ page: '2', size: '20' })
    })

    expect(apiMock).toHaveBeenCalledWith('/v2/workflows?page=2&size=20', {})
  })

  it('useWorkflowApi covers execute, cancel, retry, and node-types endpoints', async () => {
    apiMock.mockResolvedValue({})

    const result = mountHook(() => useWorkflowApi())
    await act(async () => {
      await result.current.executeWorkflow('wf-1', { hello: 'world' })
      await result.current.cancelExecution('exec-1', 'manager')
      await result.current.retryExecution('exec-1')
      await result.current.getNodeTypes()
    })

    expect(apiMock).toHaveBeenCalledWith('/v2/workflows/wf-1/execute', {
      method: 'POST',
      body: JSON.stringify({ hello: 'world' }),
    })
    expect(apiMock).toHaveBeenCalledWith('/v2/workflows/executions/exec-1/cancel', {
      method: 'POST',
      body: JSON.stringify({ cancelledBy: 'manager' }),
    })
    expect(apiMock).toHaveBeenCalledWith('/v2/workflows/executions/exec-1/retry', { method: 'POST' })
    expect(apiMock).toHaveBeenCalledWith('/v2/workflows/node-types', {})
  })

  it('useTeamApi maps team endpoints through the core client', async () => {
    apiMock.mockResolvedValueOnce({ items: [] })

    const result = mountHook(() => useTeamApi())
    await act(async () => {
      await result.current.getTeams({ page: '1' })
    })

    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/teams?page=1', {})
  })

  it('useTeamApi covers detail and membership endpoints', async () => {
    apiMock.mockResolvedValue({})

    const result = mountHook(() => useTeamApi())
    await act(async () => {
      await result.current.getTeamDetail('team-1')
      await result.current.createTeam({ name: 'Team A' })
      await result.current.addTeamMember('team-1', { userId: 'u-1' })
      await result.current.removeTeamMember('team-1', 'u-1')
    })

    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/teams/team-1', {})
    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/teams', {
      method: 'POST',
      body: JSON.stringify({ name: 'Team A' }),
    })
    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/teams/team-1/members', {
      method: 'POST',
      body: JSON.stringify({ userId: 'u-1' }),
    })
    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/teams/team-1/members/u-1', {
      method: 'DELETE',
    })
  })

  it('useApprovalApi maps approval endpoints through the core client', async () => {
    apiMock.mockResolvedValueOnce({ ok: true })

    const result = mountHook(() => useApprovalApi())
    await act(async () => {
      await result.current.getDelegatableUsers('user-1')
    })

    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/approval/tasks/delegatable-users?currentUserId=user-1', {})
  })

  it('useApprovalApi covers delegation lifecycle endpoints', async () => {
    apiMock.mockResolvedValue({})

    const result = mountHook(() => useApprovalApi())
    await act(async () => {
      await result.current.delegateTask('task-1', { assignee: 'u-1' })
      await result.current.addSign('task-1', { approver: 'u-2' })
      await result.current.transferTask('task-1', { targetUserId: 'u-3' })
      await result.current.getDelegationHistory('task-1')
      await result.current.getTransferHistory('task-1')
      await result.current.recallDelegation('del-1', 'u-1')
    })

    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/approval/tasks/task-1/delegate', {
      method: 'POST',
      body: JSON.stringify({ assignee: 'u-1' }),
    })
    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/approval/tasks/task-1/add-sign', {
      method: 'POST',
      body: JSON.stringify({ approver: 'u-2' }),
    })
    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/approval/tasks/task-1/transfer', {
      method: 'POST',
      body: JSON.stringify({ targetUserId: 'u-3' }),
    })
    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/approval/tasks/task-1/delegations', {})
    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/approval/tasks/task-1/transfers', {})
    expect(apiMock).toHaveBeenCalledWith('/v2/collaboration/approval/delegations/del-1/recall?userId=u-1', {
      method: 'POST',
    })
  })

  it('useImportExportApi uses upload and download helpers', async () => {
    apiUploadMock.mockResolvedValueOnce({ jobId: 'job-1' })
    apiDownloadMock.mockResolvedValueOnce(new Blob(['csv']))

    const result = mountHook(() => useImportExportApi())
    const formData = new FormData()

    await act(async () => {
      await result.current.createImportJob(formData)
    })

    expect(apiUploadMock).toHaveBeenCalledWith('/api/v2/import/jobs', formData)

    await act(async () => {
      await result.current.getImportTemplate('lead')
    })

    expect(apiDownloadMock).toHaveBeenCalledWith('/api/v2/import/templates/lead?format=xlsx', 'import_template_lead.xlsx')
  })

  it('useImportExportApi covers job status and export download helpers', async () => {
    apiMock.mockResolvedValue({})
    apiDownloadMock.mockResolvedValueOnce(new Blob(['zip']))

    const result = mountHook(() => useImportExportApi())
    await act(async () => {
      await result.current.getImportJobStatus('job-1')
      await result.current.cancelImportJob('job-1')
      await result.current.createExportJob({ scope: 'all' })
      await result.current.getExportJobStatus('job-2')
      await result.current.downloadExportFile('job-2', 'export-2.zip')
    })

    expect(apiMock).toHaveBeenCalledWith('/v2/import/jobs/job-1', {})
    expect(apiMock).toHaveBeenCalledWith('/v2/import/jobs/job-1', { method: 'DELETE' })
    expect(apiMock).toHaveBeenCalledWith('/v2/export/jobs', {
      method: 'POST',
      body: JSON.stringify({ scope: 'all' }),
    })
    expect(apiMock).toHaveBeenCalledWith('/v2/export/jobs/job-2', {})
    expect(apiDownloadMock).toHaveBeenCalledWith('/api/v2/export/jobs/job-2/download', 'export-2.zip')
  })
})
