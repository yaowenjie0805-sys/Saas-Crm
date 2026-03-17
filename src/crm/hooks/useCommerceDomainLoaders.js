import { useCallback, useMemo, useState } from 'react'
import { api } from '../shared'

const DEFAULT_PAGE = 1
const DEFAULT_SIZE = 50

export function useCommerceDomainLoaders({
  authToken,
  lang,
  quoteOpportunityFilter,
  orderOpportunityFilter,
}) {
  const [products, setProducts] = useState([])
  const [productsLoading, setProductsLoading] = useState(false)
  const [productsError, setProductsError] = useState('')
  const [productsLastLoadedAt, setProductsLastLoadedAt] = useState(0)
  const [productsStale, setProductsStale] = useState(false)
  const [productsStatus, setProductsStatus] = useState('')
  const [productsCode, setProductsCode] = useState('')
  const [productsName, setProductsName] = useState('')
  const [productsCategory, setProductsCategory] = useState('')

  const [priceBooks, setPriceBooks] = useState([])
  const [priceBooksLoading, setPriceBooksLoading] = useState(false)
  const [priceBooksError, setPriceBooksError] = useState('')
  const [priceBooksLastLoadedAt, setPriceBooksLastLoadedAt] = useState(0)
  const [priceBooksStale, setPriceBooksStale] = useState(false)
  const [priceBooksStatus, setPriceBooksStatus] = useState('')
  const [priceBooksName, setPriceBooksName] = useState('')

  const [quotes, setQuotes] = useState([])
  const [quotesLoading, setQuotesLoading] = useState(false)
  const [quotesError, setQuotesError] = useState('')
  const [quotesLastLoadedAt, setQuotesLastLoadedAt] = useState(0)
  const [quotesStale, setQuotesStale] = useState(false)
  const [quotesStatus, setQuotesStatus] = useState('')
  const [quotesOwner, setQuotesOwner] = useState('')

  const [orders, setOrders] = useState([])
  const [ordersLoading, setOrdersLoading] = useState(false)
  const [ordersError, setOrdersError] = useState('')
  const [ordersLastLoadedAt, setOrdersLastLoadedAt] = useState(0)
  const [ordersStale, setOrdersStale] = useState(false)
  const [ordersStatus, setOrdersStatus] = useState('')
  const [ordersOwner, setOrdersOwner] = useState('')

  const loadProducts = useCallback(async ({ signal } = {}) => {
    if (!authToken) return
    setProductsLoading(true)
    setProductsStale(products.length > 0)
    setProductsError('')
    try {
      const query = new URLSearchParams({ page: String(DEFAULT_PAGE), size: String(DEFAULT_SIZE) })
      if (productsStatus) query.set('status', productsStatus)
      const data = await api('/v1/products?' + query.toString(), { signal }, authToken, lang)
      setProducts(data.items || [])
      setProductsLastLoadedAt(Date.now())
      setProductsStale(false)
    } catch (err) {
      if (err?.name === 'AbortError') return
      setProductsError(err?.requestId ? `${err.message} [${err.requestId}]` : (err?.message || ''))
    } finally {
      setProductsLoading(false)
    }
  }, [authToken, lang, productsStatus, products.length])

  const loadPriceBooks = useCallback(async ({ signal } = {}) => {
    if (!authToken) return
    setPriceBooksLoading(true)
    setPriceBooksStale(priceBooks.length > 0)
    setPriceBooksError('')
    try {
      const query = new URLSearchParams({ page: String(DEFAULT_PAGE), size: String(DEFAULT_SIZE) })
      if (priceBooksStatus) query.set('status', priceBooksStatus)
      const data = await api('/v1/price-books?' + query.toString(), { signal }, authToken, lang)
      setPriceBooks(data.items || [])
      setPriceBooksLastLoadedAt(Date.now())
      setPriceBooksStale(false)
    } catch (err) {
      if (err?.name === 'AbortError') return
      setPriceBooksError(err?.requestId ? `${err.message} [${err.requestId}]` : (err?.message || ''))
    } finally {
      setPriceBooksLoading(false)
    }
  }, [authToken, lang, priceBooksStatus, priceBooks.length])

  const loadQuotes = useCallback(async ({ signal } = {}) => {
    if (!authToken) return
    setQuotesLoading(true)
    setQuotesStale(quotes.length > 0)
    setQuotesError('')
    try {
      const query = new URLSearchParams({ page: String(DEFAULT_PAGE), size: String(DEFAULT_SIZE) })
      if (quoteOpportunityFilter) query.set('opportunityId', quoteOpportunityFilter)
      const data = await api('/v1/quotes?' + query.toString(), { signal }, authToken, lang)
      setQuotes(data.items || [])
      setQuotesLastLoadedAt(Date.now())
      setQuotesStale(false)
    } catch (err) {
      if (err?.name === 'AbortError') return
      setQuotesError(err?.requestId ? `${err.message} [${err.requestId}]` : (err?.message || ''))
    } finally {
      setQuotesLoading(false)
    }
  }, [authToken, lang, quoteOpportunityFilter, quotes.length])

  const loadOrders = useCallback(async ({ signal } = {}) => {
    if (!authToken) return
    setOrdersLoading(true)
    setOrdersStale(orders.length > 0)
    setOrdersError('')
    try {
      const query = new URLSearchParams({ page: String(DEFAULT_PAGE), size: String(DEFAULT_SIZE) })
      if (orderOpportunityFilter) query.set('opportunityId', orderOpportunityFilter)
      const data = await api('/v1/orders?' + query.toString(), { signal }, authToken, lang)
      setOrders(data.items || [])
      setOrdersLastLoadedAt(Date.now())
      setOrdersStale(false)
    } catch (err) {
      if (err?.name === 'AbortError') return
      setOrdersError(err?.requestId ? `${err.message} [${err.requestId}]` : (err?.message || ''))
    } finally {
      setOrdersLoading(false)
    }
  }, [authToken, lang, orderOpportunityFilter, orders.length])

  const signatures = useMemo(() => ({
    products: {
      filters: {
        status: productsStatus,
      },
      pageNo: DEFAULT_PAGE,
      pageSize: DEFAULT_SIZE,
    },
    priceBooks: {
      filters: {
        status: priceBooksStatus,
        name: priceBooksName,
      },
      pageNo: DEFAULT_PAGE,
      pageSize: DEFAULT_SIZE,
    },
    quotes: {
      filters: {
        opportunityId: quoteOpportunityFilter || '',
      },
      pageNo: DEFAULT_PAGE,
      pageSize: DEFAULT_SIZE,
    },
    orders: {
      filters: {
        opportunityId: orderOpportunityFilter || '',
      },
      pageNo: DEFAULT_PAGE,
      pageSize: DEFAULT_SIZE,
    },
  }), [productsStatus, priceBooksStatus, priceBooksName, quoteOpportunityFilter, orderOpportunityFilter])

  const productsView = useMemo(() => ({
    items: products,
    loading: productsLoading,
    error: productsError,
    setError: setProductsError,
    lastLoadedAt: productsLastLoadedAt,
    stale: productsStale,
    status: productsStatus,
    setStatus: setProductsStatus,
    queryCode: productsCode,
    setQueryCode: setProductsCode,
    queryName: productsName,
    setQueryName: setProductsName,
    queryCategory: productsCategory,
    setQueryCategory: setProductsCategory,
  }), [products, productsLoading, productsError, productsLastLoadedAt, productsStale, productsStatus, productsCode, productsName, productsCategory])

  const priceBooksView = useMemo(() => ({
    books: priceBooks,
    loading: priceBooksLoading,
    error: priceBooksError,
    setError: setPriceBooksError,
    lastLoadedAt: priceBooksLastLoadedAt,
    stale: priceBooksStale,
    statusFilter: priceBooksStatus,
    setStatusFilter: setPriceBooksStatus,
    nameFilter: priceBooksName,
    setNameFilter: setPriceBooksName,
  }), [priceBooks, priceBooksLoading, priceBooksError, priceBooksLastLoadedAt, priceBooksStale, priceBooksStatus, priceBooksName])

  const quotesView = useMemo(() => ({
    items: quotes,
    loading: quotesLoading,
    error: quotesError,
    setError: setQuotesError,
    lastLoadedAt: quotesLastLoadedAt,
    stale: quotesStale,
    statusFilter: quotesStatus,
    setStatusFilter: setQuotesStatus,
    ownerFilter: quotesOwner,
    setOwnerFilter: setQuotesOwner,
  }), [quotes, quotesLoading, quotesError, quotesLastLoadedAt, quotesStale, quotesStatus, quotesOwner])

  const ordersView = useMemo(() => ({
    items: orders,
    loading: ordersLoading,
    error: ordersError,
    setError: setOrdersError,
    lastLoadedAt: ordersLastLoadedAt,
    stale: ordersStale,
    statusFilter: ordersStatus,
    setStatusFilter: setOrdersStatus,
    ownerFilter: ordersOwner,
    setOwnerFilter: setOrdersOwner,
  }), [orders, ordersLoading, ordersError, ordersLastLoadedAt, ordersStale, ordersStatus, ordersOwner])

  return {
    loaders: {
      loadProducts,
      loadPriceBooks,
      loadQuotes,
      loadOrders,
    },
    signatures,
    products: productsView,
    priceBooks: priceBooksView,
    quotes: quotesView,
    orders: ordersView,
  }
}
