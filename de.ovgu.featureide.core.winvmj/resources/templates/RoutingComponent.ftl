import { useRoutes } from 'react-router-dom'

<#list features as feature>
<#if feature.routename?has_content && feature.routefilepath?has_content>
import ${feature.routename} from '${feature.routefilepath}'
</#if>
</#list>
import commonRoutes from 'commons/routes.js'
import staticPageRoutes from 'staticPage/routes'
import settingsRoutes from 'settings/routes.js'

const GlobalRoutes = () => {
  const router = useRoutes([
  	<#list features as feature>
  	...${feature.routename},
  	</#list>
    ...commonRoutes,
    ...staticPageRoutes,
    ...settingsRoutes,
  ])

  return router
}

export default GlobalRoutes
