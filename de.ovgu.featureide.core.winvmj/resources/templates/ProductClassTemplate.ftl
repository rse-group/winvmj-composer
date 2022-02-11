package ${productPackage};

import java.util.ArrayList;

import vmj.object.mapper.VMJDatabaseMapper;
import vmj.routing.route.VMJServer;
import vmj.routing.route.Router;

// TO DO: Specify your imports based on your configurations below

public class ${productName} {

	public static void main(String[] args) {
		generateTables();
		activateServer("localhost", 7776);
		generateCRUDEndpoints();
		createObjectsAndBindEndPoints();
	}

	public static void generateTables() {
		System.out.println("== GENERATING TABLES ==");
		// TO DO: Specify your table generations
		
		System.out.println();
	}

	public static void activateServer(String hostName, int portNumber) {
		VMJServer vmjServer = VMJServer.getInstance(hostName, portNumber);
		try {
			vmjServer.startServerGeneric();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void generateCRUDEndpoints() {
		System.out.println("== CRUD ENDPOINTS ==");
		VMJServer vmjServer = VMJServer.getInstance();

		// TO DO: Specify your CRUDS
	}

	public static void createObjectsAndBindEndPoints() {
		System.out.println("== CREATING OBJECTS AND BINDING ENDPOINTS ==");
		// TO DO: Sepecify your object creations and bindings
	}

}