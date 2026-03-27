import { lazy, Suspense } from 'react'

const AppContainer = lazy(() => import('./AppContainerImpl'))

function App() {
  return (
    <Suspense fallback={<div className="app-bootstrapping">Loading app...</div>}>
      <AppContainer />
    </Suspense>
  )
}

export default App
