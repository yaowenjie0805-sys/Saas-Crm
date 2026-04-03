# Development Conventions

- Allowed direction: `runtime-kernel -> runtime`
- Forbidden direction: `runtime -> runtime-kernel`
- Dependency map reference: [PROJECT_FLOW_MAP](./PROJECT_FLOW_MAP.md)
- Structure reference: [PROJECT_STRUCTURE](./PROJECT_STRUCTURE.md)
- `components/pages/shared` is an internal helper domain. It should expose helpers via `components/pages/shared/index.js` and should not be treated as a route page domain.
- `components/pages/index.js` re-exports must map to existing named exports in each domain `index.js`. Use `npm run check:pages-entry-exports --workspace apps/web`.
