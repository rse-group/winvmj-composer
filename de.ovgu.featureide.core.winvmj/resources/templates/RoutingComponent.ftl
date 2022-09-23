import { useRoutes } from 'react-router-dom'

<#list features as feature>
import ${feature.routename} from '${feature.routefilepath}'
</#list>
import commonRoutes from 'commons/routes.js'

const GlobalRoutes = () => {
  const router = useRoutes([
  	<#list features as feature>
  	...${feature.routename},
  	</#list>
    ...commonRoutes,
  ])

  return router
}

export default GlobalRoutes
