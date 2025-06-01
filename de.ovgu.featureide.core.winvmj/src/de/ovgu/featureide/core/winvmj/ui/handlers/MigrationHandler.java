package de.ovgu.featureide.core.winvmj.ui.handlers;

import DatabaseState.DatabaseStateBuilder;
import DatabaseState.DatabaseStateComparer;
import DatabaseState.OperationCollection;
import DatabaseState.DatabaseState;
import MigrationBuilder.MigrationBuilder;
import MigrationBuilder.MigrationWriter;
import Graph.MigrationGraph;
import Loader.MigrationLoader;

import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.core.impl.ComposedProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;

public class MigrationHandler extends AFeatureProjectHandler {

	@Override
	protected void singleAction(IFeatureProject project) {
		WinVMJConsole.showConsole();
		WinVMJConsole.println("MakeMigration running");
		String projectPath = project.getProject().getLocation().toOSString();
		String parentPath = project.getProject().getLocation().removeLastSegments(1).toOSString() + java.io.File.separator;
		try {
			WinVMJProduct sourceProduct = new ComposedProduct(project);	
			String baseDirectory = project.getProject().getLocation().removeLastSegments(1).toOSString() + java.io.File.separator;
			String projectName = project.getProject().getName();
			String productName = sourceProduct.getProductName();
			
			MigrationLoader loader = new MigrationLoader();
			loader.setMigrationPath(projectPath);
			loader.setproductName(productName);
			
	        MigrationGraph graph = MigrationBuilder.getGraph(loader);
	        DatabaseStateBuilder stateBuilder = new DatabaseStateBuilder();
	        stateBuilder.setprojectName(projectName);
	        stateBuilder.setproductName(productName);
	        stateBuilder.setbaseDirectory(baseDirectory);
	        
	        DatabaseState migrationState = stateBuilder.buildDatabaseStateFromMigrationFiles(graph.nodes);
	        DatabaseState modelState = stateBuilder.buildDatabaseStateFromModels();
	        WinVMJConsole.println("===============Migration===============");
	        WinVMJConsole.println(migrationState.toString());
	        WinVMJConsole.println("=================Model=================");
	        WinVMJConsole.println(modelState.toString());
	        DatabaseStateComparer stateComparer = new DatabaseStateComparer();
	        OperationCollection operations = stateComparer.compareDatabaseState(migrationState, modelState);
	        
	        MigrationWriter writer = new MigrationWriter();
	        writer.setMigrationPath(projectPath);
	        writer.setproductName(productName);
	        
	        String migrationDependency = graph.getLatestMigrationName();
	        WinVMJConsole.println("Latest migration name: " + migrationDependency);
	        if (migrationDependency == null || migrationDependency.length() < 14) {
	        	WinVMJConsole.println("Error: Invalid migration name: '" + migrationDependency + "'");
	            return;
	        }
	        writer.writeMigration(operations, migrationDependency);
		} catch (CoreException e) {
			WinVMJConsole.println("Failed to build model state: " + e.getMessage());
			e.printStackTrace();
		}
		}
}
