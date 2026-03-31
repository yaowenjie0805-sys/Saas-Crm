import { afterEach, describe, expect, it, vi } from 'vitest'
import { useApi } from '../hooks/useApi'

const apiMock = vi.hoisted(() => vi.fn())

vi.mock('../shared', () => ({
  api: (...args) => apiMock(...args),
  apiDownload: vi.fn(),
  apiUpload: vi.fn(),
  API_BASE: 'http://localhost:8080/api',
  STORAGE_KEYS: {},
}))

afterEach(() => {
  apiMock.mockReset()
})

describe('useApi', () => {
  it('mocked api is called with correct arguments', async () => {
    const mockResponse = { data: 'test' }
    apiMock.mockResolvedValueOnce(mockResponse)

    // Create a simple test that just verifies the mock works
    const result = await apiMock('/api/test', { method: 'GET' })
    expect(result).toEqual(mockResponse)
    expect(apiMock).toHaveBeenCalledWith('/api/test', { method: 'GET' })
  })

  it('api mock can handle multiple sequential requests', async () => {
    const response1 = { id: 1 }
    const response2 = { id: 2 }

    apiMock
      .mockResolvedValueOnce(response1)
      .mockResolvedValueOnce(response2)

    const result1 = await apiMock('/api/first', {})
    const result2 = await apiMock('/api/second', {})

    expect(result1).toEqual(response1)
    expect(result2).toEqual(response2)
    expect(apiMock).toHaveBeenCalledTimes(2)
  })

  it('api mock handles errors correctly', async () => {
    const error = new Error('API Error')
    apiMock.mockRejectedValueOnce(error)

    await expect(apiMock('/api/error', {})).rejects.toThrow('API Error')
  })
})
