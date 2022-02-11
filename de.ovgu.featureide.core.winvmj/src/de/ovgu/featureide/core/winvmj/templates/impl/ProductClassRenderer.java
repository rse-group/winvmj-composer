package de.ovgu.featureide.core.winvmj.templates.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.google.common.base.CaseFormat;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class ProductClassRenderer extends TemplateRenderer {
	
	private final static String INTERFACE_PATTERN = 
			"([\\S\\s]*)public(\\s+)interface(\\s+)(\\S+)(\\s+)\\{([\\S\\s]*)\\}([\\S\\s]*)";
	

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
			// TODO Auto-generated catch block
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
		IFolder productModuleFolder = project.getBuildFolder().getFolder(product.getProductQualifiedName());
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
	
	public List<Map<String,Object>> getRequiredModels(WinVMJProduct product) throws IOException, CoreException {
		List<String> requiredModules = product.getModules();
		IFile dbRoutingFile = project.getProject().getFile(WinVMJComposer.DB_AND_ROUTING_FILENAME);
		Reader modelReader = new InputStreamReader(dbRoutingFile.getContents());
		Gson gson = new Gson();
		Map<String, Object> configMap = gson.fromJson(modelReader, 
				new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
		
		List<String> requiredModels = gson.fromJson(configMap.get("dataModel").toString(), 
				new TypeToken<List<String>>() {}.getType());
		
		List<Map<String,Object>> models = new ArrayList<>();
		
		for (String module: requiredModels) {
			if (requiredModules.contains(module)) {
				Map<String, Object> modelSpec;
				modelSpec = constructDefaultModelSpec(module, requiredModels);
				modelSpec.put("module", module);
				models.add(modelSpec);
			}
		}
		modelReader.close();
		return models;
	}
	
	private List<Map<String,Object>> getRequiredBindings(WinVMJProduct product) 
			throws IOException, CoreException {
		List<String> requiredModules = product.getModules();
		IFile dbRoutingFile = project.getProject().getFile(WinVMJComposer.DB_AND_ROUTING_FILENAME);
		Reader routingReader = new InputStreamReader(dbRoutingFile.getContents());
		Gson gson = new Gson();
		Map<String, Object> routingMap = gson.fromJson(routingReader, 
				new TypeToken<LinkedHashMap<String, Object>>() {}.getType());
		List<String> requiredRoutings = gson.fromJson(routingMap.get("methodRouting").toString(), 
				new TypeToken<List<String>>() {}.getType());
		
		List<Map<String,Object>> bindings = new ArrayList<>();
		
		for (String module: requiredRoutings) {
			if (requiredModules.contains(module)) {
				Map<String, Object> bindingSpec;
				bindingSpec = constructDefaultBindingSpec(module, requiredRoutings);
				bindings.add(bindingSpec);
			}
		}
		routingReader.close();
		return bindings;
	}
	
	private Map<String, Object> constructDefaultModelSpec(String module, 
			List<String> requiredModels) throws IOException, CoreException {
		Map<String, Object> modelSpec = new HashMap<>();
		String[] splittedModule = module.split("\\.");
		String compClass = this.getModuleInterface(module);
		String endpoint = module.endsWith(".core") ? CaseFormat.LOWER_CAMEL
				.to(CaseFormat.LOWER_HYPHEN, compClass) : splittedModule[2];
		modelSpec.put("crudEndpoint", endpoint);
		modelSpec.put("class", getModuleImplClass(module));
		modelSpec.put("tableName", getDefaultTableNameByModule(module));
		modelSpec.put("hasParentTable", isCoreInMapping(module, requiredModels));
		return modelSpec;
	}
	
	private String getDefaultTableNameByModule(String module) {
		String[] splittedModule = module.split("\\.");
		return String.join("_", Arrays.copyOfRange(splittedModule, 1, splittedModule.length));
	}
	
	private Map<String, Object> constructDefaultBindingSpec(String module, 
			List<String> routingModules) throws IOException, CoreException {
		Map<String, Object> bindingSpec = new HashMap<>();
		bindingSpec.put("interface", getModuleInterface(module));
		bindingSpec.put("factory", getModuleFactoryClass(module));
		bindingSpec.put("module", module);
		bindingSpec.put("class", getModuleImplClass(module));
		
		boolean isBase = isCoreInMapping(module, routingModules);
		bindingSpec.put("isBase", isBase);
		
		String[] splittedModuleName = module.split("\\.");
		String variableName = module.endsWith(".core") ? splittedModuleName[1] : splittedModuleName[2];
		bindingSpec.put("variableName", variableName);
		if (isBase) bindingSpec.put("parentVariable", splittedModuleName[1]);
		
		return bindingSpec;
	}
	
	private List<String> getImports(WinVMJProduct product) throws IOException, CoreException {
		List<String> imports = new ArrayList<>();
		for (String module: product.getModules()) {
			imports.addAll(constructImport(module));
		}
		return imports;
	}
	
	private List<String> constructImport(String module) throws IOException, CoreException {
		List<String> modulesToImport = new ArrayList<>();
		String coreModule = getCoreByModule(module);
		String mainModule = coreModule.replace(".core", "");
		modulesToImport.add(mainModule + "." + getModuleFactoryClass(module));
		modulesToImport.add(coreModule + "." + getModuleInterface(module));
		return modulesToImport;
	}
	
	private String getModuleInterface(String module) throws IOException, CoreException {
		String coreModule = getCoreByModule(module);
		IFolder moduleFolder = project.getProject().getFolder("modules").getFolder(coreModule);
		for (String modulePath: coreModule.split("\\."))
			moduleFolder = moduleFolder.getFolder(modulePath);
		for (IResource moduleResource: moduleFolder.members()) {
			if (moduleResource instanceof IFile && moduleResource.getName().endsWith(".java")) {
				IFile javaFile = (IFile) moduleResource;
				String fileContent = new String(javaFile.getContents().readAllBytes());
				if (fileContent.matches(INTERFACE_PATTERN))
					return Files.getNameWithoutExtension(javaFile.getName());
			}
		}
		String defaultInterfaceName = StringUtils.capitalize(module.split("\\.")[1]);
		WinVMJConsole.println("[WARNING] Interface of " + module + 
				" module not found. Proceed to generate default interface name: " +  
				defaultInterfaceName);
		return defaultInterfaceName;
	}
	
	private boolean isCoreInMapping(String module, List<String> modules) {
		return !module.endsWith(".core") && modules.contains(getCoreByModule(module));
	}
	
	private String getCoreByModule(String module) {
		String[] splittedModule = module.split("\\.");
		splittedModule[splittedModule.length-1] = "core";
		return String.join(".", splittedModule);
	}
	
	private String getModuleFactoryClass(String module) throws IOException, CoreException {
		return getModuleInterface(module) + "Factory";
	}
	
	private String getModuleImplClass(String module) throws IOException, CoreException {
		return getModuleInterface(module) + "Impl";
	}
}
