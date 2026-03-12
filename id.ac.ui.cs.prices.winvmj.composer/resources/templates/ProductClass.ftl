package ${productPackage};

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Type;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.ac.ui.cs.prices.winvmj.core.VMJCors;
import id.ac.ui.cs.prices.winvmj.core.VMJServer;
import id.ac.ui.cs.prices.winvmj.core.Router;
import id.ac.ui.cs.prices.winvmj.hibernate.HibernateUtil;
import org.hibernate.cfg.Configuration;

<#if dbMetricsEnabled!false>
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
</#if>

<#if defaultAuthModel>
import id.ac.ui.cs.prices.winvmj.auth.model.UserResourceFactory;
import id.ac.ui.cs.prices.winvmj.auth.model.RoleResourceFactory;
import id.ac.ui.cs.prices.winvmj.auth.model.core.resource.UserResource;
import id.ac.ui.cs.prices.winvmj.auth.model.core.resource.RoleResource;

</#if>
<#list imports as import>
import ${import};
</#list>

public class ${productName} {

	private static final Logger logger;
	
	static {
		logger = LoggerFactory.getLogger(${productName}.class);
	}
    
	public static void main(String[] args) {

		<#if monitoringEnabled>
		// Initialize monitoring aspect early (before Hibernate) to start OTEL
		try {
			Class.forName("${productPackage?split('.')[0]}.monitoring.aspect.MonitoringAspect");
			logger.info("[${productName}] MonitoringAspect initialized - OTEL metrics/tracing starting");
		} catch (ClassNotFoundException e) {
			logger.info("[${productName}] MonitoringAspect not found - monitoring disabled");
		}
		</#if>


		// get hostAddress and portnum from env var
        // ex:
        // AMANAH_HOST_BE --> "localhost"
        // AMANAH_PORT_BE --> 7776
		String hostAddress= getEnvVariableHostAddress("AMANAH_HOST_BE");
        int portNum = getEnvVariablePortNumber("AMANAH_PORT_BE");
        activateServer(hostAddress, portNum);
		setCors();

		Configuration configuration = new Configuration();
		// panggil setter setelah membuat object dari kelas Configuration
        // ex:
        // AMANAH_DB_URL --> jdbc:postgresql://localhost:5432/superorg
        // AMANAH_DB_USERNAME --> postgres
        // AMANAH_DB_PASSWORD --> postgres123
		setDBProperties("AMANAH_DB_URL", "url", configuration);
        setDBProperties("AMANAH_DB_USERNAME", "username", configuration);
        setDBProperties("AMANAH_DB_PASSWORD","password", configuration);

		<#if defaultAuthModel>
		configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserComponent.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserDecorator.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserImpl.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.RoleComponent.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.RoleDecorator.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.RoleImpl.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserRoleComponent.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserRoleDecorator.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserRoleImpl.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.core.model.User.class);
        configuration.addAnnotatedClass(id.ac.ui.cs.prices.winvmj.auth.model.passworded.model.UserImpl.class);

		</#if>
		<#list models as modelSpec>
		<#list modelSpec['class'] as className>
		configuration.addAnnotatedClass(${modelSpec['module']}.${className}.class);
		</#list>
		</#list>

		Map<String, Object> featureModelMappings = mappingFeatureModel();
		Gson gson = new Gson();
		Type type = new TypeToken<Map<String, Map<String, String[]>>>(){}.getType();
        String convertedFeatureModelMappings = gson.toJson(featureModelMappings, type);
		
        configuration.setProperty("feature.model.mappings", convertedFeatureModelMappings);
		configuration.buildMappings();
		// Try to initialize Hibernate - graceful failure if DB not available
		try {
			HibernateUtil.buildSessionFactory(configuration);

			<#if dbMetricsEnabled!false>
			// Register Hibernate Event Listeners for DB Metrics
			try {
				SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
				EventListenerRegistry registry = ((SessionFactoryImpl) sessionFactory)
					.getServiceRegistry()
					.getService(EventListenerRegistry.class);
				
				${productPackage?split('.')[0]}.monitoring.aspect.MonitoringAspect.DbMetricsEventListener dbListener = 
					${productPackage?split('.')[0]}.monitoring.aspect.MonitoringAspect.getDbMetricsEventListener();
				
				registry.appendListeners(EventType.PRE_INSERT, dbListener);
				registry.appendListeners(EventType.PRE_UPDATE, dbListener);
				registry.appendListeners(EventType.PRE_DELETE, dbListener);
				registry.appendListeners(EventType.POST_LOAD, dbListener);
				logger.info("[${productName}] DB Metrics Hibernate Event Listeners registered");
			} catch (Exception e) {
				logger.warn("[${productName}] Failed to register DB Metrics listeners: {}", e.getMessage());
			}
			</#if>

			createObjectsAndBindEndPoints();
		} catch (Exception e) {
			logger.warn("[${productName}] Database connection failed - server running but database features disabled");
			logger.debug("[${productName}] Database error: {}", e.getMessage());
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

	public static void createObjectsAndBindEndPoints() {
		logger.info("[${productName}] Creating objects and binding endpoints");
		<#if defaultAuthModel>
		UserResource userResource = UserResourceFactory
            .createUserResource("id.ac.ui.cs.prices.winvmj.auth.model.core.resource.UserResourceImpl"
			);

		RoleResource roleResource = RoleResourceFactory
        	.createRoleResource("id.ac.ui.cs.prices.winvmj.auth.model.core.resource.RoleResourceImpl"
			);
        
        UserResource userPasswordedResource = UserResourceFactory
	        .createUserResource("id.ac.ui.cs.prices.winvmj.auth.model.passworded.resource.UserResourceImpl"
			,
		    UserResourceFactory.createUserResource("id.ac.ui.cs.prices.winvmj.auth.model.core.resource.UserResourceImpl"));
		</#if>

		<#list routings as moduleRoutings>
            <#list moduleRoutings as routeSpec>
                <#if routeSpec['componentType'] == "service">
        ${routeSpec['class']} ${routeSpec['variableName']} = ${routeSpec['factory']}
            .create${routeSpec['class']}("${routeSpec['module']}.${routeSpec['implClass']}" 
            	<#if routeSpec['wrappedVariableName']??>, ${routeSpec['wrappedVariableName']}Service</#if>);		
                </#if>
            </#list>

            <#list moduleRoutings as routeSpec>
                <#if routeSpec['componentType'] == "resource">
        ${routeSpec['class']} ${routeSpec['variableName']} = ${routeSpec['factory']}
            .create${routeSpec['class']}("${routeSpec['module']}.${routeSpec['implClass']}" 
                <#if routeSpec['wrappedVariableName']??>, ${routeSpec['wrappedVariableName']}Resource</#if>);
                </#if>
            </#list>
			
        </#list>

		<#list routings?reverse as listRouteSpec>
		<#list listRouteSpec as routeSpec>
		logger.info("[${productName}] Binding endpoints for ${routeSpec['variableName']}");
		Router.route(${routeSpec['variableName']});
		
		</#list>
		</#list>
		<#if defaultAuthModel>
		logger.info("[${productName}] Binding auth endpoints");
		Router.route(userPasswordedResource);
		Router.route(roleResource);
		Router.route(userResource);
		</#if>
	}

	private static Map<String, Object> mappingFeatureModel() {
		Map<String, Object> featureModelMappings = new HashMap<>();

		<#list featureModelMappings as ftm>
		featureModelMappings.put(
            ${ftm['referenceComponent']}.class.getName(),
			new HashMap<String, String[]>() {{ 
				put("components", new String[] {
					<#list ftm['featureModels']['components'] as component>
					<#if component?index != (ftm['featureModels']['components']?size - 1)>
					${component}.class.getName(),
					<#else>
					${component}.class.getName()
					</#if>
					</#list>
				});
				put("deltas", new String[] {
					<#list ftm['featureModels']['deltas'] as delta>
					<#if delta?index != (ftm['featureModels']['deltas']?size - 1)>
					${delta}.class.getName(),
					<#else>
					${delta}.class.getName()
					</#if>
					</#list>
				});
			}});
		</#list>
		featureModelMappings.put(
	            id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserComponent.class.getName(),
				new HashMap<String, String[]>() {{ 
					put("components", new String[] {
						id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserComponent.class.getName()
					});
					put("deltas", new String[] {
						id.ac.ui.cs.prices.winvmj.auth.model.passworded.model.UserImpl.class.getName()
					});
				}});
        
	    featureModelMappings.put(
				id.ac.ui.cs.prices.winvmj.auth.model.core.model.RoleComponent.class.getName(),
				new HashMap<String, String[]>() {{ 
					put("components", new String[] {
						id.ac.ui.cs.prices.winvmj.auth.model.core.model.RoleComponent.class.getName()
					});
					put("deltas", new String[] {
					});
				}});
        
	    featureModelMappings.put(
				id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserRoleComponent.class.getName(),
				new HashMap<String, String[]>() {{ 
					put("components", new String[] {
						id.ac.ui.cs.prices.winvmj.auth.model.core.model.UserRoleComponent.class.getName()
					});
					put("deltas", new String[] {
					});
				}});
        
		return featureModelMappings;
	}

	public static void setDBProperties(String varname, String typeProp, Configuration configuration) {
		String varNameValue = System.getenv(varname);
		String propertyName = String.format("hibernate.connection.%s",typeProp);
		if (varNameValue != null) {
			configuration.setProperty(propertyName, varNameValue);
		} else {
			String hibernatePropertyVal = configuration.getProperty(propertyName);
			if (hibernatePropertyVal == null) {
				logger.warn("[${productName}] Please check '{}' in your local environment variable or 'hibernate.connection.{}' in your 'hibernate.properties' file!", varname, typeProp);
			}
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
	
	public static void setCors() {
    	Properties properties = new Properties();
        String propertyValue = "";
        
        try (FileInputStream fileInput = new FileInputStream("cors.properties")) {
            properties.load(fileInput);
            propertyValue = properties.getProperty("allowedMethod");
            VMJCors.setAllowedMethod(propertyValue);
            
            propertyValue = properties.getProperty("allowedOrigin");
            VMJCors.setAllowedOrigin(propertyValue);
            
        		} catch (IOException e) {
			VMJCors.setAllowedMethod("GET, POST, PUT, PATCH, DELETE");
			VMJCors.setAllowedOrigin("*");
			logger.info("[${productName}] cors.properties not found, using defaults (allowedMethod=GET,POST,PUT,PATCH,DELETE, allowedOrigin=*)");
        }
    }

}