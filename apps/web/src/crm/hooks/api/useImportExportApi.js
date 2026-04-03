import { useCallback } from 'react'
import { apiDownload, apiUpload } from '../../shared'
import { useApiClient } from './useApiClient'
import { requestWithJsonBody, withQueryString } from './utils'

export function useImportExportApi() {
  const { request, loading, error } = useApiClient()

  const createImportJob = useCallback(async (formData) => apiUpload('/api/v2/import/jobs', formData), [])
  const getImportJobStatus = useCallback(async (jobId) => request(`/api/v2/import/jobs/${jobId}`), [request])
  const cancelImportJob = useCallback(async (jobId) => request(`/api/v2/import/jobs/${jobId}`, {
    method: 'DELETE',
  }), [request])
  const getImportTemplate = useCallback(async (entityType, format = 'xlsx') => {
    const path = withQueryString(`/api/v2/import/templates/${entityType}`, { format })
    const filename = `import_template_${entityType}.${format}`
    return apiDownload(path, filename)
  }, [])
  const createExportJob = useCallback((data) => requestWithJsonBody(request, '/api/v2/export/jobs', data, {
    method: 'POST',
  }), [request])
  const getExportJobStatus = useCallback(async (jobId) => request(`/api/v2/export/jobs/${jobId}`), [request])
  const downloadExportFile = useCallback(async (jobId, filename = 'export') => apiDownload(`/api/v2/export/jobs/${jobId}/download`, filename), [])

  return {
    loading,
    error,
    createImportJob,
    getImportJobStatus,
    cancelImportJob,
    getImportTemplate,
    createExportJob,
    getExportJobStatus,
    downloadExportFile,
  }
}
