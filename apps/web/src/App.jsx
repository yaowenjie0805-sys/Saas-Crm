import { lazy, Suspense } from 'react'
import ErrorBoundary from './crm/components/ErrorBoundary'

const AppContainer = lazy(() => import('./AppContainerImpl'))

function App() {
  return (
    <ErrorBoundary showDetails={import.meta.env.DEV}>
      <Suspense fallback={<div className="app-bootstrapping">Loading app...</div>}>
        <AppContainer />
      </Suspense>
    </ErrorBoundary>
  )
}

export default App
