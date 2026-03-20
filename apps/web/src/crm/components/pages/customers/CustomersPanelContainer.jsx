import { memo } from 'react'
import CustomersPanelRuntime from './CustomersPanelRuntime'

function CustomersPanelContainer(props) {
  return <CustomersPanelRuntime {...props} />
}

export default memo(CustomersPanelContainer)
