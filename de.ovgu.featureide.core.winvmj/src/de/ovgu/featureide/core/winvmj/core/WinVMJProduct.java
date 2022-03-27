package de.ovgu.featureide.core.winvmj.core;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel.UsedModel;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;

public class WinVMJProduct {
	
	private List<IFolder> modules;
	private String productName;
	private String splName;
	
	public WinVMJProduct(IFeatureProject featureProject, Path config) {
		this.productName = getProductName(config);
		this.splName = getSplName(featureProject);
		try {
			this.modules = selectModules(featureProject, featureProject
					.loadConfiguration(config).getSelectedFeatures());
		} catch (CoreException | ParserException e) {
			this.modules = null;
		}
	}
	
	public WinVMJProduct(IFeatureProject project)
			throws CoreException {
		IFolder productModule = getProductModuleFromComposedProduct(project);
		this.splName = productModule.getName().split(".product.")[0];
		this.productName = getProductClassName(productModule);
		this.modules = getModulesFromComposedProduct(project);
		
	}
	
	private static String getProductClassName(IFolder productModule) throws CoreException {
		IFolder dirToClass = productModule;
		for (String filePath: productModule.getName().split("\\."))
			dirToClass = dirToClass.getFolder(filePath);
		return FilenameUtils.getBaseName(dirToClass.members()[0].getName());
	}
	
	private static IFolder getProductModuleFromComposedProduct(IFeatureProject project)
			throws CoreException {
		IResource productModule = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> module.getName().contains(".product."))
				.findFirst().get();
		
		return (IFolder) productModule;
	}
	
	private static List<IFolder> getModulesFromComposedProduct(IFeatureProject project) throws CoreException {
		return getModuleNamesFromComposedProduct(project).stream().map(mdl -> 
				project.getBuildFolder().getFolder(mdl)).collect(Collectors.toList());
	}
	
	private static List<String> getModuleNamesFromComposedProduct(IFeatureProject project)
			throws CoreException {
		List<String> sourceModules = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> !module.getName().contains(".product."))
				.map(module -> module.getName())
				.collect(Collectors.toList());
		List<String> orderedSourceModules = new ArrayList<>();
		for (IProject externalProject: project.getProject().getReferencedProjects()) {
			CorePlugin.getDefault();
			orderedSourceModules.addAll(getModuleNamesFromComposedProduct(
					CorePlugin.getFeatureProject(externalProject)));
		}
		orderedSourceModules.addAll(selectModulesWithMapping(sourceModules, 
				project.getProject()));
		return orderedSourceModules.stream().distinct().collect(Collectors.toList());
	}
	
	private static List<String> selectModulesWithMapping(List<String> sourceModules, 
			IProject project) throws CoreException {
		Reader mapReader = new InputStreamReader(project
				.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME).getContents()); 
		Gson gson = new Gson();
		Map<String,List<String>> mappings = gson.fromJson(mapReader, 
				new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
		
		return mappings.values().stream().distinct()
				.flatMap(modules -> modules.stream())
				.filter(module -> sourceModules.contains(module))
				.collect(Collectors.toList());
	}
	
	private List<IFolder> selectModules(IFeatureProject project, List<IFeature> features) 
			throws CoreException, ParserException {
		final FormulaFactory formulaFactory = new FormulaFactory();
		final PropositionalParser formulaParser = new PropositionalParser(formulaFactory);
		
		IFile featureToModuleMapper = project.getProject()
		.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME);
		List<IFolder> selectedModules = new ArrayList<>();
		
		CorePlugin.getDefault();
		Map<String, IFeatureProject> refProjectMap = 
				Stream.of(project.getProject().getReferencedProjects())
				.map(pr -> CorePlugin.getFeatureProject(pr))
				.collect(Collectors.toMap(pr -> getSplName(pr), Function.identity()));
		
		MultiFeatureModel multiFetureModel = (MultiFeatureModel) project.getFeatureModel();
		if (multiFetureModel.isMultiProductLineModel()) {
			for (Entry<String, UsedModel> importedModel: multiFetureModel
					.getExternalModels().entrySet()) {
				String externalSplName = importedModel.getValue()
						 .getModelName().replace("interfaces.", "");
				IFeatureProject refProject = refProjectMap.get(externalSplName);
				 List<IFeature> externalFeatures = features.stream()
						 .filter(f -> f.getName()
								 .startsWith(importedModel.getKey() + "."))
						 .map(f -> new Feature(refProject.getFeatureModel(), 
								 f.getName().replace(importedModel.getKey() + ".", "")))
						 .collect(Collectors.toList());
				List<String> relatedProducts = getRelatedProducts(project, externalSplName);
				externalFeatures.addAll(selectFeaturesFromRelatedProducts(externalSplName, 
						refProject, relatedProducts));
				selectedModules.addAll(selectModules(refProject, externalFeatures));
			}
		}
		
		Assignment assignment = getFeatureCheckingAssignment(features, formulaFactory);
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
			if (this.evaluate(assignment, formulaParser, mapping.getKey())) 
				selectedModules.addAll(mapping.getValue().stream().map(mdl -> 
				project.getProject().getFolder(WinVMJComposer.MODULE_FOLDERNAME)
				.getFolder(mdl)).collect(Collectors.toList()));
		}
		return selectedModules.stream().distinct().collect(Collectors.toList());
	}
	
	private List<String> getRelatedProducts(IFeatureProject project, 
			String externalSplName) throws CoreException {
		IFile interSplProductMapper = project.getProject()
				.getFile(WinVMJComposer.INTER_SPL_PRODUCT_MAPPER_FILENAME);
		Reader mapReader =  new InputStreamReader(interSplProductMapper.getContents());
		Gson gson = new Gson();
		Map<String, List<String>> mappings;
		try {
			mappings = gson.fromJson(mapReader, 
					new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
		} catch (NullPointerException e) {
			mappings = new LinkedHashMap<String, List<String>>();
		}
		if (mappings.containsKey(productName))
			return mappings.get(productName).stream()
				.filter(p -> p.startsWith(externalSplName)).map(p -> 
				p.replace(externalSplName + ":", ""))
				.collect(Collectors.toList());
		return new ArrayList<>();
	}
	
	private List<IFeature> selectFeaturesFromRelatedProducts(String externalSplName, 
			IFeatureProject refProject, List<String> relatedProducts) 
					throws CoreException {
		List<IFeature> features = new ArrayList<>();
		for (String relatedProduct: relatedProducts) {
			Optional<IResource> configFile = Stream.of(refProject.getConfigFolder()
					.members()).filter(c -> c.getName().startsWith(relatedProduct))
					.findFirst();
			if (configFile.isPresent()) 
				features.addAll(refProject.loadConfiguration(EclipseFileSystem
					.getPath(configFile.get())).getSelectedFeatures());
			else WinVMJConsole.println("[WARNING] Product `" + relatedProduct + 
					"` does not exist in `" + externalSplName + "` SPL and will be ignored");
		}
		
		return features.stream().distinct().collect(Collectors.toList());
	}
	
	private Assignment getFeatureCheckingAssignment(List<IFeature> features, 
			FormulaFactory formulaFactory) {
		List<Variable> featureVariables = features.stream().map(feature -> 
		formulaFactory.variable(feature.getName())).collect(Collectors.toList());
		return new Assignment(featureVariables);
	}
	
	private boolean evaluate(Assignment assignment, PropositionalParser formulaParser, 
			String formulaString) throws ParserException {
		Formula formula = formulaParser.parse(formulaString);
		return formula.evaluate(assignment);
	}
	
	private String getSplName(IFeatureProject project) {
		return project.getFeatureModel().getStructure().getRoot().getFeature().getName();
	}
	
	private String getProductName(Path config) {
		return StringUtils.capitalize(FilenameUtils.getBaseName(config.getFileName().toString()));
	}

	public List<IFolder> getModules() {
		return modules;
	}
	
	public List<String> getModuleNames() {
		return modules.stream().map(IFolder::getName).collect(Collectors.toList());
	}

	public String getProductName() {
		return productName;
	}
	
	public String getProductQualifiedName() {
		return splName.toLowerCase() + ".product." + productName.toLowerCase();
	}
}
