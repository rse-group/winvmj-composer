package de.ovgu.featureide.core.winvmj.templates.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.logicng.io.parsers.ParserException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.ConfigurationPropagator;
import de.ovgu.featureide.fm.core.configuration.SelectableFeature;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.manager.IFeatureModelManager;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ModuleInfoRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ProductClassRenderer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;

public class MultiStageConfiguration {
	private WinVMJProduct product;
	private WinVMJComposer composer =  new WinVMJComposer();
	private static String FEATURE_MODEL_FOLDER = "featureModels";
	
	public static List<IFile> getAllFeatureModelNames(IFeatureProject featureProject){
		List<IFile> featureModelNames = new ArrayList<>();
        IProject project = featureProject.getProject();

        try {
            project.accept(resource -> {
                if (resource instanceof IFile && resource.getName().endsWith(".uvl")) {
                	WinVMJConsole.println(resource.getName());
                	featureModelNames.add((IFile) resource);
                }
                return true; 
            });
        } catch (CoreException e) {
            e.printStackTrace();
        }
        
        return featureModelNames;
	}
	
	public static List<IFeatureModel> loadAllFeatureModels(List<IFile> featureModelFiles) {
        List<IFeatureModel> featureModels = new ArrayList<>();

        for (IFile file : featureModelFiles) {
            IFeatureModel featureModel = loadFeatureModel(file);

            if (featureModel != null) {
                featureModels.add(featureModel);
            }
        }

        return featureModels;
    }

    public static IFeatureModel loadFeatureModel(IFile file) {
        Path featurePath = Paths.get(file.getLocation().toOSString());
        return FeatureModelManager.load(featurePath);
    }
    
    public ArrayList<IFeature> convertSelectedFeaturesToList(Map<String, HashSet<String>> selectedFeaturesMap, IFeatureProject featureProject) {
    	ArrayList<IFeature> featureList = new ArrayList<>();
    	
    	HashSet<String> allowedFeatureNames = loadAllowedFeatureNamesFromJson(featureProject);
    	
        for (Map.Entry<String, HashSet<String>> entry : selectedFeaturesMap.entrySet()) {
            String selectedFile = entry.getKey();
            HashSet<String> featureNames = entry.getValue();
            
            IFile featureFile = featureProject.getProject().getFolder(FEATURE_MODEL_FOLDER).getFile(selectedFile);
            IFeatureModel featureModel = loadFeatureModel(featureFile);

            ArrayList<IFeature> features = new ArrayList<>(
        			featureNames.stream()
        				.filter(allowedFeatureNames::contains)
        				.map(name -> new Feature(featureModel, name))
        				.collect(Collectors.toList())
        		);
            
            featureList.addAll(features);
        }

        return featureList;
    }
    
    private HashSet<String> loadAllowedFeatureNamesFromJson(IFeatureProject featureProject) {
    	HashSet<String> allowed = new HashSet<>();
    	IFile jsonFile = featureProject.getProject().getFile("feature_to_module.json");

    	try {
    		IFile featureToModuleMapper = featureProject.getProject().getFile(composer.FEATURE_MODULE_MAPPER_FILENAME);

    		Reader reader = new InputStreamReader(featureToModuleMapper.getContents());
    		Gson gson = new Gson();

    		Map<String, List<String>> mappings = gson.fromJson(
    			reader,
    			new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType()
    		);

    		if (mappings != null) {
    			allowed.addAll(mappings.keySet());
    		}
    	} catch (Exception e) {
    		WinVMJConsole.println("Error loading feature_to_module.json: " + e.getMessage());
    	}


    	return allowed;
    }
    
    public void composeProduct(WinVMJProduct product, ArrayList<IFeature> selectedFeature, IFeatureProject featureProject) throws CoreException, ParserException {
    	IFolder srcFolder = featureProject.getBuildFolder();
        if (srcFolder.exists()) {
            for (IResource member : srcFolder.members()) {
                member.delete(true, null);
            }
        }
    	composer.selectModulesFromProject(featureProject, product);
    	composer.checkMultiLevelDelta(featureProject, product, selectedFeature);
        IFolder productModule = featureProject.getBuildFolder()
                .getFolder(product.getProductQualifiedName());
        if (!productModule.exists()) productModule.create(false, true, null);
        Map<String, List<String>> multiLevelDeltaMappings = null;
        TemplateRenderer moduleInfoRenderer = new ModuleInfoRenderer(featureProject, multiLevelDeltaMappings);
        TemplateRenderer productClassRenderer = new ProductClassRenderer(featureProject);
        moduleInfoRenderer.render(product);
        productClassRenderer.render(product); 
    }

}
