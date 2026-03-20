import { useEffect, useMemo, useState } from 'react'
import { shallow } from 'zustand/shallow'
import { useAppStore } from '../../store/appStore'

const toSetterName = (field) => `set${String(field || '').charAt(0).toUpperCase()}${String(field || '').slice(1)}`

export function useRuntimeSectionFields(sliceName, sectionName, defaults) {
  const initSliceField = useAppStore((state) => state.initSliceField)
  const setSliceField = useAppStore((state) => state.setSliceField)
  const section = useAppStore((state) => state?.[sliceName]?.[sectionName] || {}, shallow)
  const [initialMap] = useState(() => {
    const next = {}
    const initialKeys = Object.keys(defaults || {})
    initialKeys.forEach((field) => {
      const raw = defaults[field]
      next[field] = typeof raw === 'function' ? raw() : raw
    })
    return next
  })
  const keys = useMemo(() => Object.keys(defaults || {}), [defaults])

  useEffect(() => {
    keys.forEach((field) => {
      initSliceField(sliceName, sectionName, field, initialMap[field])
    })
  }, [initSliceField, sliceName, sectionName, keys, initialMap])

  const setters = useMemo(() => {
    const next = {}
    keys.forEach((field) => {
      next[toSetterName(field)] = (nextValueOrUpdater) => {
        setSliceField(sliceName, sectionName, field, nextValueOrUpdater)
      }
    })
    return next
  }, [keys, setSliceField, sliceName, sectionName])

  return useMemo(() => {
    const next = {}
    keys.forEach((field) => {
      const value = Object.prototype.hasOwnProperty.call(section, field)
        ? section[field]
        : initialMap[field]
      next[field] = value
      next[toSetterName(field)] = setters[toSetterName(field)]
    })
    return next
  }, [keys, section, initialMap, setters])
}
