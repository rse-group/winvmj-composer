import { useRoutes } from 'react-router-dom'

<#list features as routename, routefilepath>
<#if routename?has_content && routefilepath?has_content>
import ${routename} from '${routefilepath}'
</#if>
</#list>
import commonRoutes from 'commons/routes.js'

const GlobalRoutes = () => {
  const router = useRoutes([
    <#list features as routename, _>
    <#if routename?has_content>
    ...${routename},
    </#if>
    </#list>
    ...commonRoutes,
  ])

  return router
}

export default GlobalRoutes
