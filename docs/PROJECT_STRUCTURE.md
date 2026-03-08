# CRM Project Structure

## Root
- `backend/`: Spring Boot backend (REST API, auth, role permissions, reports, exports).
- `src/`: React frontend.
- `scripts/`: local helper scripts (DB init, API smoke, local e2e).
- `docs/`: project documentation.

## Frontend (`src`)
- `App.jsx`: app state orchestration, API calls, auth flow, and data loaders.
- `App.css`: global styles and component-level visual rules.
- `crm/i18n.js`: Chinese/English translation dictionary.
- `crm/shared.js`: shared constants + helpers + API client.
- `crm/components/`
  - `LoginView.jsx`: login/register/SSO UI.
  - `SidebarNav.jsx`: grouped navigation and role info.
  - `MainContent.jsx`: page composition layer (passes grouped props to panels).
  - `BarChartRow.jsx`: chart row UI atom.
  - `pages/`
    - `DashboardPanel.jsx`
    - `PermissionsPanel.jsx`
    - `AuditPanel.jsx`
    - `CustomersPanel.jsx`
    - `PipelinePanel.jsx`
    - `FollowUpsPanel.jsx`
    - `ContactsPanel.jsx`
    - `ContractsPanel.jsx`
    - `PaymentsPanel.jsx`
    - `TasksPanel.jsx`

## Backend (`backend`)
- `src/main/java/com/yao/crm/controller/`: REST controllers by domain.
- `src/main/java/com/yao/crm/entity/`: JPA entities.
- `src/main/java/com/yao/crm/repository/`: Spring Data repositories.
- `src/main/java/com/yao/crm/service/`: domain services and i18n support.
- `src/main/resources/`: application config.
- `src/test/java/`: integration and unit tests.

## Current Frontend Layering
1. `App.jsx`: state + side effects + permissions.
2. `MainContent.jsx`: grouped props routing into page panels.
3. `pages/*.jsx`: concrete business panels (CRUD/report/permissions/audit).
4. `shared.js` + `i18n.js`: reusable infra.

## Start Commands
- Frontend: `npm run dev`
- Backend: `npm run dev:backend`
- API smoke: `npm run test:api`
- Frontend lint/build: `npm run lint` / `npm run build`
