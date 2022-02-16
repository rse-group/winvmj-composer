package ${productPackage};

import java.util.ArrayList;

import vmj.object.mapper.VMJDatabaseMapper;
import vmj.routing.route.VMJServer;
import vmj.routing.route.Router;
import vmj.hibernate.integrator.HibernateUtil;
import org.hibernate.cfg.Configuration;

<#list imports as import>
import ${import};
</#list>

import prices.auth.vmj.model.UserFactory;
import prices.auth.vmj.model.RoleFactory;
import prices.auth.vmj.model.core.User;
import prices.auth.vmj.model.core.Role;

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
		
		generateTables();
		generateCRUDEndpoints();
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
	
	public static void generateTables() {
		try {
			System.out.println("== GENERATING TABLES ==");
			VMJDatabaseMapper.generateTable("prices.auth.vmj.model.passworded.UserImpl", false);
			VMJDatabaseMapper.generateTable("prices.auth.vmj.model.core.RoleImpl", false);
			VMJDatabaseMapper.generateTable("prices.auth.vmj.model.core.UserRoleImpl", false);
			System.out.println();
		} catch (Exception e) {
			System.out.println("Skipping generate tables...");
		} catch (Error e) {
			System.out.println("Skipping generate tables...");
		}

	}

	public static void generateCRUDEndpoints() {
		System.out.println("== CRUD ENDPOINTS ==");
		VMJServer vmjServer = VMJServer.getInstance();

		/**
		 * AUTH BASE MODELS
		 */
		vmjServer.createABSCRUDEndpoint("users", "auth_user", "prices.auth.vmj.model.core.UserImpl",
				VMJDatabaseMapper.getTableColumnsNames("prices.auth.vmj.model.core.UserImpl", false));
		vmjServer.createABSCRUDEndpoint("users", "auth_user_passworded", "prices.auth.vmj.model.passworded.UserImpl",
				VMJDatabaseMapper.getTableColumnsNames("prices.auth.vmj.model.passworded.UserImpl", true));
		vmjServer.createABSCRUDEndpoint("roles", "auth_role", "prices.auth.vmj.model.core.RoleImpl",
				VMJDatabaseMapper.getTableColumnsNames("prices.auth.vmj.model.core.RoleImpl", false));
		vmjServer.createABSCRUDEndpoint("user-roles", "auth_user_role", "prices.auth.vmj.model.core.UserRoleImpl",
				VMJDatabaseMapper.getTableColumnsNames("prices.auth.vmj.model.core.UserRoleImpl", false));

		System.out.println();

	}

	public static void createObjectsAndBindEndPoints() {
		System.out.println("== CREATING OBJECTS AND BINDING ENDPOINTS ==");
		<#list routings as routeSpec>
		${routeSpec['class']} ${routeSpec['variableName']} = ${routeSpec['factory']}
			.create${routeSpec['class']}(
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