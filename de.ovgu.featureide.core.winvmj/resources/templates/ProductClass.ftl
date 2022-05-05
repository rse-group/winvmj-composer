package ${productPackage};

import java.util.ArrayList;

import vmj.routing.route.VMJServer;
import vmj.routing.route.Router;
import vmj.hibernate.integrator.HibernateUtil;
import org.hibernate.cfg.Configuration;

<#list imports as import>
import ${import};
</#list>

import prices.auth.vmj.model.UserControllerFactory;
import prices.auth.vmj.model.RoleControllerFactory;
import prices.auth.vmj.model.core.UserController;
import prices.auth.vmj.model.core.RoleController;


public class ${productName} {

	public static void main(String[] args) {
		activateServer("localhost", 7776);
		Configuration configuration = new Configuration();
		<#list models as modelSpec>
		<#list modelSpec['class'] as className>
		configuration.addAnnotatedClass(${modelSpec['module']}.${className}.class);
		</#list>
		</#list>
		configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserComponent.class);
		configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserImpl.class);
		configuration.addAnnotatedClass(prices.auth.vmj.model.passworded.UserPasswordedImpl.class);
		configuration.addAnnotatedClass(prices.auth.vmj.model.core.RoleComponent.class);
		configuration.addAnnotatedClass(prices.auth.vmj.model.core.RoleImpl.class);
		configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserRoleImpl.class);
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
			.create${routeSpec['class']}(
			"${routeSpec['module']}.${routeSpec['implClass']}"
			<#if routeSpec['coreModule']??>,
			${routeSpec['factory']}.create${routeSpec['class']}(
			"${routeSpec['coreModule']}.${routeSpec['coreImplClass']}")</#if>);
		</#list>
		
		UserController userCore = UserControllerFactory
				.createUserController("prices.auth.vmj.model.core.UserControllerImpl");
		UserController userPassworded = UserControllerFactory
				.createUserController("prices.auth.vmj.model.passworded.UserPasswordedControllerDecorator", userCore);
		RoleController roleCore = RoleControllerFactory
				.createRoleController("prices.auth.vmj.model.core.RoleControllerImpl");


		<#list routings?reverse as routeSpec>
		System.out.println("${routeSpec['variableName']} endpoints binding");
		Router.route(${routeSpec['variableName']});
		Router.route(userPassworded);
		Router.route(roleCore);
		
		</#list>
		System.out.println();
	}

}