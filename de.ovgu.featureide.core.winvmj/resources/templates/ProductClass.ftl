package ${productPackage};

import java.util.ArrayList;

import vmj.object.mapper.VMJDatabaseMapper;
import vmj.routing.route.VMJServer;
import vmj.routing.route.Router;

<#list imports as import>
import ${import};
</#list>

import prices.auth.vmj.model.UserFactory;
import prices.auth.vmj.model.RoleFactory;
import prices.auth.vmj.model.core.User;
import prices.auth.vmj.model.core.Role;

public class ${productName} {

	public static void main(String[] args) {
		generateTables();
		activateServer("localhost", 7776);
		generateCRUDEndpoints();
		createObjectsAndBindEndPoints();
	}

	public static void generateTables() {
		try {
			System.out.println("== GENERATING TABLES ==");
			<#list models as modelSpec>
			VMJDatabaseMapper.generateTable("${modelSpec['module']}.${modelSpec['class']}", ${modelSpec['hasParentTable']?then('true', 'false')});
			</#list>
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

	public static void activateServer(String hostName, int portNumber) {
		VMJServer vmjServer = VMJServer.getInstance(hostName, portNumber);
		try {
			vmjServer.startServerGeneric();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public static void generateCRUDEndpoints() {
		System.out.println("== CRUD ENDPOINTS ==");
		VMJServer vmjServer = VMJServer.getInstance();
		
		/**
		 * Full Generated Table
		 */
		<#list models as modelSpec>
		<#if modelSpec['crudEndpoint']??>
		vmjServer.createABSCRUDEndpoint("${modelSpec['crudEndpoint']}", "${modelSpec['tableName']}", "${modelSpec['module']}.${modelSpec['class']}",
				VMJDatabaseMapper.getTableColumnsNames("${modelSpec['module']}.${modelSpec['class']}", ${modelSpec['hasParentTable']?then('true', 'false')}));
		</#if>
		</#list>
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
		${routeSpec['interface']} ${routeSpec['variableName']} = ${routeSpec['factory']}
			.create${routeSpec['interface']}("${routeSpec['module']}.${routeSpec['class']}"
			<#if routeSpec['parentVariable']??>, ${routeSpec['parentVariable']}</#if>);
		</#list>
		
		User user = UserFactory.createUser("prices.auth.vmj.model.core.UserImpl");
		User passwordedUser = UserFactory.createUser("prices.auth.vmj.model.passworded.UserImpl", user);
		Role role = RoleFactory.createRole("prices.auth.vmj.model.core.RoleImpl");

		<#list routings as routeSpec>
		System.out.println("${routeSpec['variableName']} endpoints binding");
		Router.route(${routeSpec['variableName']});
		
		</#list>
		System.out.println("Passworded User binding");
		Router.bindMethod("login", passwordedUser);
		Router.bindMethod("register", passwordedUser);
		Router.bindMethod("forgotPassword", passwordedUser);
		Router.bindMethod("getForgotPasswordToken", passwordedUser);
		Router.bindMethod("changePassword", passwordedUser);

		System.out.println("Permission mangement");
		Router.bindMethod("changePermissions", user);
		Router.bindMethod("changePermissions", role);
		System.out.println();
	}

}