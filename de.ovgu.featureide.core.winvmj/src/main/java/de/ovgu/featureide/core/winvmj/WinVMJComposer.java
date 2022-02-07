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

import com.google.common.io.Files;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.ComposerExtensionClass;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.attributes.base.impl.ExtendedFeature;
import de.ovgu.featureide.fm.attributes.base.impl.ExtendedFeatureModel;
import de.ovgu.featureide.fm.attributes.base.impl.FeatureAttributeFactory;
import de.ovgu.featureide.fm.attributes.format.XmlExtendedFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;
import de.ovgu.featureide.fm.core.io.IFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

public class WinVMJComposer extends ComposerExtensionClass {
	
	public static String FEATURE_MODULE_MAPPER_FILENAME = "feature_to_module.json";
	public static String DB_AND_ROUTING_FILENAME = "db_and_routing.json";
	private String FEATURE_MODULE_SPEC_FILENAME = "selected_features.txt";
	private String MODULE_FOLDERNAME = "modules";

	@Override
	public boolean initialize(IFeatureProject project) {
		super.initialize(project);
		// initFeatureModel(project);
		initExtraConfigFiles(project);
		return true;
	}

	@Override
	public void performFullBuild(Path config) {
		// TODO Auto-generated method stub
		IFile featureModuleMapper = featureProject.getProject().getFile(FEATURE_MODULE_MAPPER_FILENAME);
		String splName = getSplName(featureProject);
		String productName = getProductNameFromConfig(featureProject);
		
		WinVMJProduct product = new WinVMJProduct(productName, splName, featureProject.loadConfiguration(config)
				.getSelectedFeatures(), featureModuleMapper);
		WinVMJConsole.println(splName);
		WinVMJConsole.println(product.getProductName());
		WinVMJConsole.println(product.getProductQualifiedName());
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
			WinVMJConsole.println("Create module " + productModule.getName() + " at " + productModule.getLocation().toOSString());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
	
	@Override
	public IFeatureModelFormat getFeatureModelFormat() {
		return new XmlExtendedFeatureModelFormat();
	}
	
	private boolean initFeatureModel(IFeatureProject project) {
		ExtendedFeatureModel featureModel = (ExtendedFeatureModel) project.getFeatureModel();
		ExtendedFeature rootFeature = (ExtendedFeature) featureModel.getStructure().getRoot().getFeature();
		
		if (rootFeature.getAttribute("productName") == null) {
			FeatureAttributeFactory attrFactory = new FeatureAttributeFactory();
			rootFeature.addAttribute(attrFactory
					.createStringAttribute(rootFeature, "productName", "", "", false, true));
			FeatureModelManager.save(featureModel, 
					EclipseFileSystem.getPath(project.getProject().getFile("model.xml")), 
					getFeatureModelFormat());
		}
		
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
		InputStream emptyContentStream = new ByteArrayInputStream("".getBytes());
		
		try {
			IFile featureModuleMapper = project.getProject().getFile(FEATURE_MODULE_MAPPER_FILENAME);
			if (!featureModuleMapper.exists()) featureModuleMapper.create(emptyContentStream, false, null);
			
			IFile dbAndRoutingFile = project.getProject().getFile(DB_AND_ROUTING_FILENAME);
			if (!dbAndRoutingFile.exists()) dbAndRoutingFile.create(emptyContentStream, false, null);
			
			IFolder moduleFolder = project.getProject().getFolder(MODULE_FOLDERNAME);
			if (!moduleFolder.exists()) moduleFolder.create(false, true, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	private String getSplName(IFeatureProject project) {
		return project.getFeatureModel().getStructure().getRoot().getFeature().getName();
	}
	
	private String getProductNameFromConfig(IFeatureProject project) {
		ExtendedFeature rootFeature = (ExtendedFeature) project.loadCurrentConfiguration().getRoot().getFeature();
		if (hasProductName(rootFeature)) return rootFeature.getAttribute("productName").getValue().toString();
		else {
			return Files.getNameWithoutExtension(featureProject
					.getCurrentConfiguration().getFileName().toString());
		}
	}
	
	private boolean hasProductName(ExtendedFeature rootFeature) {
		return rootFeature.getAttribute("productName") != null && 
				!rootFeature.getAttribute("productName").getValue().toString().equals("");
	}

}
