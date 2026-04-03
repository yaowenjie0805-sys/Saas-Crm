import { lazy } from 'react'

function lazyNamed(loader, key) {
  return lazy(() => loader().then((module) => ({ default: module[key] })))
}

export default lazyNamed
