package de.ovgu.featureide.core.winvmj.templates.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

	private final static String PREFIX_AUTH_MODEL_PRODUCT = "auth";
	
	public ProductClassRenderer(IFeatureProject project) {
		super(project);
	}

	@Override
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("productPackage", product.getProductQualifiedName());
		dataModel.put("productName", product.getProductName());
		dataModel.put("defaultAuthModel", checkDefaultAuthModel(product));
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

	private boolean checkDefaultAuthModel(WinVMJProduct product) {
		for (String module : product.getModuleNames()) {
			if (module.startsWith(PREFIX_AUTH_MODEL_PRODUCT))
				return false;
		}
		return true;
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
	
	private List<List<Map<String, Object>>> getRequiredBindings(WinVMJProduct product) 
			throws IOException, CoreException {
		List<List<Map<String, Object>>> bindings = new ArrayList<>();
		
		for (String module: product.getModuleNames()) {
			List<Map<String, Object>> bindingSpec = constructBindingSpec(module);
			if (bindingSpec != null) bindings.add(bindingSpec);
		}
		return bindings;
	}
	
	
	private List<Map<String, Object>> constructBindingSpec(String module) 
			throws IOException, CoreException {
		
		String implClass = getCoreImplClass(module);
		boolean isCoreConstructed = true;

		if (implClass == null) {
			List<String> listImplClass = getListModuleImplClass(module, CONTROLLER_FOLDERNAME);
			if (listImplClass.size() == 0) return null;
			isCoreConstructed = false;
			implClass = listImplClass.get(0);
		}
		
		List<String> fileNames = getListModuleImplClass(module, CONTROLLER_FOLDERNAME);
		if (fileNames.size() == 0) return null;
		String fileName = fileNames.get(0);
		
		List<Map<String, Object>> listBindingSpec = new ArrayList<>();

		Map<String, Object> bindingSpec = new HashMap<>();
		String baseClass = implClass.replace("Impl", "");

		bindingSpec.put("factory", baseClass + "Factory");
		bindingSpec.put("module", module);
		bindingSpec.put("class", baseClass);
		bindingSpec.put("implClass", fileName);

		if (!isCoreModule(module) && isCoreConstructed) {

			String upperLevelModule = getUpperLevelModuleName(module);

			if (upperLevelModule != null) {
				bindingSpec.put("wrappedVariableName", upperLevelModule + baseClass);
			}

		}

		String[] splittedModuleName = module.split("\\.");
		String variableName = module.endsWith(".core") ? 
				splittedModuleName[splittedModuleName.length - 2] : splittedModuleName[splittedModuleName.length - 1];
		bindingSpec.put("variableName", variableName + baseClass);

		listBindingSpec.add(bindingSpec);
		
		
		return listBindingSpec;
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
		for (String moduleInterface : getListModuleInterface(module, CONTROLLER_FOLDERNAME)) {
			modulesToImport.add(mainModule + "." + moduleInterface + "Factory");
			modulesToImport.add(coreModule + "." + moduleInterface);
		}
		return modulesToImport;
	}

//	returns nama file java tanpa extensi 
	private List<String> getListJavaCompOnModuleByContentPattern(IFolder module, String pattern) {
		List<String> fileResources = new ArrayList<>();
		try {
			for (IResource moduleResource: module.members()) {
				if (moduleResource instanceof IFile && moduleResource.getName().endsWith(".java")) {
					IFile javaFile = (IFile) moduleResource;
					String fileContent = new String(javaFile.getContents().readAllBytes());
					if (fileContent.matches(pattern))
						fileResources.add(FilenameUtils.getBaseName(javaFile.getName()));
				}
			}
		} catch (IOException | CoreException e) { }
		return fileResources;
	}
	
	private IFolder getArtifactDirectoryOfModule(String module, String... subDirectories) {
		IFolder moduleFolder = project.getBuildFolder().getFolder(module);
		for (String modulePath: module.split("\\."))
			moduleFolder = moduleFolder.getFolder(modulePath);
		for (String subDir: subDirectories)
			moduleFolder = moduleFolder.getFolder(subDir);
		return moduleFolder;
	}
	
	private List<String> getListModuleInterface(String module, String... subDirectories) {
		String coreModule = getCoreByModule(module);
		IFolder moduleFolder = getArtifactDirectoryOfModule(coreModule, subDirectories);
		List<String> listInterfaceName = getListJavaCompOnModuleByContentPattern(moduleFolder, INTERFACE_PATTERN);
		return listInterfaceName;
	}

	private List<String> getListModuleImplClass(String module, String... subDirectories) {
		IFolder moduleFolder = getArtifactDirectoryOfModule(module, subDirectories);
		return getListJavaCompOnModuleByContentPattern(moduleFolder, CONCRETE_CLASS_PATTERN);
	}

	private String getModuleImplClass(String module, String implClass, String... subDirectories) {
		List<String> listImplName = getListModuleImplClass(module, subDirectories);
		for (String implName : listImplName) {
			if (implName.endsWith(implClass))
				return implName;
		}  
		return null;
	}
	
	private boolean isCoreModule(String module) {
		return module.endsWith(".core");
	}
	
	private String getCoreByModule(String module) {
		String[] splittedModule = module.split("\\.");
		splittedModule[splittedModule.length-1] = "core";
		return String.join(".", splittedModule);
	}
	
	private String getUpperLevelModuleName(String module) {
		String withCore = getCoreByModule(module);
		if (isModuleExist(withCore)) {
			String[] splittedModule = withCore.split("\\.");
			return splittedModule[splittedModule.length-2];
		}
		String[] splittedModule = module.split("\\.");
		String[] upperLevelArray = Arrays.copyOfRange(splittedModule, 0, splittedModule.length-1);
		if (isModuleExist(String.join(".", upperLevelArray))) {
			return upperLevelArray[upperLevelArray.length-1];
		}
		return null;
	}
	
	private boolean isModuleExist(String module) {
		return project.getBuildFolder().getFolder(module).exists();
	}
	
	private String getCoreImplClass(String module) {
		try {
			if (module.endsWith(".core"))
				return getListModuleImplClass(module, CONTROLLER_FOLDERNAME).get(0);
			String[] splittedModule = module.split("\\.");
			while (true) {   
				String coreModule = getCoreByModule(String.join(".", splittedModule));
				if (isModuleExist(coreModule)) {
					return getListModuleImplClass(coreModule, CONTROLLER_FOLDERNAME).get(0);
				}
				splittedModule = Arrays.copyOfRange(splittedModule, 0, splittedModule.length-1);
				if (splittedModule.length < 3) break;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
		
	}
}
