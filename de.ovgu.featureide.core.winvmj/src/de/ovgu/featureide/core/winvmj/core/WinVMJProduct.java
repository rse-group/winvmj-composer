package de.ovgu.featureide.core.winvmj.core;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
		return selectedModules;
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
