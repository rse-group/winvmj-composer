package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class ModuleInfoRenderer extends TemplateRenderer {

	private final static String PREFIX_AUTH_MODEL_PRODUCT = "auth";
	
	public ModuleInfoRenderer(IFeatureProject project) {
		super(project);
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("productPackage", product.getProductQualifiedName());
		dataModel.put("requiredModules", product.getModuleNames());
		dataModel.put("defaultAuthModel", checkDefaultAuthModel(product));
		
		return dataModel;
	}

	protected boolean checkDefaultAuthModel(WinVMJProduct product) {
		for (String module : product.getModuleNames()) {
			if (module.startsWith(PREFIX_AUTH_MODEL_PRODUCT))
				return false;
		}
		return true;
	}
	
	protected String loadTemplateFilename() {
		return "module-info";
	}
	
	protected IFile getOutputFile(WinVMJProduct product) {
		return project.getBuildFolder().getFolder(product.getProductQualifiedName()).getFile("module-info.java");
	}
}
