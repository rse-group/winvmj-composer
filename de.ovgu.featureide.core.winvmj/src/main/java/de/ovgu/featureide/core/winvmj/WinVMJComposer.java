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
import org.eclipse.core.runtime.CoreException;

import com.google.common.io.Files;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.ComposerExtensionClass;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ModuleInfoRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ProductClassRenderer;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;

public class WinVMJComposer extends ComposerExtensionClass {
	
	public static String FEATURE_MODULE_MAPPER_FILENAME = "feature_to_module.json";
	public static String DB_AND_ROUTING_FILENAME = "db_and_routing.json";
	private String FEATURE_MODULE_SPEC_FILENAME = "selected_features.txt";
	private String EXTERNAL_LIB_FOLDERNAME = "external";
	private String MODULE_FOLDERNAME = "modules";
	private Path previousConfig = null;
	private Set<String> previousFeatures = null;

	@Override
	public boolean initialize(IFeatureProject project) {
		super.initialize(project);
		createUvlFeatureModel(project);
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
		// TODO Auto-generated method stub
		if (isSameConfig()) {
			return;
		}
		updatePreviousConfig();
		IFile featureModuleMapper = featureProject.getProject().getFile(FEATURE_MODULE_MAPPER_FILENAME);
		String splName = getSplName(featureProject);
		String productName = getProductNameFromConfig(featureProject);
		
		WinVMJProduct product = new WinVMJProduct(productName, splName, featureProject.loadConfiguration(config)
				.getSelectedFeatures(), featureModuleMapper);
		IFolder moduleFolder = featureProject.getProject().getFolder(MODULE_FOLDERNAME);
		
		for (String module: product.getModules()) {
			IFolder buildFolder = featureProject.getBuildFolder().getFolder(module);
			try {
				buildFolder.create(false, true, null);
				copy(moduleFolder.getFolder(module), buildFolder);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		IFolder productModule = featureProject.getBuildFolder().getFolder(product.getProductQualifiedName());
		try {
			productModule.create(false, true, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TemplateRenderer moduleInfoRenderer = new ModuleInfoRenderer(featureProject);
		TemplateRenderer productClassRenderer = new ProductClassRenderer(featureProject);
		moduleInfoRenderer.render(product);
		productClassRenderer.render(product);
		createSpecFile(featureProject.loadConfiguration(config).getSelectedFeatureNames(), product);
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
	
	private boolean createUvlFeatureModel(IFeatureProject project) {
		IFile uvlFile = project.getProject().getFile("model.uvl");
//		if (uvlFile.exists()) uvlFile.create(null, isInitialized(), null);
		if (!uvlFile.exists())
			FeatureModelManager.save(project.getFeatureModel(), 
				EclipseFileSystem.getPath(uvlFile), 
				new UVLFeatureModelFormat());
		return true;
	}

	
	private boolean createSpecFile(Set<String> features, WinVMJProduct product) {
		IFile featureModuleSpec = featureProject.getBuildFolder()
				.getFolder(product.getProductQualifiedName())
				.getFile(FEATURE_MODULE_SPEC_FILENAME);
		String featureString = "Features:\n" + String.join("\n", features);
		String moduleString = "Modules:\n" + String.join("\n", product.getModules());
		String specString = featureString + "\n\n" + moduleString;
		InputStream specContentStream = new ByteArrayInputStream(specString.getBytes());
		try {
			if (featureModuleSpec.exists()) featureModuleSpec.setContents(specContentStream, false, false, null);
			else featureModuleSpec.create(specContentStream, false, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	private boolean initExtraConfigFiles(IFeatureProject project) {
		InputStream emptyContentStream = new ByteArrayInputStream("{}".getBytes());
		
		try {
			IFile featureModuleMapper = project.getProject().getFile(FEATURE_MODULE_MAPPER_FILENAME);
			if (!featureModuleMapper.exists()) {
				featureModuleMapper.create(emptyContentStream, false, null);
				emptyContentStream.close();
				emptyContentStream = new ByteArrayInputStream("{}".getBytes());
			}
			
			IFile dbAndRoutingFile = project.getProject().getFile(DB_AND_ROUTING_FILENAME);
			if (!dbAndRoutingFile.exists()) {
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
