import { memo } from 'react'
import CustomersPanelContainer from './customers'

function CustomersPanel(props) {
  return <CustomersPanelContainer {...props} />
}

export default memo(CustomersPanel)
