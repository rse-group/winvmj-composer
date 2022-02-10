package de.ovgu.featureide.core.winvmj.templates.impl;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class ProductClassRenderer extends TemplateRenderer {
	
	public ProductClassRenderer(IFeatureProject project) {
		super(project);
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("productPackage", product.getProductQualifiedName());
		dataModel.put("productName", product.getProductName());
		
		return dataModel;
	}
	
	protected String loadTemplateFilename() {
		return "ProductClassTemplate";
	}
	
	protected IFile getOutputFile(WinVMJProduct product) {
		IFolder productModuleFolder = project.getBuildFolder().getFolder(product.getProductQualifiedName());
		for (String modulePath: product.getProductQualifiedName().split("\\.")) {
			productModuleFolder = productModuleFolder.getFolder(modulePath);
			if (!productModuleFolder.exists())
				try {
					productModuleFolder.create(false, true, null);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return productModuleFolder.getFile(product.getProductName() + ".java");
	}
	
	private void extractImports(Map<String, Object> mapping, List<String> imports) {
		if (mapping.containsKey("imports") && mapping.get("imports") instanceof List) { 
			List<String> modulesToImport = (List<String>) mapping.remove("imports");
			imports.addAll(modulesToImport);
		}
	}
	
	private String getModuleInterface(String module) throws MalformedURLException, ClassNotFoundException, CoreException {
		IFolder moduleFolder = project.getProject().getFolder("modules").getFolder(module);
		for (String modulePath: module.split("\\."))
			moduleFolder = moduleFolder.getFolder(modulePath);
		for (IResource moduleResource: moduleFolder.members()) {
			if (moduleResource instanceof IFile && moduleResource.getName().endsWith(".java")) {
				IFile javaFile = (IFile) moduleResource;
				// TODO: try to check if its interface or not
			}
		}
		return module;
	}

}
