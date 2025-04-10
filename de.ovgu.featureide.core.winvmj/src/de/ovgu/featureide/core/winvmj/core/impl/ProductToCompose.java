package de.ovgu.featureide.core.winvmj.core.impl;

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
import de.ovgu.featureide.core.winvmj.Utils;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel.UsedModel;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;

public class ProductToCompose extends WinVMJProduct {
	
	public ProductToCompose(IFeatureProject featureProject, Path config) {
		this.productName = getProductName(config);
		this.splName = Utils.getSplName(featureProject);
		try {
			this.modules = selectModules(featureProject, featureProject
					.loadConfiguration(config).getSelectedFeatures());
		} catch (CoreException | ParserException e) {
			this.modules = null;
		}
	}
	
	private List<IFolder> selectModules(IFeatureProject project, List<IFeature> features) 
			throws CoreException, ParserException {
		List<IFolder> selectedModules = new ArrayList<>();
		selectedModules.addAll(selectExternalModules(project, features));
		selectedModules.addAll(selectAndOrderModulesByMapping(project, features));
		return selectedModules.stream().distinct().collect(Collectors.toList());
	}
	
	private List<IFolder> selectExternalModules(IFeatureProject project, 
			List<IFeature> features) throws CoreException, ParserException {
		CorePlugin.getDefault();
		Map<String, IFeatureProject> refProjectMap = 
				Stream.of(project.getProject().getReferencedProjects())
				.map(pr -> CorePlugin.getFeatureProject(pr))
				.collect(Collectors.toMap(pr -> Utils.getSplName(pr), Function.identity()));
		
		List<IFolder> selectedModules = new ArrayList<>();
		MultiFeatureModel multiFetureModel = (MultiFeatureModel) project.getFeatureModel();
		if (multiFetureModel.isMultiProductLineModel()) {
			for (Entry<String, UsedModel> interfaceModel: multiFetureModel
					.getExternalModels().entrySet()) {
				String externalSplName = interfaceModel.getValue()
						 .getModelName().replace("interfaces.", "");
				IFeatureProject refProject = refProjectMap.get(externalSplName);
				 List<IFeature> externalFeatures = features.stream()
						 .filter(f -> f.getName()
								 .startsWith(interfaceModel.getKey() + "."))
						 .map(f -> new Feature(refProject.getFeatureModel(), 
								 f.getName().replace(interfaceModel.getKey() + ".", "")))
						 .collect(Collectors.toList());
				List<String> relatedProducts = Utils.getRelatedProducts(
					project, externalSplName, productName);
				externalFeatures.addAll(Utils.selectFeaturesFromRelatedProducts(externalSplName, 
						refProject, relatedProducts));
				selectedModules.addAll(selectModules(refProject, externalFeatures));
			}
		}
		return selectedModules;
	}
	
	private List<IFolder> selectAndOrderModulesByMapping(IFeatureProject project, 
			List<IFeature> features) throws ParserException, CoreException {
		List<IFolder> selectedModules = new ArrayList<>();
		IFile featureToModuleMapper = project.getProject()
				.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME);
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
			if (Utils.evaluate(assignment, formulaParser, mapping.getKey())) 
				selectedModules.addAll(mapping.getValue().stream().map(mdl -> 
				project.getProject().getFolder(WinVMJComposer.MODULE_FOLDERNAME)
				.getFolder(mdl)).collect(Collectors.toList()));
		}
		return selectedModules;
	}
	
	private String getProductName(Path config) {
		return StringUtils.capitalize(FilenameUtils.getBaseName(config.getFileName().toString()));
	}
}
