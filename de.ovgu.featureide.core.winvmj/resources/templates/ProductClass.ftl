package ${productPackage};

import java.util.ArrayList;

import vmj.routing.route.VMJServer;
import vmj.routing.route.Router;
import vmj.hibernate.integrator.HibernateUtil;
import org.hibernate.cfg.Configuration;

<#list imports as import>
import ${import};
</#list>

import prices.auth.vmj.model.UserResourceFactory;
import prices.auth.vmj.model.RoleResourceFactory;
import prices.auth.vmj.model.core.UserResource;
import prices.auth.vmj.model.core.RoleResource;


public class ${productName} {

	public static void main(String[] args) {
		
		// get hostAddress and portnum from env var
        // ex:
        // AMANAH_HOST_BE --> "localhost"
        // AMANAH_PORT_BE --> 7776
		String hostAddress= getEnvVariableHostAddress("AMANAH_HOST_BE");
        int portNum = getEnvVariablePortNumber("AMANAH_PORT_BE");
        activateServer(hostAddress, portNum);

		Configuration configuration = new Configuration();
		// panggil setter setelah membuat object dari kelas Configuration
        // ex:
        // AMANAH_DB_URL --> jdbc:postgresql://localhost:5432/superorg
        // AMANAH_DB_USERNAME --> postgres
        // AMANAH_DB_PASSWORD --> postgres123
		setDBProperties("AMANAH_DB_URL", "url", configuration);
        setDBProperties("AMANAH_DB_USERNAME", "username", configuration);
        setDBProperties("AMANAH_DB_PASSWORD","password", configuration);
		
		<#list models as modelSpec>
		<#list modelSpec['class'] as className>
		configuration.addAnnotatedClass(${modelSpec['module']}.${className}.class);
		</#list>
		</#list>
		
		configuration.addAnnotatedClass(prices.auth.vmj.model.core.Role.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.core.RoleComponent.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.core.RoleImpl.class);
        
        configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserRole.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserRoleComponent.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserRoleImpl.class);

        configuration.addAnnotatedClass(prices.auth.vmj.model.core.User.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserComponent.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserDecorator.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.core.UserImpl.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.passworded.UserPasswordedImpl.class);
        configuration.addAnnotatedClass(prices.auth.vmj.model.social.UserSocialImpl.class);
		
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
		
		UserResource userCore = UserResourceFactory
                .createUserResource("prices.auth.vmj.model.core.UserResourceImpl");
        UserResource userPassworded = UserResourceFactory
	        .createUserResource("prices.auth.vmj.model.passworded.UserPasswordedResourceDecorator",
		        UserResourceFactory
		        	.createUserResource("prices.auth.vmj.model.core.UserResourceImpl"));
        UserResource userSocial = UserResourceFactory
        	.createUserResource("prices.auth.vmj.model.social.UserSocialResourceDecorator",
        		userPassworded);        
        RoleResource role = RoleResourceFactory
        	.createRoleResource("prices.auth.vmj.model.core.RoleResourceImpl");

		<#list routings?reverse as routeSpec>
		System.out.println("${routeSpec['variableName']} endpoints binding");
		Router.route(${routeSpec['variableName']});
		
		</#list>
		
		System.out.println("auth endpoints binding");
		Router.route(userCore);
		Router.route(userPassworded);
		Router.route(userSocial);
		Router.route(role);
		System.out.println();
	}
	
	public static void setDBProperties(String varname, String typeProp, Configuration configuration){
        try{
                String varNameValue = System.getenv(varname);
                String propertyName = String.format("hibernate.connection.%s",typeProp);
                configuration.setProperty(propertyName, varNameValue);
        }catch (Exception e){
                String error_message = String.format("%s: try to check %s in your local environment variable!", e, varname);
                System.out.println(error_message);
        }
    }

	// if the env variable for server host is null, use localhost instead.
    public static String getEnvVariableHostAddress(String varname_host){
            String hostAddress = System.getenv(varname_host)  != null ? System.getenv(varname_host) : "localhost"; // Host
            return hostAddress;
    }

    // try if the environment variable for port number is null, use 7776 instead
    public static int getEnvVariablePortNumber(String varname_port){
            String portNum = System.getenv(varname_port)  != null? System.getenv(varname_port)  : "7776"; //PORT
            int portNumInt = Integer.parseInt(portNum);
            return portNumInt;
    }

}