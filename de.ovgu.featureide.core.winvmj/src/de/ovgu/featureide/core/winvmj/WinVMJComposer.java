package de.ovgu.featureide.core.winvmj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.google.gson.JsonObject;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.ComposerExtensionClass;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.core.impl.ProductToCompose;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ModuleInfoRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ProductClassRenderer;
import de.ovgu.featureide.fm.core.io.IFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

public class WinVMJComposer extends ComposerExtensionClass {
	
	public static String FEATURE_MODULE_MAPPER_FILENAME = "feature_to_module.json";
	public static String INTER_SPL_PRODUCT_MAPPER_FILENAME = "inter_spl_product.json";
	public static String DB_CONFIG_FILENAME = "db.properties";
	public static String EXTERNAL_LIB_FOLDERNAME = "external";
	public static String MODULE_FOLDERNAME = "modules";
	public static String INTERFACES_FOLDERNAME = "interfaces";
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
		return (previousConfig != null && previousConfig.equals(
				featureProject.getCurrentConfiguration()) && 
				previousFeatures.equals(featureProject
						.loadCurrentConfiguration().getSelectedFeatureNames()));
	}

	@Override
	public void performFullBuild(Path config) {
		if (isSameConfig()) return;
		updatePreviousConfig();
		
		WinVMJProduct product = new ProductToCompose(featureProject, config);
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {
			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				composeProduct(product);
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compose Product").schedule();
	}
	
	private void composeProduct(WinVMJProduct product) {
		try {
			CorePlugin.getDefault();
			for (IProject refProject: featureProject.getProject().getReferencedProjects())
				selectModulesFromProject(CorePlugin.getFeatureProject(refProject), 
						featureProject, product);
			selectModulesFromProject(featureProject, featureProject, product);
			IFolder productModule = featureProject.getBuildFolder()
					.getFolder(product.getProductQualifiedName());
			if (!productModule.exists()) productModule.create(false, true, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		TemplateRenderer moduleInfoRenderer = new ModuleInfoRenderer(featureProject);
		TemplateRenderer productClassRenderer = new ProductClassRenderer(featureProject);
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
	
	@Override
	public IFeatureModelFormat getFeatureModelFormat() {
		return new UVLFeatureModelFormat();
	}
	
	private void selectModulesFromProject(IFeatureProject sourceProject, 
			IFeatureProject destProject, WinVMJProduct product) throws CoreException {
		for (IFolder sourceModule: product.getModules()) {
			IFolder destModule = destProject.getBuildFolder().getFolder(sourceModule.getName());
			if (!destModule.exists() && sourceModule.exists()) {
				destModule.create(false, true, null);
				copy(sourceModule, destModule);
			}
		}
	}
	
	private boolean initExtraConfigFiles(IFeatureProject project) {
		JsonObject jsonInitContent = new JsonObject();
		InputStream emptyContentStream = new ByteArrayInputStream(
				jsonInitContent.toString().getBytes());
		Properties dbProperties = new Properties();
		
		try {
			IFile featureModuleMapper = project.getProject().getFile(FEATURE_MODULE_MAPPER_FILENAME);
			if (!featureModuleMapper.exists()) {
				featureModuleMapper.create(emptyContentStream, false, null);
				emptyContentStream.close();
			}
			
			IFile dbPropertiesFile = project.getProject().getFile(DB_CONFIG_FILENAME);
			if (!dbPropertiesFile.exists()) {
				dbProperties.setProperty("db.username", "");
				dbProperties.setProperty("db.password", "");
				ByteArrayOutputStream dbPropStream = new ByteArrayOutputStream();
				
				dbProperties.store(dbPropStream, null);
				InputStream dbPropInputStream = new ByteArrayInputStream(dbPropStream.toByteArray());
				dbPropertiesFile.create(dbPropInputStream, true, null);
			}
			
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
