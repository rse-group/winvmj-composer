package de.ovgu.featureide.core.winvmj.templates.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class ProductClassHibernateRenderer extends TemplateRenderer {
	
	private final static String INTERFACE_PATTERN = 
			"([\\S\\s]*)public(\\s+)interface(\\s+)(\\S+)(\\s+)\\{([\\S\\s]*)\\}([\\S\\s]*)";
	

	public ProductClassHibernateRenderer(IFeatureProject project) {
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
		return "ProductClassHibernate";
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
				modelSpec = constructHibernateModelSpec(module, requiredModels);
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
				bindingSpec = constructHibernateBindingSpec(module, requiredRoutings);
				bindings.add(bindingSpec);
			}
		}
		routingReader.close();
		return bindings;
	}
	
	private Map<String, Object> constructHibernateModelSpec(String module, 
			List<String> requiredModels) throws CoreException {
		Map<String, Object> modelSpec = new HashMap<>();
		modelSpec.put("class", getAllClassInModule(module, "model"));
		return modelSpec;
	}
	
	private Map<String, Object> constructHibernateBindingSpec(String module, 
			List<String> routingModules) throws IOException, CoreException {
		Map<String, Object> bindingSpec = new HashMap<>();
		
		bindingSpec.put("factory", getModuleControllerFactoryClass(getCoreByModule(module)));
		
		bindingSpec.put("module", module);
		bindingSpec.put("class", getModuleInterface(module, "controller"));

		if (module.endsWith(".core")) bindingSpec.put("implClass", getModuleImplClass(module, "controller"));
		else {
			try {
			List<String> deltaClasses = getAllClassInModule(module, "controller");
			String implClass = deltaClasses.size() > 0 ? deltaClasses.get(0) : 
				getModuleImplClass(module);
			bindingSpec.put("implClass", implClass);
			} catch (NullPointerException e) {
				bindingSpec.put("implClass", getModuleImplClass(module));
			}
		}
		
		
		boolean isBase = isCoreInMapping(module, routingModules);
		bindingSpec.put("isBase", isBase);
		
		String[] splittedModuleName = module.split("\\.");
		String variableName = module.endsWith(".core") ? splittedModuleName[1] : splittedModuleName[2];
		bindingSpec.put("variableName", variableName);
		if (isBase) bindingSpec.put("parentVariable", splittedModuleName[1]);
		
		return bindingSpec;
	}
	
	private List<String> getAllClassInModule(String module, 
			String... subDirectories) throws CoreException {
		File moduleDir = new File("src");
		IFolder moduleFolder = project.getProject().getFolder("modules").getFolder(module);
		moduleDir = new File(moduleDir, module);
		for (String modulePath: module.split("\\.")) {
			moduleFolder = moduleFolder.getFolder(modulePath);
		}
		for (String subDir: subDirectories) {
			moduleFolder = moduleFolder.getFolder(subDir);
		}
		List<String> classNames = new ArrayList<>();
		for (IResource classFile: moduleFolder.members()) 
			classNames.add(Files.getNameWithoutExtension(classFile.getName()));
		return classNames;
	}
	
	private Set<String> getImports(WinVMJProduct product) throws IOException, CoreException {
		Set<String> imports = new LinkedHashSet<>();
		for (String module: product.getModules()) {
			imports.addAll(constructImport(module));
		}
		return imports;
	}
	
	private List<String> constructImport(String module) throws IOException, CoreException {
		List<String> modulesToImport = new ArrayList<>();
		String coreModule = getCoreByModule(module);
		String mainModule = coreModule.replace(".core", "");
		modulesToImport.add(mainModule + "." + getModuleControllerFactoryClass(module));
		modulesToImport.add(coreModule + "." + getModuleInterface(module, "controller"));
		return modulesToImport;
	}
	
	private String getModuleInterface(String module) throws IOException, CoreException {
		return getModuleInterface(module, new String[] {}); 
	}
	
	private String getModuleInterface(String module, String... subDirectories) throws IOException, CoreException {
		String coreModule = getCoreByModule(module);
		IFolder moduleFolder = project.getProject().getFolder("modules").getFolder(coreModule);
		for (String modulePath: coreModule.split("\\."))
			moduleFolder = moduleFolder.getFolder(modulePath);
		for (String subDir: subDirectories)
			moduleFolder = moduleFolder.getFolder(subDir);
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
	
	private String getModuleControllerFactoryClass(String module) throws IOException, CoreException {
		return getModuleInterface(module, "controller") + "Factory";
	}
	
	private String getModuleImplClass(String module, String... subDirectories) throws IOException, CoreException {
		return getModuleInterface(module, subDirectories) + "Impl";
	}
}
