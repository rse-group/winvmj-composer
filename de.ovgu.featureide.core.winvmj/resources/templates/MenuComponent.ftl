<#macro traversePrint structure, not_last, indent=0>
<#local pad1>${""?left_pad(indent*6+4)}</#local>
<#if features?seq_contains(structure.name)>
${pad1}{
${pad1}  route: '${structure.route}',
${pad1}  label: '${structure.menulabel}',
${pad1}  subMenus: [
           <#list structure.children as child>
           <@traversePrint child, child?has_next, indent+1/>
           </#list>
${pad1}  ] 
${pad1}}<#if not_last>,</#if>
</#if>
</#macro>
import React from 'react'
import { Link } from 'react-router-dom'

import AuthConsumer from 'commons/auth'
import RootMenu from 'commons/components/RootMenu/RootMenu'
import MenuItem from 'commons/components/MenuItem/MenuItem'
import MenuLink from 'commons/components/MenuLink/MenuLink'

import { FeatureArrow, MenuChildren } from 'commons/components'

class FeatureMainMenu extends React.Component {
  const menus = [
    <#list structures as structure>
      <@traversePrint structure, structure?has_next/>
    </#list>
  ]

  render() {
    const appVariant = this.props.variant
    return (
      <AuthConsumer>
      </AuthConsumer>
    )
  }
}

export default FeatureMainMenu
