package de.ovgu.featureide.core.winvmj;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.ComposerExtensionClass;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ModuleInfoRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ProductClassRenderer;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel;
import de.ovgu.featureide.fm.core.io.IFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;

public class WinVMJComposer extends ComposerExtensionClass {
	
	public static String FEATURE_MODULE_MAPPER_FILENAME = "feature_to_module.json";
	public static String DB_AND_ROUTING_FILENAME = "db_and_routing.json";
	public static String EXTERNAL_LIB_FOLDERNAME = "external";
	public static String HIBERNATE_MAPPERS_FOLDERNAME = "mappers";
	public static String MODULE_FOLDERNAME = "modules";
	private Path previousConfig = null;
	private Set<String> previousFeatures = null;

	@Override
	public boolean initialize(IFeatureProject project) {
		super.initialize(project);
		initExtraConfigFiles(project);
		return true;
	}
	
	@Override
	public boolean clean() {
		return !isSameConfig();
	}
	
	public void updatePreviousConfig() {
		previousConfig = featureProject.getCurrentConfiguration();
		previousFeatures = featureProject.loadCurrentConfiguration().getSelectedFeatureNames();
	}
	
	public boolean isSameConfig() {
		return (previousConfig != null && previousConfig.equals(featureProject.getCurrentConfiguration()) && 
				previousFeatures.equals(featureProject.loadCurrentConfiguration().getSelectedFeatureNames()));
	}

	@Override
	public void performFullBuild(Path config) {
		if (isSameConfig()) return;
		updatePreviousConfig();
		
//		MultiFeatureModel mplFm = (MultiFeatureModel) featureProject.getFeatureModel();
//		for (String imports: mplFm.getExternalModels().keySet()) {
//			WinVMJConsole.print(imports);
//			WinVMJConsole.print(": ");
//			WinVMJConsole.println(mplFm.getExternalModel(imports).getModelName());
//			WinVMJConsole.println(mplFm.getExternalModel(imports).getVarName());
//		}
		
		WinVMJProduct product = new WinVMJProduct(featureProject, config);
		
		try {
			IFolder moduleFolder = featureProject.getProject().getFolder(MODULE_FOLDERNAME);
			for (String module: product.getModules()) {
				IFolder buildFolder = featureProject.getBuildFolder().getFolder(module);
				if (!buildFolder.exists()) {
					buildFolder.create(false, true, null);
					copy(moduleFolder.getFolder(module), buildFolder);
				}
			}
			
			IFolder productModule = featureProject.getBuildFolder().getFolder(product.getProductQualifiedName());
			productModule.create(false, true, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		try {
			TemplateRenderer moduleInfoRenderer = new ModuleInfoRenderer(featureProject);
			TemplateRenderer productClassRenderer = new ProductClassRenderer(featureProject);
			moduleInfoRenderer.render(product);
			productClassRenderer.render(product);
			//hibernateCfgRenderer.render(product);
		} catch (Exception e) {
			for (StackTraceElement em: e.getStackTrace())
				WinVMJConsole.println(em.toString());
			e.printStackTrace();
		}
	}

	@Override
	public void buildPartialFeatureProjectAssets(IFolder sourceFolder, ArrayList<String> removedFeatures,
			ArrayList<String> mandatoryFeatures) throws IOException, CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean supportsPartialFeatureProject() {
		return false;
	}
	
	@Override
	public boolean hasFeatureFolder() {
		return false;
	}
	
//	@Override
//	public IFeatureModelFormat getFeatureModelFormat() {
//		return new UVLFeatureModelFormat();
//	}
	
	private boolean initExtraConfigFiles(IFeatureProject project) {
		JsonObject jsonInitContent = new JsonObject();
		InputStream emptyContentStream = new ByteArrayInputStream(jsonInitContent.toString().getBytes());
		
		try {
			IFile featureModuleMapper = project.getProject().getFile(FEATURE_MODULE_MAPPER_FILENAME);
			if (!featureModuleMapper.exists()) {
				featureModuleMapper.create(emptyContentStream, false, null);
				emptyContentStream.close();
			}
			
			IFile dbAndRoutingFile = project.getProject().getFile(DB_AND_ROUTING_FILENAME);
			if (!dbAndRoutingFile.exists()) {
				// prepare init content for db and routing
				jsonInitContent.add("dataModel", new JsonArray());
				jsonInitContent.add("methodRouting", new JsonArray());
				emptyContentStream = new ByteArrayInputStream(jsonInitContent.toString().getBytes());
				
				dbAndRoutingFile.create(emptyContentStream, false, null);
				emptyContentStream.close();
			}
			
			IFolder mapperFolder = project.getProject().getFolder(HIBERNATE_MAPPERS_FOLDERNAME);
			if (!mapperFolder.exists()) mapperFolder.create(false, true, null);
			
			IFolder moduleFolder = project.getProject().getFolder(MODULE_FOLDERNAME);
			if (!moduleFolder.exists()) moduleFolder.create(false, true, null);
			
			IFolder externalLibFolder = project.getProject().getFolder(EXTERNAL_LIB_FOLDERNAME);
			if (!externalLibFolder.exists()) externalLibFolder.create(false, true, null);
		} catch (CoreException | IOException e) {
			WinVMJConsole.println(e.getMessage());
			e.printStackTrace();
		}
		
		return true;
	}
}
