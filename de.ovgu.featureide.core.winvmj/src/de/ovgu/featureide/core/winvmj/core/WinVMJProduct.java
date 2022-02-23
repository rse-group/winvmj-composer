package de.ovgu.featureide.core.winvmj.core;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.base.IFeature;

public class WinVMJProduct {
	
	private List<IFeature> features;
	private List<String> modules;
	private String productName;
	private String splName;
	final private FormulaFactory formulaFactory;
	final private PropositionalParser formulaParser;
	
	public WinVMJProduct(String productName, String splName, 
			List<IFeature> features, IFile featureToModuleMapper) {
		this.productName = productName;
		this.splName = splName;
		this.features = features;
		formulaFactory = new FormulaFactory();
		formulaParser = new PropositionalParser(formulaFactory);
		this.modules = selectModules(featureToModuleMapper);
		
	}
	
	public WinVMJProduct(IFeatureProject project, IFile featureToModuleMapper)
			throws CoreException {
		IFolder productModule = getProductModuleFromComposedProduct(project);
		this.splName = productModule.getName().split(".product.")[0];
		this.productName = getProductClassName(productModule);
		this.features = null;
		formulaFactory = null;
		formulaParser = null;
		this.modules = getFeatureModulesFromComposedProduct(project, featureToModuleMapper);
		
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
	
	private static List<String> getFeatureModulesFromComposedProduct(IFeatureProject project,
			IFile featureToModuleMapper)
			throws CoreException {
		Reader mapReader = null;
		try {
			mapReader = new InputStreamReader(featureToModuleMapper.getContents());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Gson gson = new Gson();
		Map<String,List<String>> mappings = gson.fromJson(mapReader, 
				new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
		
		List<String> sourceModules = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> !module.getName().contains(".product."))
				.map(module -> module.getName())
				.collect(Collectors.toList());
		
		return mappings.values().stream().distinct()
				.flatMap(modules -> modules.stream())
				.filter(module -> sourceModules.contains(module))
				.collect(Collectors.toList());
	}
	
	private List<String> selectModules(IFile featureToModuleMapper) {
		return selectModules(features, featureToModuleMapper);
	}
	
	private List<String> selectModules(List<IFeature> features, IFile featureToModuleMapper) {
		List<String> selectedModules = new ArrayList<>();
		Assignment assignment = getFeatureCheckingAssignment();
		Reader mapReader = null;
		try {
			mapReader = new InputStreamReader(featureToModuleMapper.getContents());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Gson gson = new Gson();
		Map<String, List<String>> mappings;
		try {
			mappings = gson.fromJson(mapReader, new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
		} catch (NullPointerException e) {
			mappings = new LinkedHashMap<String, List<String>>();
		}
		for (Entry<String, List<String>> mapping: mappings.entrySet()) {
			try {
				if (this.evaluate(assignment, mapping.getKey())) selectedModules.addAll(mapping.getValue());
			} catch (ParserException e) {
				e.printStackTrace();
			}
		}
		return selectedModules.stream().distinct().collect(Collectors.toList());
	}
	
	private Assignment getFeatureCheckingAssignment() {
		List<Variable> featureVariables = features.stream().map(feature -> 
		formulaFactory.variable(feature.getName())).collect(Collectors.toList());
		return new Assignment(featureVariables);
	}
	
	private boolean evaluate(Assignment assignment, String formulaString) throws ParserException {
		Formula formula = formulaParser.parse(formulaString);
		return formula.evaluate(assignment);
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
