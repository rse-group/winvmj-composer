<#macro traversePrint structure, not_last, indent=0>
<#local pad>${""?left_pad(indent*4+2)}</#local>
<#if features?seq_contains(structure.name)>
${pad}{
${pad}  route: '${structure.route}',
${pad}  label: '${structure.menulabel}',
${pad}  subMenus: [
           <#list structure.children as child>
           <@traversePrint child, child?has_next, indent+1/>
           </#list>
${pad}  ] 
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
        subMenus: [] 
      },
      {
        route: '/settings/role',
        label: 'Pengaturan Role',
        subMenus: [] 
      },
      {
        route: '/settings/user',
        label: 'Pengaturan User',
        subMenus: [] 
      },
    ] 
  },
]

export default menus
