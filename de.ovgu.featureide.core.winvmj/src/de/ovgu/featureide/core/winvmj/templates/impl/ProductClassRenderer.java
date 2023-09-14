package de.ovgu.featureide.core.winvmj.templates.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class ProductClassRenderer extends TemplateRenderer {
	
	private final static String INTERFACE_PATTERN = 
			"([\\S\\s]*)public(\\s+)interface(\\s+)(\\S+)(\\s+)\\{([\\S\\s]*)\\}([\\S\\s]*)";
	
	private final static String CONCRETE_CLASS_PATTERN = 
			"([\\S\\s]*)public(\\s+)class(\\s+)[(\\S+)(\\s+)]*\\{([\\S\\s]*)\\}([\\S\\s]*)";
	
	private final static String CONTROLLER_FOLDERNAME = "resource";
	private final static String MODEL_FOLDERNAME = "model";
	
	public ProductClassRenderer(IFeatureProject project) {
		super(project);
	}

	@Override
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("productPackage", product.getProductQualifiedName());
		dataModel.put("productName", product.getProductName());
		try {
			dataModel.put("imports", getImports(product));
			dataModel.put("models", getRequiredModels(product));
			dataModel.put("routings", getRequiredBindings(product));
		} catch (IOException | CoreException e) {
			WinVMJConsole.println(e.getMessage());
			for (StackTraceElement em: e.getStackTrace())
				WinVMJConsole.println(em.toString());
			e.printStackTrace();
		}
		
		return dataModel;
	}

	@Override
	protected String loadTemplateFilename() {
		return "ProductClass";
	}

	@Override
	protected IFile getOutputFile(WinVMJProduct product) {
		IFolder productModuleFolder = project.getBuildFolder()
				.getFolder(product.getProductQualifiedName());
		for (String modulePath: product.getProductQualifiedName().split("\\.")) {
			productModuleFolder = productModuleFolder.getFolder(modulePath);
			if (!productModuleFolder.exists())
				try {
					productModuleFolder.create(false, true, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}
		return productModuleFolder.getFile(product.getProductName() + ".java");
	}
	
	private List<Map<String,Object>> getRequiredModels(WinVMJProduct product) 
			throws IOException, CoreException {
		List<Map<String,Object>> models = new ArrayList<>();
		
		for (String module: product.getModuleNames()) {
			if (getArtifactDirectoryOfModule(module, MODEL_FOLDERNAME).exists()) {
				Map<String, Object> modelSpec = new HashMap<>();
				modelSpec.put("class", getAllClassInModule(module, MODEL_FOLDERNAME));
				modelSpec.put("module", module);
				models.add(modelSpec);
			}
		}
		return models;
	}
	
	private List<Map<String,Object>> getRequiredBindings(WinVMJProduct product) 
			throws IOException, CoreException {
		List<Map<String,Object>> bindings = new ArrayList<>();
		
		for (String module: product.getModuleNames()) {
			Map<String, Object> bindingSpec = constructBindingSpec(module);
			if (bindingSpec != null) bindings.add(bindingSpec);
		}
		return bindings;
	}
	
	private Map<String, Object> constructBindingSpec(String module) 
			throws IOException, CoreException {
		String implClass = getModuleImplClass(module, CONTROLLER_FOLDERNAME);
		if (implClass == null) return null;
		
		Map<String, Object> bindingSpec = new HashMap<>();
		
		bindingSpec.put("factory", getModuleControllerFactoryClass(getCoreByModule(module)));
		bindingSpec.put("module", module);
		bindingSpec.put("class", getModuleInterface(module, CONTROLLER_FOLDERNAME));
		bindingSpec.put("implClass", implClass);
		
		if (!isCoreModule(module)) {
			String coreModule = getCoreByModule(module);
			String coreImplClass = getModuleImplClass(coreModule, CONTROLLER_FOLDERNAME);
			if (coreImplClass != null) {
				bindingSpec.put("coreModule", coreModule);
				bindingSpec.put("coreImplClass", coreImplClass);
			}
		}
		
		String[] splittedModuleName = module.split("\\.");
		String variableName = module.endsWith(".core") ? 
				splittedModuleName[1] : splittedModuleName[2];
		bindingSpec.put("variableName", variableName);
		
		return bindingSpec;
	}
	
	private List<String> getAllClassInModule(String module, 
			String... subDirectories) throws CoreException {
		IFolder moduleFolder = project.getBuildFolder().getFolder(module);
		for (String modulePath: module.split("\\.")) {
			moduleFolder = moduleFolder.getFolder(modulePath);
		}
		for (String subDir: subDirectories) {
			moduleFolder = moduleFolder.getFolder(subDir);
		}
		List<String> classNames = new ArrayList<>();
		for (IResource classFile: moduleFolder.members()) 
			classNames.add(FilenameUtils.getBaseName(classFile.getName()));
		return classNames;
	}
	
	private Set<String> getImports(WinVMJProduct product) throws IOException, CoreException {
		Set<String> imports = new LinkedHashSet<>();
		for (String module: product.getModuleNames()) {
			imports.addAll(constructImport(module));
		}
		return imports;
	}
	
	private List<String> constructImport(String module) throws IOException, CoreException {
		List<String> modulesToImport = new ArrayList<>();
		String coreModule = getCoreByModule(module);
		String mainModule = coreModule.replace(".core", "");
		modulesToImport.add(mainModule + "." + getModuleControllerFactoryClass(module));
		modulesToImport.add(coreModule + "." + getModuleInterface(module, CONTROLLER_FOLDERNAME));
		return modulesToImport;
	}
	
	private String getJavaCompOnModuleByContentPattern(IFolder module,
			String pattern) throws IOException, CoreException {
		for (IResource moduleResource: module.members()) {
			if (moduleResource instanceof IFile && moduleResource.getName().endsWith(".java")) {
				IFile javaFile = (IFile) moduleResource;
				String fileContent = new String(javaFile.getContents().readAllBytes());
				if (fileContent.matches(pattern))
					return FilenameUtils.getBaseName(javaFile.getName());
			}
		}
		return null;
	}
	
	private IFolder getArtifactDirectoryOfModule(String module, String... subDirectories) {
		IFolder moduleFolder = project.getBuildFolder().getFolder(module);
		for (String modulePath: module.split("\\."))
			moduleFolder = moduleFolder.getFolder(modulePath);
		for (String subDir: subDirectories)
			moduleFolder = moduleFolder.getFolder(subDir);
		return moduleFolder;
	}
	
	private String getModuleInterface(String module, String... subDirectories) 
			throws IOException, CoreException {
		String coreModule = getCoreByModule(module);
		IFolder moduleFolder = getArtifactDirectoryOfModule(coreModule, subDirectories);
		String interfaceName = getJavaCompOnModuleByContentPattern(moduleFolder, INTERFACE_PATTERN);
		if (interfaceName != null) return interfaceName;
		interfaceName = StringUtils.capitalize(module.split("\\.")[1]);
		WinVMJConsole.println("[WARNING] Interface of " + module + 
				" module not found. Proceed to generate default interface name: " +  
				interfaceName);
		return interfaceName;
	}
	
	private String getModuleImplClass(String module, String... subDirectories) 
			throws IOException, CoreException {
		IFolder moduleFolder =  getArtifactDirectoryOfModule(module, subDirectories);
		return getJavaCompOnModuleByContentPattern(moduleFolder, CONCRETE_CLASS_PATTERN);
	}
	
	private boolean isCoreModule(String module) {
		return module.endsWith(".core");
	}
	
	private String getCoreByModule(String module) {
		String[] splittedModule = module.split("\\.");
		splittedModule[splittedModule.length-1] = "core";
		return String.join(".", splittedModule);
	}
	
	private String getModuleControllerFactoryClass(String module) 
			throws IOException, CoreException {
		return getModuleInterface(module, CONTROLLER_FOLDERNAME) + "Factory";
	}
}
