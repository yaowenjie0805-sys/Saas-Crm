import { create } from 'zustand'
import { FILTERS_KEY, LANG_KEY } from '../shared'

const readStoredAuth = () => {
  try {
    const parsed = JSON.parse(safeGetStorage('crm_auth') || 'null')
    if (!parsed || typeof parsed !== 'object') return null
    if (!parsed.sessionActive) return null
    return { ...parsed, token: 'COOKIE_SESSION' }
  } catch {
    return null
  }
}

const readStoredFilters = () => {
  try {
    return JSON.parse(safeGetStorage(FILTERS_KEY) || '{}')
  } catch {
    return {}
  }
}

const createLoadingState = () => ({
  status: 'idle',
  error: '',
  lastUpdatedAt: 0,
  requestId: '',
})

const nowTs = () => Date.now()

const safeGetStorage = (key) => {
  try {
    if (typeof window === 'undefined' || !window.localStorage) return ''
    return localStorage.getItem(key) || ''
  } catch {
    return ''
  }
}

const setSafeStorage = (key, value) => {
  try {
    localStorage.setItem(key, value)
  } catch {
    // ignore storage write failures
  }
}

const createDomainSetter = (set, sliceName) => (nextValue) =>
  set((state) => {
    const current = state[sliceName] || {}
    const patch = nextValue || {}
    const patchKeys = Object.keys(patch)
    if (!patchKeys.length) return state
    const changed = patchKeys.some((key) => !Object.is(current[key], patch[key]))
    if (!changed) return state
    return { [sliceName]: { ...current, ...patch } }
  })

export const useAppStore = create((set, get) => ({
  auth: {
    lang: safeGetStorage(LANG_KEY) || 'en',
    session: readStoredAuth(),
    data: {
      baseModel: {},
    },
    forms: {},
    query: {},
    ui: {},
  },
  ui: {
    loginError: '',
    error: '',
    runtime: {},
  },
  filters: {
    persisted: readStoredFilters(),
  },
  customerDomain: { data: {}, forms: {}, query: {}, ui: {} },
  commerceDomain: { data: {}, forms: {}, query: {}, ui: {} },
  governanceDomain: { data: {}, forms: {}, query: {}, ui: {} },
  approvalDomain: { data: {}, forms: {}, query: {}, ui: {} },
  reportingDomain: { data: {}, forms: {}, query: {}, ui: {} },
  leadImportDomain: { data: {}, forms: {}, query: {}, ui: {} },
  perfDomain: { data: {}, forms: {}, query: {}, ui: {} },
  searchDomain: { data: {}, forms: {}, query: {}, ui: {} },
  loading: {
    auth: createLoadingState(),
    ui: createLoadingState(),
    filters: createLoadingState(),
    customerDomain: createLoadingState(),
    commerceDomain: createLoadingState(),
    governanceDomain: createLoadingState(),
    approvalDomain: createLoadingState(),
    reportingDomain: createLoadingState(),
    leadImportDomain: createLoadingState(),
    perfDomain: createLoadingState(),
    searchDomain: createLoadingState(),
  },

  setLang: (lang) => {
    const nextLang = String(lang || 'en')
    if ((get().auth?.lang || 'en') === nextLang) return
    set((state) => ({ auth: { ...state.auth, lang: nextLang } }))
    setSafeStorage(LANG_KEY, nextLang)
  },
  setAuth: (session) => {
    set((state) => ({ auth: { ...state.auth, session } }))
  },
  setUiError: (error) => set((state) => ({ ui: { ...state.ui, error } })),
  setUiLoginError: (loginError) => set((state) => ({ ui: { ...state.ui, loginError } })),
  setAuthBaseModel: (baseModel) =>
    set((state) => {
      const currentBaseModel = state.auth?.data?.baseModel || {}
      const nextBaseModel = baseModel || {}
      const currentKeys = Object.keys(currentBaseModel)
      const nextKeys = Object.keys(nextBaseModel)
      if (
        currentKeys.length === nextKeys.length
        && nextKeys.every((key) => Object.is(currentBaseModel[key], nextBaseModel[key]))
      ) {
        return state
      }
      return { auth: { ...state.auth, data: { ...(state.auth?.data || {}), baseModel: nextBaseModel } } }
    }),
  setPersistedFilters: (persisted) => {
    set((state) => ({ filters: { ...state.filters, persisted } }))
    setSafeStorage(FILTERS_KEY, JSON.stringify(persisted || {}))
  },
  setCustomerDomainState: createDomainSetter(set, 'customerDomain'),
  setCommerceDomainState: createDomainSetter(set, 'commerceDomain'),
  setGovernanceDomainState: createDomainSetter(set, 'governanceDomain'),
  setApprovalDomainState: createDomainSetter(set, 'approvalDomain'),
  setReportingDomainState: createDomainSetter(set, 'reportingDomain'),
  setLeadImportDomainState: createDomainSetter(set, 'leadImportDomain'),
  setPerfDomainState: createDomainSetter(set, 'perfDomain'),
  setSearchDomainState: createDomainSetter(set, 'searchDomain'),
  resetDomainSlices: () =>
    set(() => ({
      customerDomain: { data: {}, forms: {}, query: {}, ui: {} },
      commerceDomain: { data: {}, forms: {}, query: {}, ui: {} },
      governanceDomain: { data: {}, forms: {}, query: {}, ui: {} },
      approvalDomain: { data: {}, forms: {}, query: {}, ui: {} },
      reportingDomain: { data: {}, forms: {}, query: {}, ui: {} },
      leadImportDomain: { data: {}, forms: {}, query: {}, ui: {} },
      perfDomain: { data: {}, forms: {}, query: {}, ui: {} },
      searchDomain: { data: {}, forms: {}, query: {}, ui: {} },
    })),
  initSliceField: (sliceName, section, field, initialValue) => {
    set((state) => {
      const slice = state[sliceName] || {}
      const sectionObj = slice[section] || {}
      if (Object.prototype.hasOwnProperty.call(sectionObj, field)) return state
      const nextValue = typeof initialValue === 'function' ? initialValue() : initialValue
      return {
        [sliceName]: {
          ...slice,
          [section]: {
            ...sectionObj,
            [field]: nextValue,
          },
        },
      }
    })
  },
  setSliceField: (sliceName, section, field, nextValueOrUpdater) => {
    set((state) => {
      const slice = state[sliceName] || {}
      const sectionObj = slice[section] || {}
      const prev = sectionObj[field]
      const nextValue = typeof nextValueOrUpdater === 'function'
        ? nextValueOrUpdater(prev)
        : nextValueOrUpdater
      if (Object.is(prev, nextValue)) return state
      return {
        [sliceName]: {
          ...slice,
          [section]: {
            ...sectionObj,
            [field]: nextValue,
          },
        },
      }
    })
  },
  setLoadingState: (sliceName, patch) => {
    set((state) => ({
      loading: {
        ...state.loading,
        [sliceName]: {
          ...(state.loading?.[sliceName] || createLoadingState()),
          ...(patch || {}),
          lastUpdatedAt: nowTs(),
        },
      },
    }))
  },
  setLoadingStateIfCurrent: (sliceName, patch, expectedRequestId = '') => {
    set((state) => {
      const current = state.loading?.[sliceName] || createLoadingState()
      const expected = String(expectedRequestId || '')
      const currentRequestId = String(current.requestId || '')
      if (expected && currentRequestId && currentRequestId !== expected) {
        return state
      }
      return {
        loading: {
          ...state.loading,
          [sliceName]: {
            ...current,
            ...(patch || {}),
            lastUpdatedAt: nowTs(),
          },
        },
      }
    })
  },
  loadStarted: (sliceName, requestId = '') =>
    get().setLoadingState(sliceName, { status: 'loading', error: '', requestId }),
  loadSucceeded: (sliceName, requestId = '') =>
    get().setLoadingStateIfCurrent(sliceName, { status: 'success', error: '', requestId }, requestId),
  loadFailed: (sliceName, error = '', requestId = '') =>
    get().setLoadingStateIfCurrent(sliceName, { status: 'error', error: String(error || ''), requestId }, requestId),
  resetLoading: (sliceName) =>
    get().setLoadingState(sliceName, { status: 'idle', error: '', requestId: '' }),
  readPageSize: (key, fallback = 8) => {
    const raw = Number(safeGetStorage(key) || fallback)
    if (!Number.isFinite(raw)) return fallback
    return Math.min(Math.max(Math.floor(raw), 5), 50)
  },
}))

export const selectAuthSlice = (state) => state.auth
export const selectUiSlice = (state) => state.ui
export const selectFiltersSlice = (state) => state.filters
export const selectCustomerDomainSlice = (state) => state.customerDomain
export const selectCommerceDomainSlice = (state) => state.commerceDomain
export const selectGovernanceDomainSlice = (state) => state.governanceDomain
export const selectApprovalDomainSlice = (state) => state.approvalDomain
export const selectReportingDomainSlice = (state) => state.reportingDomain
export const selectLeadImportDomainSlice = (state) => state.leadImportDomain
export const selectPerfDomainSlice = (state) => state.perfDomain
export const selectSearchDomainSlice = (state) => state.searchDomain
export const selectLoadingSlice = (state) => state.loading

export const selectLang = (state) => state.auth.lang
export const selectAuth = (state) => state.auth.session
export const selectPersistedFilters = (state) => state.filters.persisted
