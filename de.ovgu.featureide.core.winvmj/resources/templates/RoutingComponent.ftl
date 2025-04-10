import { useRoutes } from 'react-router-dom'

<#list features as routename, routefilepath>
<#if routename?has_content && routefilepath?has_content>
import ${routename} from '${routefilepath}'
</#if>
</#list>
import commonRoutes from 'commons/routes.js'
import userRoutes from 'user/routes'
import roleRoutes from 'role/routes'

const GlobalRoutes = () => {
  const router = useRoutes([
    <#list features as routename, _>
    <#if routename?has_content>
    ...${routename},
    </#if>
    </#list>
    ...commonRoutes,
    ...userRoutes,
    ...roleRoutes,
  ])

  return router
}

export default GlobalRoutes
