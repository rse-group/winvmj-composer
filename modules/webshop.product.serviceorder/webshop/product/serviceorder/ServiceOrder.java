package webshop.product.serviceorder;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import vmj.routing.route.VMJServer;
import vmj.routing.route.Router;
import vmj.hibernate.integrator.HibernateUtil;
import org.hibernate.cfg.Configuration;

import vmj.auth.model.UserResourceFactory;
import vmj.auth.model.RoleResourceFactory;
import vmj.auth.model.core.UserResource;
import vmj.auth.model.core.RoleResource;

import webshop.seller.SellerServiceFactory;
import webshop.seller.core.SellerService;
import webshop.catalog.CatalogResourceFactory;
import webshop.catalog.core.CatalogResource;
import webshop.catalog.CatalogServiceFactory;
import webshop.catalog.core.CatalogService;
import webshop.customer.CustomerServiceFactory;
import webshop.customer.core.CustomerService;
import webshop.order.OrderResourceFactory;
import webshop.order.core.OrderResource;
import webshop.order.OrderServiceFactory;
import webshop.order.core.OrderService;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

public class ServiceOrder {

	private static Connection connection;
	private static Channel channel;
	private static String APP_ID = "serviceorder";

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

		configuration.addAnnotatedClass(vmj.auth.model.core.Role.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.RoleComponent.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.RoleDecorator.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.RoleImpl.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.UserRole.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.UserRoleComponent.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.UserRoleDecorator.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.UserRoleImpl.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.User.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.UserComponent.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.UserDecorator.class);
		configuration.addAnnotatedClass(vmj.auth.model.core.UserImpl.class);
		configuration.addAnnotatedClass(vmj.auth.model.passworded.UserImpl.class);

		configuration.addAnnotatedClass(webshop.seller.core.Seller.class);
		configuration.addAnnotatedClass(webshop.seller.core.SellerComponent.class);
		configuration.addAnnotatedClass(webshop.seller.core.SellerDecorator.class);
		configuration.addAnnotatedClass(webshop.seller.core.SellerImpl.class);
		configuration.addAnnotatedClass(webshop.catalog.core.Catalog.class);
		configuration.addAnnotatedClass(webshop.catalog.core.CatalogComponent.class);
		configuration.addAnnotatedClass(webshop.catalog.core.CatalogDecorator.class);
		configuration.addAnnotatedClass(webshop.catalog.core.CatalogImpl.class);
		configuration.addAnnotatedClass(webshop.customer.core.Customer.class);
		configuration.addAnnotatedClass(webshop.customer.core.CustomerComponent.class);
		configuration.addAnnotatedClass(webshop.customer.core.CustomerDecorator.class);
		configuration.addAnnotatedClass(webshop.customer.core.CustomerImpl.class);
		configuration.addAnnotatedClass(webshop.order.core.Order.class);
		configuration.addAnnotatedClass(webshop.order.core.OrderComponent.class);
		configuration.addAnnotatedClass(webshop.order.core.OrderDecorator.class);
		configuration.addAnnotatedClass(webshop.order.core.OrderImpl.class);

		Map<String, Object> featureModelMappings = mappingFeatureModel();
		Gson gson = new Gson();
		Type type = new TypeToken<Map<String, Map<String, String[]>>>(){}.getType();
		String convertedFeatureModelMappings = gson.toJson(featureModelMappings, type);

		configuration.setProperty("feature.model.mappings", convertedFeatureModelMappings);
		configuration.buildMappings();
		HibernateUtil.buildSessionFactory(configuration);

		setUpRabbitMQConnection();
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
		UserResource userResource = UserResourceFactory
				.createUserResource("vmj.auth.model.core.UserResourceImpl"
				);

		RoleResource roleResource = RoleResourceFactory
				.createRoleResource("vmj.auth.model.core.RoleResourceImpl"
				);

		UserResource userPasswordedResource = UserResourceFactory
				.createUserResource("vmj.auth.model.passworded.UserResourceImpl"
						,
						UserResourceFactory.createUserResource("vmj.auth.model.core.UserResourceImpl"));

		CatalogService catalogCatalogService = CatalogServiceFactory
				.createCatalogService("webshop.catalog.core.CatalogServiceImpl", channel, APP_ID
				);

		CatalogResource catalogCatalogResource = CatalogResourceFactory
				.createCatalogResource("webshop.catalog.core.CatalogResourceImpl", catalogCatalogService
				);

		OrderService orderOrderService = OrderServiceFactory
				.createOrderService("webshop.order.core.OrderServiceImpl", channel, APP_ID, catalogCatalogService
				);

		OrderResource orderOrderResource = OrderResourceFactory
				.createOrderResource("webshop.order.core.OrderResourceImpl", orderOrderService
				);

		System.out.println("orderOrderResource endpoints binding");
		Router.route(orderOrderResource);

		System.out.println("orderOrderService endpoints binding");
		Router.route(orderOrderService);

		System.out.println("catalogCatalogResource endpoints binding");
		Router.route(catalogCatalogResource);

		System.out.println("catalogCatalogService endpoints binding");
		Router.route(catalogCatalogService);

		System.out.println("authResource endpoints binding");
		Router.route(userPasswordedResource);
		Router.route(roleResource);
		Router.route(userResource);
	}

	private static Map<String, Object> mappingFeatureModel() {
		Map<String, Object> featureModelMappings = new HashMap<>();

		featureModelMappings.put(
				webshop.seller.core.SellerComponent.class.getName(),
				new HashMap<String, String[]>() {{
					put("components", new String[] {
							webshop.seller.core.SellerComponent.class.getName()
					});
					put("deltas", new String[] {
					});
				}}
		);

		featureModelMappings.put(
				webshop.catalog.core.CatalogComponent.class.getName(),
				new HashMap<String, String[]>() {{
					put("components", new String[] {
							webshop.catalog.core.CatalogComponent.class.getName()
					});
					put("deltas", new String[] {
					});
				}}
		);

		featureModelMappings.put(
				webshop.customer.core.CustomerComponent.class.getName(),
				new HashMap<String, String[]>() {{
					put("components", new String[] {
							webshop.customer.core.CustomerComponent.class.getName()
					});
					put("deltas", new String[] {
					});
				}}
		);

		featureModelMappings.put(
				webshop.order.core.OrderComponent.class.getName(),
				new HashMap<String, String[]>() {{
					put("components", new String[] {
							webshop.order.core.OrderComponent.class.getName()
					});
					put("deltas", new String[] {
					});
				}}
		);

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
				String error_message = String.format("Please check '%s' in your local environment variable or "
						+ "'hibernate.connection.%s' in your 'hibernate.properties' file!", varname, typeProp);
				System.out.println(error_message);
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

	public static void setUpRabbitMQConnection() {
		try {
			String rabbitMqHost = System.getenv("RABBITMQ_HOST") != null ? System.getenv("RABBITMQ_HOST") : "localhost";
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(rabbitMqHost);
			connection = factory.newConnection();
			channel = connection.createChannel();


			String BASE_EXCHANGE = "webshop";
			channel.exchangeDeclare(BASE_EXCHANGE, "direct");

		} catch (Exception e) {
			System.out.println("Failed to connect to RabbitMQ " +e);
		}
	}

}