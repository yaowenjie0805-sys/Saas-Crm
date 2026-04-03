export const toQueryString = (params = {}) => new URLSearchParams(params).toString()

export const withQueryString = (path, params = {}) => {
  const query = toQueryString(params)
  return query ? `${path}?${query}` : path
}

export const requestWithJsonBody = (request, path, data, options = {}) => request(path, {
  ...options,
  body: JSON.stringify(data),
})
