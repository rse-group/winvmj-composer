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

export default const menus = [
<#list structures as structure>
  <@traversePrint structure, structure?has_next/>
</#list>
  {
    route: '#',
    label: 'Pengaturan',
    subMenus: [
      {
        route: '/pengaturan/tampilan',
        label: 'Pengaturan Tampilan',
        subMenus: [] 
      },
      {
        route: '/pengaturan/role',
        label: 'Pengaturan Role',
        subMenus: [] 
      },
      {
        route: '/pengaturan/user',
        label: 'Pengaturan User',
        subMenus: [] 
      },
    ] 
  },
]
