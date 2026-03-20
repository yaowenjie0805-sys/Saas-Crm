import { create } from 'zustand'
import { FILTERS_KEY, LANG_KEY } from '../shared'

const readStoredAuth = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem('crm_auth') || 'null')
    if (!parsed || typeof parsed !== 'object') return null
    if (!parsed.sessionActive) return null
    return { ...parsed, token: 'COOKIE_SESSION' }
  } catch {
    return null
  }
}

const readStoredFilters = () => {
  try {
    return JSON.parse(localStorage.getItem(FILTERS_KEY) || '{}')
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

const setSafeStorage = (key, value) => {
  try {
    localStorage.setItem(key, value)
  } catch {
    // ignore storage write failures
  }
}

const removeSafeStorage = (key) => {
  try {
    localStorage.removeItem(key)
  } catch {
    // ignore storage remove failures
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
    lang: localStorage.getItem(LANG_KEY) || 'en',
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
  },

  setLang: (lang) => {
    set((state) => ({ auth: { ...state.auth, lang } }))
    setSafeStorage(LANG_KEY, lang)
  },
  setAuth: (session) => {
    set((state) => ({ auth: { ...state.auth, session } }))
    if (!session) {
      removeSafeStorage('crm_auth')
      return
    }
    setSafeStorage('crm_auth', JSON.stringify(session))
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
  loadStarted: (sliceName, requestId = '') =>
    get().setLoadingState(sliceName, { status: 'loading', error: '', requestId }),
  loadSucceeded: (sliceName, requestId = '') =>
    get().setLoadingState(sliceName, { status: 'success', error: '', requestId }),
  loadFailed: (sliceName, error = '', requestId = '') =>
    get().setLoadingState(sliceName, { status: 'error', error: String(error || ''), requestId }),
  resetLoading: (sliceName) =>
    get().setLoadingState(sliceName, { status: 'idle', error: '', requestId: '' }),
  readPageSize: (key, fallback = 8) => {
    const raw = Number(localStorage.getItem(key) || fallback)
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
export const selectLoadingSlice = (state) => state.loading

export const selectLang = (state) => state.auth.lang
export const selectAuth = (state) => state.auth.session
export const selectPersistedFilters = (state) => state.filters.persisted
