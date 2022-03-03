package de.ovgu.featureide.core.winvmj.core;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.fm.core.base.IFeature;

public class WinVMJProduct {
	
	private List<IFeature> features;
	private List<String> modules;
	private String productName;
	private String splName;
	
	public WinVMJProduct(IFeatureProject featureProject, Path config) {
		this.productName = getProductName(config);
		this.splName = getSplName(featureProject);
		this.features = featureProject.loadConfiguration(config).getSelectedFeatures();
		try {
			this.modules = selectModules(features, featureProject.getProject()
					.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME));
		} catch (CoreException | ParserException e) {
			this.modules = null;
		}
	}
	
	public WinVMJProduct(IFeatureProject project)
			throws CoreException {
		IFolder productModule = getProductModuleFromComposedProduct(project);
		this.splName = productModule.getName().split(".product.")[0];
		this.productName = getProductClassName(productModule);
		this.features = null;
		this.modules = getFeatureModulesFromComposedProduct(project);
		
	}
	
	private static String getProductClassName(IFolder productModule) throws CoreException {
		IFolder dirToClass = productModule;
		for (String filePath: productModule.getName().split("\\."))
			dirToClass = dirToClass.getFolder(filePath);
		return Files.getNameWithoutExtension(dirToClass.members()[0].getName());
	}
	
	private static IFolder getProductModuleFromComposedProduct(IFeatureProject project)
			throws CoreException {
		IResource productModule = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> module.getName().contains(".product."))
				.findFirst().get();
		
		return (IFolder) productModule;
	}
	
	private static List<String> getFeatureModulesFromComposedProduct(IFeatureProject project)
			throws CoreException {
		List<String> sourceModules = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> !module.getName().contains(".product."))
				.map(module -> module.getName())
				.collect(Collectors.toList());
		List<String> orderedSourceModules = new ArrayList<>();
		for (IProject externalProject: project.getProject().getReferencedProjects()) {
			CorePlugin.getDefault();
			orderedSourceModules.addAll(getFeatureModulesFromComposedProduct(
					CorePlugin.getFeatureProject(externalProject)));
		}
		orderedSourceModules.addAll(selectFeatureModuleWithMapping(sourceModules, 
				project.getProject()));
		return orderedSourceModules.stream().distinct().collect(Collectors.toList());
	}
	
	private static List<String> selectFeatureModuleWithMapping(List<String> sourceModules, 
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
	
	private List<String> selectModules(List<IFeature> features, 
			IFile featureToModuleMapper) throws CoreException, ParserException {
		final FormulaFactory formulaFactory = new FormulaFactory();
		final PropositionalParser formulaParser = new PropositionalParser(formulaFactory);
		
		List<String> selectedModules = new ArrayList<>();
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
				selectedModules.addAll(mapping.getValue());
		}
		return selectedModules.stream().distinct().collect(Collectors.toList());
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
		return StringUtils.capitalize(Files.getNameWithoutExtension(config.getFileName().toString()));
	}

	public List<String> getModules() {
		return modules;
	}

	public String getProductName() {
		return productName;
	}
	
	public String getProductQualifiedName() {
		return splName.toLowerCase() + ".product." + productName.toLowerCase();
	}
}
