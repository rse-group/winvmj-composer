package de.ovgu.featureide.core.winvmj.templates.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.configuration.Configuration;


public class ProductClassRenderer extends TemplateRenderer {

	private final static String INTERFACE_PATTERN = "([\\S\\s]*)public(\\s+)interface(\\s+)(\\S+)(\\s+)\\{([\\S\\s]*)\\}([\\S\\s]*)";

	private final static String CONCRETE_CLASS_PATTERN = "([\\S\\s]*)public(\\s+)class(\\s+)[(\\S+)(\\s+)]*\\{([\\S\\s]*)\\}([\\S\\s]*)";

	private final static String CONTROLLER_FOLDERNAME = "resource";
	private final static String SERVICE_FOLDERNAME = "service";
	private final static String MODEL_FOLDERNAME = "model";

	private final static String PREFIX_AUTH_MODEL_PRODUCT = "auth";

	public static String FEATURE_MODULE_MAPPER_FILENAME = "feature_to_module.json";
	private Map<String, List<String>> featureToModuleMap;
	private List<String> selectedFeature;
	private Map<String, Integer> variableNameCounts = new HashMap<>();

	public ProductClassRenderer(IFeatureProject project) {
		super(project);
		try {
			loadFeatureToModuleMap(project);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		getSelectedFeature(project);
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
			dataModel.put(
				"featureTableMappings", getRequiredFeatureTableMappings(product));
			dataModel.put("routings", getRequiredBindings(product));
		} catch (IOException | CoreException e) {
			WinVMJConsole.println(e.getMessage());
			for (StackTraceElement em : e.getStackTrace())
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

	private void loadFeatureToModuleMap(IFeatureProject project) throws CoreException {
        IFile mapFile = project.getProject().getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME);
        if (mapFile.exists()) {
            try (Reader mapReader = new InputStreamReader(mapFile.getContents())) {
                Gson gson = new Gson();
                featureToModuleMap = gson.fromJson(mapReader,
                    new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Feature to module map file does not exist");
            featureToModuleMap = new LinkedHashMap<>();
        }
    }
	
	private void getSelectedFeature(IFeatureProject winVmjProject) {
		Configuration config = winVmjProject.loadCurrentConfiguration();
		Set<String> features = winVmjProject.loadCurrentConfiguration().getSelectedFeatureNames();
		
	    
	    selectedFeature = new ArrayList<>(features);
	}

	private List<Map<String,Object>> getRequiredFeatureTableMappings(WinVMJProduct product) 
            throws IOException, CoreException {
        List<Map<String,Object>> featureTableMappings = new ArrayList<>();
        
        for (String module: product.getModuleNames()) {
            if (getArtifactDirectoryOfModule(module, MODEL_FOLDERNAME).exists()) {
                String featureName = getFeatureName((String) module);
				Map<String, Object> featureTableMapping = getFeatureTableMapping(
					featureTableMappings, featureName);

                if (isCoreModule(module)) {
                    if (featureTableMapping == null) {
                        String componentClassName = featureName.substring(
							0, 1).toUpperCase() + featureName.substring(
								1).toLowerCase() + "Component";
						featureTableMapping = new HashMap<>();
                        featureTableMapping.put(
							"component", module + "." + componentClassName);
                        featureTableMapping.put("deltas", new ArrayList<String>());
                        featureTableMappings.add(featureTableMapping);
                    }
                } else {
                    if (featureTableMapping != null) {
                        List<String> deltas = (List<String>) featureTableMapping.get("deltas");
                        String delta = String.format("%s.%s", module, getListModuleImplClass(
                                module, MODEL_FOLDERNAME).get(0));
                        if (!deltas.contains(delta)) {
                            deltas.add(delta);
                        }
                    }
                }
            }
        }

        return featureTableMappings;
    }

	private List<List<Map<String, Object>>> getRequiredBindings(WinVMJProduct product)
			throws IOException, CoreException {
		List<List<Map<String, Object>>> bindings = new ArrayList<>();

		Set<String> productModules = new HashSet<>(product.getModuleNames());

		// Iterate over the entries of the featureToModuleMap
		for (Map.Entry<String, List<String>> entry : featureToModuleMap.entrySet()) {
			String feature = entry.getKey();
	        if (selectedFeature.contains(feature)) {
				List<String> modules = entry.getValue();
				for (String module : modules) {
					if (productModules.contains(module)) {
						try {
							List<Map<String, Object>> bindingSpec = constructBindingSpec(module, feature);
							if (bindingSpec != null) {
								bindings.add(bindingSpec);
							}
						} catch (IOException | CoreException e) {
							e.printStackTrace();
						}
					}
				}
	        }
		}
		return bindings;
	}

	
	private List<Map<String, Object>> constructBindingSpec(String module, String feature)
			throws IOException, CoreException {

		List<Map<String, Object>> listBindingSpec = new ArrayList<>();

		// Check if the controller and service folder exists before constructing the spec
		boolean isControllerExist = checkArtifactDirectoryOfModule(module, CONTROLLER_FOLDERNAME);
		boolean isServiceExist = checkArtifactDirectoryOfModule(module, SERVICE_FOLDERNAME);

		if (isControllerExist && isServiceExist){
			Map<String, Object> controllerSpec = constructComponentSpec(module, feature, CONTROLLER_FOLDERNAME);
			if (controllerSpec != null) {
				controllerSpec.put("notSingleStructured", true);
				listBindingSpec.add(controllerSpec);
			}
			Map<String, Object> serviceSpec = constructComponentSpec(module, feature, SERVICE_FOLDERNAME);
			if (serviceSpec != null) {
				listBindingSpec.add(serviceSpec);
			}
		// Only Resource structure support
		} else if (isControllerExist) {
			Map<String, Object> controllerSpec = constructComponentSpec(module, feature, CONTROLLER_FOLDERNAME);
			if (controllerSpec != null) {
				listBindingSpec.add(controllerSpec);
			}
		}
		return listBindingSpec.isEmpty() ? null : listBindingSpec;
	}

	private Map<String, Object> constructComponentSpec(String module, String feature, String componentType)
			throws IOException, CoreException {
		String implClass = getCoreImplClass(module, componentType);
		boolean isCoreConstructed = true;
		String componentTypeCap = componentType.equals("service")
									? "Service"
									: "Resource";

		if (implClass == null) {
			List<String> listImplClass = getListModuleImplClass(module, componentType);
			if (listImplClass.isEmpty())
				return null;
			isCoreConstructed = false;
			implClass = listImplClass.get(0);
		}

		List<String> fileNames = getListModuleImplClass(module, componentType);
		if (fileNames.isEmpty())
			return null;
		String fileName = fileNames.get(0);
		
		
		List<Map<String, Object>> listBindingSpec = new ArrayList<>();

		Map<String, Object> bindingSpec = new HashMap<>();
		String baseClass = implClass.replace("Impl", ""); 
		String featureClass = componentType.equals("service")
				? baseClass.replace("Service", "")
				: baseClass.replace("Resource", "");
		bindingSpec.put("factory", baseClass + "Factory");
		bindingSpec.put("module", module);
		bindingSpec.put("class", baseClass);
		bindingSpec.put("implClass", fileName);
		bindingSpec.put("componentType", componentType);

		String variableName = getVariableNameFromModule(module) + featureClass; 
    	bindingSpec.put("variableName", addUniqueVariableName(variableName, componentTypeCap) + componentTypeCap); 

		if (!isCoreModule(module) && isCoreConstructed) {
			String upperLevelModule = getUpperLevelModuleName(feature, module);
			if (upperLevelModule != null) {
				String wrappedVariableName = getVariableNameFromModule(upperLevelModule) + featureClass; 
				bindingSpec.put("wrappedVariableName", getUniqueVariableName(wrappedVariableName, componentTypeCap));
			} else {
				String coreModule = getCoreByModule(module);
				String wrappedVariableName = getVariableNameFromModule(coreModule) + featureClass; 
				bindingSpec.put("wrappedVariableName", getUniqueVariableName(wrappedVariableName, componentTypeCap));
			}
		}

		return bindingSpec;
	}

	// Helps distinguish variableName if a delta is being used multiple times in creating a feature
	private String getUniqueVariableName(String baseName, String componentType) { 
		int count = variableNameCounts.getOrDefault(baseName + componentType, 0); 
		return count == 1 ? baseName : baseName + count;
	}

	private String addUniqueVariableName(String baseName, String componentType) {  
		int count = variableNameCounts.getOrDefault(baseName + componentType, 0) + 1;
		variableNameCounts.put(baseName + componentType, count); 
		return count == 1 ? baseName : baseName + count; 
	}

	private String getFeatureForModule(String module) {
		for (Map.Entry<String, List<String>> entry : featureToModuleMap.entrySet()) {
			if (entry.getValue().contains(module)) {
				return entry.getKey();
			}
		}
		return null;
	}

	private List<String> getAllClassInModule(String module,
			String... subDirectories) throws CoreException {
		IFolder moduleFolder = project.getBuildFolder().getFolder(module);
		for (String modulePath : module.split("\\.")) {
			moduleFolder = moduleFolder.getFolder(modulePath);
		}
		for (String subDir : subDirectories) {
			moduleFolder = moduleFolder.getFolder(subDir);
		}
		List<String> classNames = new ArrayList<>();
		for (IResource classFile : moduleFolder.members()) {
			if (classFile.getName().endsWith(".java")) {
				classNames.add(FilenameUtils.getBaseName(classFile.getName()));
			}
		}
		return classNames;
	}

	private Set<String> getImports(WinVMJProduct product) throws IOException, CoreException {
		Set<String> imports = new LinkedHashSet<>();
		for (String module : product.getModuleNames()) {
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
		for (String moduleInterface : getListModuleInterface(module, SERVICE_FOLDERNAME)) { 
			modulesToImport.add(mainModule + "." + moduleInterface + "Factory");
			modulesToImport.add(coreModule + "." + moduleInterface);
		}
		return modulesToImport;
	}

	// returns nama file java tanpa extensi
	private List<String> getListJavaCompOnModuleByContentPattern(IFolder module, String pattern) {
		List<String> fileResources = new ArrayList<>();
		try {
			for (IResource moduleResource : module.members()) {
				if (moduleResource instanceof IFile && moduleResource.getName().endsWith(".java")) {
					IFile javaFile = (IFile) moduleResource;
					String fileContent = new String(javaFile.getContents().readAllBytes());
					if (fileContent.matches(pattern))
						fileResources.add(FilenameUtils.getBaseName(javaFile.getName()));
				}
			}
		} catch (IOException | CoreException e) {
		}
		return fileResources;
	}

	private IFolder getArtifactDirectoryOfModule(String module, String... subDirectories) {
		IFolder moduleFolder = project.getBuildFolder().getFolder(module);
		for (String modulePath : module.split("\\."))
			moduleFolder = moduleFolder.getFolder(modulePath);
		for (String subDir : subDirectories)
			moduleFolder = moduleFolder.getFolder(subDir);
		return moduleFolder;
	}

	// To check whether the folder of subdirectories exist or not
	private boolean checkArtifactDirectoryOfModule(String module, String... subDirectories) {
		IFolder moduleFolder = project.getBuildFolder().getFolder(module);
		for (String modulePath : module.split("\\.")) {
			moduleFolder = moduleFolder.getFolder(modulePath);
		}
		for (String subDir : subDirectories) {
			moduleFolder = moduleFolder.getFolder(subDir);
		}
		return moduleFolder.exists();
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
		splittedModule[splittedModule.length - 1] = "core";
		return String.join(".", splittedModule);
	}

	private String getUpperLevelModuleName(String feature, String currentModule) {
		List<String> modules = featureToModuleMap.get(feature);
		if (modules != null && !modules.isEmpty()) {
			int currentIndex = modules.indexOf(currentModule);
			if (currentIndex > 0) {
				return modules.get(currentIndex - 1);
			}
		}
		return null;
	}

	private boolean isModuleExist(String module) {
		return project.getBuildFolder().getFolder(module).exists();
	}

	private String getCoreImplClass(String module, String componentType) throws CoreException {
		if (module.endsWith(".core")) {
			List<String> implClasses = getListModuleImplClass(module, componentType);
			return implClasses.isEmpty() ? null : implClasses.get(0);
		}

		String[] splittedModule = module.split("\\.");
		while (splittedModule.length >= 3) {
			String coreModule = getCoreByModule(String.join(".", splittedModule));
			if (isModuleExist(coreModule)) {
				List<String> implClasses = getListModuleImplClass(coreModule, componentType);
				return implClasses.isEmpty() ? null : implClasses.get(0);
			}
			splittedModule = Arrays.copyOfRange(splittedModule, 0, splittedModule.length - 1);
		}
		return null;
	}

	private String getVariableNameFromModule(String module) {
        String[] splittedModuleName = module.split("\\.");
        return module.endsWith(".core") ?
            splittedModuleName[splittedModuleName.length - 2] :
            splittedModuleName[splittedModuleName.length - 1];
    }

	private String getFeatureName(String module) {
		String[] moduleParts = module.split("\\.");
		return moduleParts[1];
	}

	private Map<String, Object> getFeatureTableMapping(
		List<Map<String,Object>> featureTableMappings, 
		String featureName
	) {
		for (Map<String, Object> featureTableMapping: featureTableMappings) {
			if (
				getFeatureName(
					(String) featureTableMapping.get("component")
				).equals(featureName)
			) {
				return featureTableMapping;
			}
		}

		return null;
	}
}