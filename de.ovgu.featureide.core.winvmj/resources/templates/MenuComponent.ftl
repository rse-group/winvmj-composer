<#macro traversePrint structure, not_last, indent=0>
<#local pad>${""?left_pad(indent*4+2)}</#local>
<#if features?seq_contains(structure.name)>
${pad}{
${pad}  route: '${structure.route}',
${pad}  label: '${structure.menulabel}',
        <#if structure.children?? >
${pad}  subMenus: [
           <#list structure.children as child>
           <@traversePrint child, child?has_next, indent+1/>
           </#list>
${pad}  ] 
        </#if>
${pad}}<#if not_last>,</#if>
</#if>
</#macro>
const menus = [
<#list structures as structure>
  <@traversePrint structure, structure?has_next/>
</#list>
]

export const settingsMenu = [
  {
    route: '#',
    label: 'Pengaturan',
    subMenus: [
      {
        route: '/settings/appearance',
        label: 'Pengaturan Tampilan',
      },
      {
        route: '/settings/role',
        label: 'Pengaturan Role',
      },
      {
        route: '/settings/user',
        label: 'Pengaturan User',
      },
    ] 
  },
]

export default menus
