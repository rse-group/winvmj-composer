package ${productPackage};

import java.util.ArrayList;

import vmj.object.mapper.VMJDatabaseMapper;
import vmj.routing.route.VMJServer;
import vmj.routing.route.Router;

<#list imports as import>
import ${import};
</#list>

public class ${productName} {

	public static void main(String[] args) {
		activateServer("localhost", 7776);
		Configuration configuration = new Configuration().configure();
		configuration.addResource("program_concrete_union.hbm.xml");
		<#list models as modelSpec>
		<#list modelSpec['class'] as className>
		configuration.addAnnotatedClass(${modelSpec['module']}.${className}.class);
		</#list>
		</#list>
		configuration.buildMappings();
		HibernateUtil.buildSessionFactory(configuration);
		createObjectsAndBindEndPoints();
	}

	public static void activateServer(String hostName, int portNumber) {
		VMJServer vmjServer = VMJServer.getInstance(hostName, portNumber);
		try {
			vmjServer.startServerGeneric();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public static void createObjectsAndBindEndPoints() {
		System.out.println("== CREATING OBJECTS AND BINDING ENDPOINTS ==");
		<#list routings as routeSpec>
		${routeSpec['class']} ${routeSpec['variableName']} = ${routeSpec['factory']}
			.create${routeSpec['factory']}(
			"${routeSpec['module']}.${routeSpec['implClass']}"
			<#if routeSpec['parentVariable']??>, ${routeSpec['parentVariable']}</#if>);
		</#list>

		<#list routings as routeSpec>
		System.out.println("${routeSpec['variableName']} endpoints binding");
		Router.route(${routeSpec['variableName']});
		
		</#list>
		System.out.println();
	}

}