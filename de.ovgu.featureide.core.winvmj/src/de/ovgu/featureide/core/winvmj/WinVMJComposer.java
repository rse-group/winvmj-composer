package de.ovgu.featureide.core.winvmj;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.ComposerExtensionClass;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ModuleInfoHibernateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ModuleInfoRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ProductClassHibernateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ProductClassRenderer;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;
import de.ovgu.featureide.fm.core.io.manager.ConfigurationManager;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;

public class WinVMJComposer extends ComposerExtensionClass {
	
	public static String FEATURE_MODULE_MAPPER_FILENAME = "feature_to_module.json";
	public static String DB_AND_ROUTING_FILENAME = "db_and_routing.json";
	private String EXTERNAL_LIB_FOLDERNAME = "external";
	private String MODULE_FOLDERNAME = "modules";
	private Path previousConfig = null;
	private Set<String> previousFeatures = null;

	@Override
	public boolean initialize(IFeatureProject project) {
		super.initialize(project);
		try {
			createUvlFeatureModel(project);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		
		IFile featureModuleMapper = featureProject.getProject().getFile(FEATURE_MODULE_MAPPER_FILENAME);
		String splName = getSplName(featureProject);
		String productName = getProductNameFromConfig(featureProject);
		
		WinVMJProduct product = new WinVMJProduct(productName, splName, featureProject.loadConfiguration(config)
				.getSelectedFeatures(), featureModuleMapper);
		
		try {
			WinVMJConsole.println("Referenced Projects");
			for (IProject refProj: featureProject.getProject().getReferencedProjects()) {
				WinVMJConsole.println(refProj.getName());
				WinVMJConsole.println(refProj.getLocation().toOSString());
			}
			
			IFolder moduleFolder = featureProject.getProject().getFolder(MODULE_FOLDERNAME);
			for (String module: product.getModules()) {
				IFolder buildFolder = featureProject.getBuildFolder().getFolder(module);
				buildFolder.create(false, true, null);
				copy(moduleFolder.getFolder(module), buildFolder);
			}
			
			IFolder productModule = featureProject.getBuildFolder().getFolder(product.getProductQualifiedName());
			productModule.create(false, true, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		TemplateRenderer moduleInfoRenderer = new ModuleInfoHibernateRenderer(featureProject);
		TemplateRenderer productClassRenderer = new ProductClassHibernateRenderer(featureProject);
		moduleInfoRenderer.render(product);
		productClassRenderer.render(product);
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
	
	private IFile getConfigFromReferencedProject(IProject refProject, String configName) throws CoreException {
		return getConfigFromReferencedProject((IFolder) refProject, configName);
	}
	
	private IFile getConfigFromReferencedProject(IFolder folder, String configName) throws CoreException {
		for (IResource resource: folder.members()) {
			if (resource instanceof IFolder) {
				IFile configFile = getConfigFromReferencedProject((IFolder) resource,configName);
				if (configFile != null) return configFile;
			} else if (resource instanceof IFile && 
					Files.getNameWithoutExtension(resource.getName()).equals(configName) &&
					ConfigurationManager.isFileSupported(EclipseFileSystem.getPath(resource)))
				return (IFile) resource;
		}
		return null;
	}
	
	private boolean createUvlFeatureModel(IFeatureProject project) throws CoreException {
		IFile uvlFile = project.getProject().getFile("model.uvl");
//		if (uvlFile.exists()) uvlFile.create(null, isInitialized(), null);
		if (!uvlFile.exists())
			FeatureModelManager.save(project.getFeatureModel(), 
				EclipseFileSystem.getPath(uvlFile), 
				new UVLFeatureModelFormat());
		return true;
	}
	
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
			
			IFolder moduleFolder = project.getProject().getFolder(MODULE_FOLDERNAME);
			if (!moduleFolder.exists()) moduleFolder.create(false, true, null);
			
			IFolder externalLibFolder = project.getProject().getFolder(EXTERNAL_LIB_FOLDERNAME);
			if (!externalLibFolder.exists()) externalLibFolder.create(false, true, null);
		} catch (CoreException | IOException e) {
			// TODO Auto-generated catch block
			WinVMJConsole.println(e.getMessage());
			e.printStackTrace();
		}
		
		return true;
	}
	
	private String getSplName(IFeatureProject project) {
		return project.getFeatureModel().getStructure().getRoot().getFeature().getName();
	}
	
	private String getProductNameFromConfig(IFeatureProject project) {
		return StringUtils.capitalize(Files.getNameWithoutExtension(featureProject
				.getCurrentConfiguration().getFileName().toString()));
	}
}
