export function mergeRuntimeInput(input) {
  const runtime = input.runtime ?? {}
  const explicit = Object.fromEntries(
    Object.entries(input).filter(([key, value]) => key !== 'runtime' && value !== undefined),
  )
  return { ...runtime, ...explicit, runtime }
}
