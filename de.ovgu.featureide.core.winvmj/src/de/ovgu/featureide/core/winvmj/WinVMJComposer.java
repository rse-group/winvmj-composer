package de.ovgu.featureide.core.winvmj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.ComposerExtensionClass;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.core.impl.MicroserviceProductToCompose;
import de.ovgu.featureide.core.winvmj.core.impl.MultiLevelDeltaComposer;
import de.ovgu.featureide.core.winvmj.core.impl.ProductToCompose;
import de.ovgu.featureide.core.winvmj.internal.InternalResourceManager;
import de.ovgu.featureide.core.winvmj.microservicepreprocessor.ModulePreprocessor;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.MicroserviceProductClassRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ModuleInfoRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ProductClassRenderer;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel.UsedModel;
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
	private Map<String, List<String>> multiLevelDeltaMappings;

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
		
		multiLevelDeltaMappings = null;
		WinVMJProduct product = new ProductToCompose(featureProject, config);
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {
			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				composeProduct(product, featureProject.loadConfiguration(config).getSelectedFeatures());
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compose Product").schedule();
	}
	
	// Micro-services
	public void performFullBuildMicroservices() {
		multiLevelDeltaMappings = null;
		
		Map<String, List<IFeature>> serviceDefinition = Utils.getMicroservicesDefinition(featureProject);
		Map<String, IFolder> allModulesMapping = null;
		try {
			allModulesMapping = Utils.getAllModulesMapping(featureProject);
		} catch (CoreException e) {
			e.printStackTrace();
			return;
		}
		final Map<String, IFolder> finalModulesMapping = allModulesMapping;
		
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {
		    @Override
		    public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
		    	// Clean src directory
		    	IFolder buildDir = featureProject.getBuildFolder();
		    	if (buildDir.exists()) {
		    	    for (IResource resource : buildDir.members()) {
		    	        resource.delete(true, null); 
		    	    }
		    	}
		    	
		    	// Add messaging module to build directory
		    	String messagingModuleName = "vmj.messaging";
				IFolder messagingModule = buildDir.getFolder(messagingModuleName);
				if (!messagingModule.exists()) messagingModule.create(false, true, null);
		        InternalResourceManager.loadResourceDirectory("microservice-preprocessor/" + messagingModuleName, 
		        		messagingModule.getLocation().toOSString());
		    	
		    	Map<String,Integer> modulesCount = new HashMap<String, Integer>();
		    	Map<String, WinVMJProduct> serviceProductMap = new HashMap<String, WinVMJProduct>();
		    	
		    	for (Map.Entry<String, List<IFeature>> entry : serviceDefinition.entrySet()) {
					String productName = entry.getKey();
					List<IFeature> featureList = entry.getValue();
					
					WinVMJProduct product = new MicroserviceProductToCompose(featureProject, productName, 
							featureList, finalModulesMapping, messagingModule);
					serviceProductMap.put(productName, product);
					
					
					for (IFolder module : product.getModules()) {
						String moduleName = module.getName();
						if (moduleName.equals(messagingModuleName)) {
							continue;
						}
						modulesCount.put(moduleName, modulesCount.getOrDefault(moduleName, 0) + 1);
					}
		        }
		    	
		    	Set<String> duplicateModuleNames = new HashSet<>();
		    	for (Map.Entry<String, Integer> entry : modulesCount.entrySet()) {
		    		String module = entry.getKey();
					Integer moduleCount = entry.getValue();
					
					if (moduleCount >= 2) {
						duplicateModuleNames.add(module);
					}
		    	}
		    	
		    	// Compose product module and move module to build folder
		    	for (Map.Entry<String, WinVMJProduct> entry : serviceProductMap.entrySet()) {
		    		String productName = entry.getKey();
		    		WinVMJProduct product = entry.getValue();
		    		List<IFeature> featureList = serviceDefinition.get(productName);
		    		
		    		composeMicroserviceProduct(product, featureList);
		    	}
		    	
		    	// Pre-process duplicate feature module and product module
		    	Set<IFolder> duplicateModules = new HashSet<>();
		    	for (String moduleName : duplicateModuleNames) {
		    		IFolder module = featureProject.getBuildFolder().getFolder(moduleName);
		    		duplicateModules.add(module);
		    	}
		    	
		        ModulePreprocessor.modifyServiceImplClass(duplicateModules);
		        
		        for (WinVMJProduct product : serviceProductMap.values()) {
		        	IFolder productModule = featureProject.getBuildFolder()
							.getFolder(product.getProductQualifiedName());
		        	
		        	Set<IFolder> duplicateModulesOnProduct = new HashSet<>();
		        	for (IFolder module : product.getModules()) {
		        		if (duplicateModuleNames.contains(module.getName())) {
		        			duplicateModulesOnProduct.add(module);
		        		}
		        	}
		        	
			        ModulePreprocessor.modifyProductModule(duplicateModulesOnProduct,productModule, product.getProductName());
		    	}
		        
		        WinVMJConsole.println("Completed compose microservices product");
		    	
		        return true;
		    }
		};

		LongRunningWrapper.getRunner(job, "Compose Products in Microservices").schedule();
	}
	
	
	private void composeMicroserviceProduct(WinVMJProduct product, List<IFeature> selectedFeatures) 
	{
		try {
			selectModulesFromProject(featureProject, product);
			IFolder productModule = featureProject.getBuildFolder()
					.getFolder(product.getProductQualifiedName());
			if (!productModule.exists()) productModule.create(false, true, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		List<String> selectedFeaturesForRouting = new ArrayList<String>();
		for (IFeature feature : selectedFeatures) {
			selectedFeaturesForRouting.add(feature.getName());
		}
		
		TemplateRenderer productClassRenderer = new MicroserviceProductClassRenderer(featureProject, selectedFeaturesForRouting);
		TemplateRenderer moduleInfoRenderer = new ModuleInfoRenderer(
				featureProject, multiLevelDeltaMappings);
		
		productClassRenderer.render(product);
		moduleInfoRenderer.render(product);
		
	}
	
	private void composeProduct(WinVMJProduct product, List<IFeature> features) {
		try {
			selectModulesFromProject(featureProject, product);
			checkMultiLevelDelta(
				featureProject,
				product,
				features
			);
			IFolder productModule = featureProject.getBuildFolder()
					.getFolder(product.getProductQualifiedName());
			if (!productModule.exists()) productModule.create(false, true, null);
		} catch (CoreException | ParserException e) {
			e.printStackTrace();
		}
		
		TemplateRenderer moduleInfoRenderer = new ModuleInfoRenderer(
			featureProject, multiLevelDeltaMappings);
		TemplateRenderer productClassRenderer = new ProductClassRenderer(featureProject);
		moduleInfoRenderer.render(product);
		productClassRenderer.render(product);
	}
	
	@Override
	public boolean supportsPartialFeatureProject() {
		return true;
	}
	
	@Override
	public void buildPartialFeatureProjectAssets(IFolder sourceFolder, ArrayList<String> removedFeatures, ArrayList<String> mandatoryFeatures)
			throws IOException, CoreException {}

	
	@Override
	public boolean hasFeatureFolder() {
		return false;
	}
	
	@Override
	public IFeatureModelFormat getFeatureModelFormat() {
		return new UVLFeatureModelFormat();
	}
	
	private void selectModulesFromProject(
		IFeatureProject project, WinVMJProduct product) throws CoreException {
		for (IFolder sourceModule: product.getModules()) {
			IFolder destModule = project.getBuildFolder().getFolder(sourceModule.getName());
			if (!destModule.exists()) destModule.create(false, true, null);
			copy(sourceModule, destModule);
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

	private void checkMultiLevelDelta(
		IFeatureProject project,
		WinVMJProduct product,
		List<IFeature> features
	) throws CoreException, ParserException {
		checkExternalMultiLevelDelta(project, product, features);
		processMultiLevelDelta(project, product, features);
	}

	private void checkExternalMultiLevelDelta(
		IFeatureProject project,
		WinVMJProduct product,
		List<IFeature> features
	) throws CoreException, ParserException {
		CorePlugin.getDefault();
		Map<String, IFeatureProject> refProjectMap =
				Stream.of(project.getProject().getReferencedProjects())
				.map(pr -> CorePlugin.getFeatureProject(pr))
				.collect(Collectors.toMap(pr -> Utils.getSplName(pr), Function.identity()));
		
		MultiFeatureModel multiFetureModel = (MultiFeatureModel) project.getFeatureModel();
		if (multiFetureModel.isMultiProductLineModel()) {
			for (Entry<String, UsedModel> interfaceModel: multiFetureModel
					.getExternalModels().entrySet()) {
				String externalSplName = interfaceModel.getValue()
						 .getModelName().replace("interfaces.", "");
				IFeatureProject refProject = refProjectMap.get(externalSplName);
				List<IFeature> externalFeatures = features.stream()
					    .filter(f -> f.getName().startsWith(interfaceModel.getKey() + "."))
					    .<IFeature>map(f -> new Feature(refProject.getFeatureModel(), 
					        f.getName().replace(interfaceModel.getKey() + ".", "")))
					    .collect(Collectors.toList());
				List<String> relatedProducts = Utils.getRelatedProducts(
					project, externalSplName, product.getProductName());
				externalFeatures.addAll(Utils.selectFeaturesFromRelatedProducts(externalSplName, 
						refProject, relatedProducts));
				checkMultiLevelDelta(refProject, product, externalFeatures);
			}
		}
	}

	private void processMultiLevelDelta(
		IFeatureProject project,
		WinVMJProduct product,
		List<IFeature> features
	) throws CoreException, ParserException {
		IFile featureToModuleMapper = project.getProject()
				.getFile(FEATURE_MODULE_MAPPER_FILENAME);
		final FormulaFactory formulaFactory = new FormulaFactory();
		final PropositionalParser formulaParser = new PropositionalParser(formulaFactory);

		Assignment assignment = Utils.getFeatureCheckingAssignment(features, formulaFactory);
		Reader mapReader =  new InputStreamReader(featureToModuleMapper.getContents());
		Gson gson = new Gson();
		Map<String, List<String>> mappings;
		try {
			mappings = gson.fromJson(mapReader, 
					new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
		} catch (NullPointerException e) {
			mappings = new LinkedHashMap<String, List<String>>();
		}

		for (Entry<String, List<String>> mapping: mappings.entrySet()) {
			String key = mapping.getKey();
			List<String> value = mapping.getValue();
			if (
				Utils.evaluate(assignment, formulaParser, key) &&
				Utils.isMultiLevelDelta(mapping)
			) {
				MultiLevelDeltaComposer multiLevelDeltaComposer = new MultiLevelDeltaComposer(
					featureProject, product, key, value);
				multiLevelDeltaComposer.compose();

				if (multiLevelDeltaMappings == null) multiLevelDeltaMappings = new HashMap<>();
				multiLevelDeltaMappings.put(key, value);
			}
		}
	}
}
