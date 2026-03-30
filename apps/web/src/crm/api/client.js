import { api, apiDownload, apiUpload, STORAGE_KEYS } from '../shared'

// Thin compatibility layer to decouple hooks from shared.js directly.
export const apiClient = {
  request: api,
  download: apiDownload,
  upload: apiUpload,
  storageKeys: STORAGE_KEYS,
}

export const requestApi = (path, options = {}, token, lang = 'en') =>
  apiClient.request(path, options, token, lang)

export const downloadApi = (path, filename = 'download') =>
  apiClient.download(path, filename)

export const uploadApi = (path, formData, options = {}) =>
  apiClient.upload(path, formData, options)

export const API_STORAGE_KEYS = apiClient.storageKeys
