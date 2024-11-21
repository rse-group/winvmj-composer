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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.units.qual.kmPERh;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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
import de.ovgu.featureide.core.winvmj.core.impl.MultiLevelDeltaComposer;
import de.ovgu.featureide.core.winvmj.core.impl.ProductToCompose;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
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
				composeProduct(product, config);
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compose Product").schedule();
	}
	
	private void composeProduct(WinVMJProduct product, Path config) {
		try {
			selectModulesFromProject(featureProject, product);
			checkMultiLevelDelta(
				featureProject,
				product,
				featureProject.loadConfiguration(config).getSelectedFeatures()
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
