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

public class MultiStageConfiguration {
	private WinVMJProduct product;
	private WinVMJComposer composer =  new WinVMJComposer();
	
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

        for (Map.Entry<String, HashSet<String>> entry : selectedFeaturesMap.entrySet()) {
            String selectedFile = entry.getKey();
            HashSet<String> featureNames = entry.getValue();
            
            IFile featureFile = featureProject.getProject().getFolder("featureModels").getFile(selectedFile);
            
            IFeatureModel featureModel = loadFeatureModel(featureFile);

            ArrayList<IFeature> features = new ArrayList<>(
                    featureNames.stream()
                    	.filter(name -> !name.equals("Features"))
                        .map(name -> new Feature(featureModel, name))
                        .collect(Collectors.toList()) 
                );
            
            featureList.addAll(features);
        }

        return featureList;
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
    
    
//    public static void composeProduct(IFeatureProject featureProject,  HashSet<String> selectedFeature) {
//		try {
//			WinVMJConsole.println(selectedFeature.toString());
//			selectModulesFromProject(featureProject, selectedFeature);
////			checkMultiLevelDelta(
////				featureProject,
////				product,
////				featureProject.loadConfiguration(config).getSelectedFeatures()
////			);
////			IFolder productModule = featureProject.getBuildFolder()
////					.getFolder(product.getProductQualifiedName());
////			if (!productModule.exists()) productModule.create(false, true, null);
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
//		
//		TemplateRenderer moduleInfoRenderer = new ModuleInfoRenderer(
//			featureProject, multiLevelDeltaMappings);
//		TemplateRenderer productClassRenderer = new ProductClassRenderer(featureProject);
//		moduleInfoRenderer.render(product);
//		productClassRenderer.render(product);
//	}
//	
//	private static void selectModulesFromProject(IFeatureProject project, HashSet<String> selectedFeatures) throws CoreException {
//		IFolder modulesFolder = project.getProject().getFolder("modules");
//		
//	    Set<String> filteredFeatures = new HashSet<>();
//	    for (String feature : selectedFeatures) {
//	        String[] parts = feature.split("\\.", 2);
//	        if (parts.length > 1) {
//	            filteredFeatures.add(parts[1].toLowerCase()); 
//	        } else {
//	            filteredFeatures.add(parts[0].toLowerCase());
//	        }
//	    }
//	    
//	    WinVMJConsole.println(filteredFeatures.toString());
//	    
//		for (IResource resource : modulesFolder.members()) {
//	        if (resource instanceof IFolder) {
//	            IFolder sourceModule = (IFolder) resource;
//	            WinVMJConsole.println(sourceModule.getName());
//	            boolean shouldCopy = filteredFeatures.stream().anyMatch(sourceModule.getName()::contains);
//	            if (shouldCopy) {
//	                IFolder destModule = project.getBuildFolder().getFolder(sourceModule.getName());
//	                if (!destModule.exists()) destModule.create(false, true, null);
//	                copyFolder(sourceModule, destModule);
//	                WinVMJConsole.println("done");
//	            }
//	        }
//	    }
//	}
//	
//	private static void copyFolder(IFolder source, IFolder destination) throws CoreException {
//	    for (IResource resource : source.members()) {
//	        if (resource instanceof IFile) {
//	            IFile srcFile = (IFile) resource;
//	            IFile destFile = destination.getFile(srcFile.getName());
//	            if (!destFile.exists()) {
//	                srcFile.copy(destFile.getFullPath(), true, null);
//	            }
//	        } else if (resource instanceof IFolder) {
//	            IFolder srcFolder = (IFolder) resource;
//	            IFolder destFolder = destination.getFolder(srcFolder.getName());
//	            if (!destFolder.exists()) {
//	                destFolder.create(false, true, null);
//	            }
//	            copyFolder(srcFolder, destFolder); // Recursively copy subfolders
//	        }
//	    }
//	}
	
//	private void checkMultiLevelDelta(
//			IFeatureProject project,
//			WinVMJProduct product,
//			List<IFeature> features
//		) throws CoreException, ParserException {
//			checkExternalMultiLevelDelta(project, product, features);
//			processMultiLevelDelta(project, product, features);
//		}
//
//		private void checkExternalMultiLevelDelta(
//			IFeatureProject project,
//			WinVMJProduct product,
//			List<IFeature> features
//		) throws CoreException, ParserException {
//			CorePlugin.getDefault();
//			Map<String, IFeatureProject> refProjectMap =
//					Stream.of(project.getProject().getReferencedProjects())
//					.map(pr -> CorePlugin.getFeatureProject(pr))
//					.collect(Collectors.toMap(pr -> Utils.getSplName(pr), Function.identity()));
//			
//			MultiFeatureModel multiFetureModel = (MultiFeatureModel) project.getFeatureModel();
//			if (multiFetureModel.isMultiProductLineModel()) {
//				for (Entry<String, UsedModel> interfaceModel: multiFetureModel
//						.getExternalModels().entrySet()) {
//					String externalSplName = interfaceModel.getValue()
//							 .getModelName().replace("interfaces.", "");
//					IFeatureProject refProject = refProjectMap.get(externalSplName);
//					List<IFeature> externalFeatures = features.stream()
//						    .filter(f -> f.getName().startsWith(interfaceModel.getKey() + "."))
//						    .<IFeature>map(f -> new Feature(refProject.getFeatureModel(), 
//						        f.getName().replace(interfaceModel.getKey() + ".", "")))
//						    .collect(Collectors.toList());
//					List<String> relatedProducts = Utils.getRelatedProducts(
//						project, externalSplName, product.getProductName());
//					externalFeatures.addAll(Utils.selectFeaturesFromRelatedProducts(externalSplName, 
//							refProject, relatedProducts));
//					checkMultiLevelDelta(refProject, product, externalFeatures);
//				}
//			}
//		}
//
//		private void processMultiLevelDelta(
//			IFeatureProject project,
//			WinVMJProduct product,
//			List<IFeature> features
//		) throws CoreException, ParserException {
//			IFile featureToModuleMapper = project.getProject()
//					.getFile(FEATURE_MODULE_MAPPER_FILENAME);
//			final FormulaFactory formulaFactory = new FormulaFactory();
//			final PropositionalParser formulaParser = new PropositionalParser(formulaFactory);
//
//			Assignment assignment = Utils.getFeatureCheckingAssignment(features, formulaFactory);
//			Reader mapReader =  new InputStreamReader(featureToModuleMapper.getContents());
//			Gson gson = new Gson();
//			Map<String, List<String>> mappings;
//			try {
//				mappings = gson.fromJson(mapReader, 
//						new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
//			} catch (NullPointerException e) {
//				mappings = new LinkedHashMap<String, List<String>>();
//			}
//
//			for (Entry<String, List<String>> mapping: mappings.entrySet()) {
//				String key = mapping.getKey();
//				List<String> value = mapping.getValue();
//				if (
//					Utils.evaluate(assignment, formulaParser, key) &&
//					Utils.isMultiLevelDelta(mapping)
//				) {
//					MultiLevelDeltaComposer multiLevelDeltaComposer = new MultiLevelDeltaComposer(
//						featureProject, product, key, value);
//					multiLevelDeltaComposer.compose();
//
//					if (multiLevelDeltaMappings == null) multiLevelDeltaMappings = new HashMap<>();
//					multiLevelDeltaMappings.put(key, value);
//				}
//			}
//		}
	
//	public static void multiStage(IFeatureProject project, List<String> featureModelFiles, String selectedVendor, String selectedFeature) {
//        WinVMJConsole.println("Using " + featureModelFiles.size() + " Feature Models..................");
//
//        Map<String, IFeatureModel> featureModels = new HashMap<>();
//        Map<String, Configuration> configurations = new HashMap<>();
//        Map<String, FeatureModelFormula> formulas = new HashMap<>();
//        Map<String, String> featureVendorMapping = new HashMap<>();
//
//
//        for (String featureModelFile : featureModelFiles) {
//            IFile file = project.getProject().getFile(featureModelFile);
//            Path path = Paths.get(file.getLocation().toOSString());
//
//            IFeatureModel featureModel = FeatureModelManager.load(path.toFile());
//            FeatureModelFormula formula = new FeatureModelFormula(featureModel);
//            Configuration config = new Configuration(formula);
//
//            featureModels.put(featureModelFile, featureModel);
//            configurations.put(featureModelFile, config);
//            formulas.put(featureModelFile, formula);
//
//            WinVMJConsole.println("Loaded Feature Model: " + featureModelFile);
//            WinVMJConsole.println("Feature List: " + featureModel.getFeatureOrderList().toString());
//        }
//
//
//        for (String featureModelFile : featureModelFiles) {
//            IFeatureModel featureModel = featureModels.get(featureModelFile);
//            List<String> featureList = featureModel.getFeatureOrderList();
//
//            for (IConstraint constraint : featureModel.getConstraints()) {
//                String constraintStr = constraint.toString();
//                WinVMJConsole.println("Processing constraint: " + constraintStr);
//
//                for (String feature : featureList) {
//
//                    for (String vendorModelFile : featureModelFiles) {
//                        IFeatureModel vendorModel = featureModels.get(vendorModelFile);
//                        for (String vendor : vendorModel.getFeatureOrderList()) {
//                            if (constraintStr.contains(feature) && constraintStr.contains(vendor)) {
//                                featureVendorMapping.put(feature, vendor);
//                                WinVMJConsole.println("Mapped feature " + feature + " to vendor " + vendor);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        WinVMJConsole.println("Dynamic Feature-Vendor Mapping: " + featureVendorMapping);
//
//
//        boolean vendorSelected = false;
//        for (String featureModelFile : featureModelFiles) {
//            IFeatureModel vendorFeatureModel = featureModels.get(featureModelFile);
//            Configuration vendorConfig = configurations.get(featureModelFile);
//
//            if (vendorFeatureModel.getFeatureOrderList().contains(selectedVendor)) {
//                vendorConfig.setManual(selectedVendor, Selection.SELECTED);
//                WinVMJConsole.println("Selected Vendor: " + selectedVendor);
//                vendorSelected = true;
//            }
//        }
//
//        if (!vendorSelected) {
//            WinVMJConsole.println("Invalid Vendor Selection!");
//            return;
//        }
//
//        // **4️⃣ User selects a Feature (Ensuring Vendor is Selected First)**
//        if (featureVendorMapping.containsKey(selectedFeature)) {
//            String requiredVendor = featureVendorMapping.get(selectedFeature);
//
//            for (String featureModelFile : featureModelFiles) {
//                Configuration vendorConfig = configurations.get(featureModelFile);
//                Configuration featureConfig = configurations.get(featureModelFile);
//
//                if (vendorConfig.getSelectableFeature(requiredVendor) != null &&
//                        vendorConfig.getSelectableFeature(requiredVendor).getSelection() == Selection.SELECTED) {
//                    featureConfig.setManual(selectedFeature, Selection.SELECTED);
//                    WinVMJConsole.println("Selected Feature: " + selectedFeature);
//                } else {
//                    WinVMJConsole.println(selectedFeature + " requires " + requiredVendor + " to be selected first!");
//                    return;
//                }
//            }
//        } else {
//            WinVMJConsole.println("Invalid Feature Selection!");
//            return;
//        }
//
//        // Validate All Configurations**
//        boolean allValid = true;
//
//        for (String featureModelFile : featureModelFiles) {
//            Configuration config = configurations.get(featureModelFile);
//            FeatureModelFormula formula = formulas.get(featureModelFile);
//
//            ConfigurationPropagator propagator = new ConfigurationPropagator(formula, config);
//            Boolean isValid = LongRunningWrapper.runMethod(propagator.canBeValid());
//
//            if (!Boolean.TRUE.equals(isValid)) {
//                allValid = false;
//            }
//        }
//
//        if (allValid) {
//            WinVMJConsole.println("Final Configuration is VALID! ✅");
//        } else {
//            WinVMJConsole.println("Final Configuration is INVALID ❌. Check constraints.");
//        }
//    }
//	
	public static void multiStage(IFeatureProject project) {
	
		
//		//path ke feature model
		IFeatureModelManager featureModelPath = project.getFeatureModelManager();
		WinVMJConsole.println("Feature Model FeatureModelManager PersistentFormula");
		WinVMJConsole.println(featureModelPath.getPersistentFormula().toString());
		WinVMJConsole.println("Feature Model FeatureModelManager PersistentFormula");	
		
		//feature model
		IFeatureModel featureModel = project.getFeatureModel();
		
		WinVMJConsole.println("Structure " + featureModel.getStructure());
		WinVMJConsole.println("Root " + featureModel.getStructure().getRoot());
		WinVMJConsole.println("Features " + featureModel.getStructure().getRoot().getFeature());
		WinVMJConsole.println("Features Root Property" + featureModel.getStructure().getRoot().getFeature().getProperty());
		WinVMJConsole.println("Features Root Feature Model" + featureModel.getStructure().getRoot().getFeature().getFeatureModel());
		
		Collection<IFeature> features = featureModel.getStructure().getFeaturesPreorder();
		for (IFeature feature : features) {
		    WinVMJConsole.println("Feature: " + feature);
		    WinVMJConsole.println(" - Name: " + feature.getName());
		    WinVMJConsole.println(" - Property: " + feature.getProperty());
		    if (feature.getStructure().getParent() != null) {
		        WinVMJConsole.println(" - Feature Parent Name: " + feature.getStructure().getParent().getFeature().getName());
		    }

		    WinVMJConsole.println("---------------------------------------------------------");

//		    if (feature. != null) {
//		        WinVMJConsole.println(" - Parent: " + feature.getParent().getName());
//		    } else {
//		        WinVMJConsole.println(" - Parent: None (Root Feature)");
//		    }
		}
		
		
//		List<IConstraint> features = featureModel.getConstraints();
//		WinVMJConsole.println("Feature Model Constraints:");
//		List<String> extractedConstraints = new ArrayList<>();
//
//		for (IConstraint constraint : features) {
//			WinVMJConsole.println("getDescription " + constraint.getDescription()); 
//			WinVMJConsole.println("getDisplayName " + constraint.getDisplayName()); 
//			WinVMJConsole.println("getContainedFeatures " + constraint.getContainedFeatures()); 
//			WinVMJConsole.println("getInternalId " + constraint.getInternalId()); 
//			WinVMJConsole.println("getName " + constraint.getName()); 
//			WinVMJConsole.println("getTags " + constraint.getTags()); 
//			WinVMJConsole.println("getNode " + constraint.getNode()); 
//			WinVMJConsole.println("---------------------------------------------------"); 
//		}
		
		HashSet<String> selectedFeatures = new HashSet<>();
		HashSet<String> targetFeatures = new HashSet<>(Arrays.asList("Xendit", "Vendor"));
				
		for (IConstraint constraint : project.getFeatureModel().getConstraints()) {
		    String node = constraint.getNode().toString();
		    String[] parts = node.split("=>");

		    if (parts.length == 2) {
		        String leftSide = parts[0].trim();
		        String rightSide = parts[1].trim();

		        HashSet<String> rightFeatures = new HashSet<>();
		        for (String feature : rightSide.split("\\|")) {
		            feature = feature.trim();
		            if (feature.contains(".")) {
		                feature = feature.substring(feature.indexOf('.') + 1); 
		            }
		            rightFeatures.add(feature);
		        }
		        if (!Collections.disjoint(targetFeatures, rightFeatures)) {
		            for (String feature : leftSide.split("\\|")) {
		                feature = feature.trim();
		                if (feature.contains(".")) {
		                    feature = feature.substring(feature.indexOf('.') + 1); 
		                }
		                selectedFeatures.add(feature);
		            }
		        }
		    }
		}
		
		WinVMJConsole.println("Final Selected Features: " + selectedFeatures);
	    
	}
	

//		for (IConstraint constraint : features) {
//		    String node = constraint.getNode().toString();
//
//		    if (node.contains(targetFeature)) {
//		        String[] parts = node.split("=>");
//		        WinVMJConsole.println("parts " + parts.toString()); 
//		        if (parts.length == 2) {
//		            String leftSide = parts[0].trim(); 
//		            WinVMJConsole.println("left side "+ leftSide.toString()); 
//		            String[] leftFeatures = leftSide.split("\\|"); 
//		            WinVMJConsole.println("left features "+ leftFeatures.toString()); 
//		            for (String feature : leftFeatures) {
//		                relatedFeatures.add(feature.trim());
//		            }
//		        }
//		    }
//		}

		// Print the related features
		

//		
//		
//		List<String> featuresList = featureModel.getFeatureOrderList();
//		WinVMJConsole.println("Feature Model Feature Order List");
//		WinVMJConsole.println(featuresList.toString());
//		WinVMJConsole.println("Feature Model Feature Order List");
		
//        final HashSet<SelectableFeature> updateFeatures = new HashSet<>(); 
//		WinVMJConsole.println("Using 1 Feature Model....................");
//		IFeatureModel featureModel = project.getFeatureModel();
//		IFeatureModelManager featureModelManager = project.getFeatureModelManager();
//
//		Configuration configuration = new Configuration(featureModelManager.getPersistentFormula());
//		
//		WinVMJConsole.println("Formula..................");
//		WinVMJConsole.println(featureModelManager.getPersistentFormula().toString());
//		
//		
//        WinVMJConsole.println("Configuration..................");
//        WinVMJConsole.println(configuration.toString());
//        
// 
//        List<String> featuresList = featureModel.getFeatureOrderList();
//        WinVMJConsole.println("Available Features: " + featuresList);
//
//        // Choose two features (modify these according to the actual model)
//        String feature1 = "Midtrans";
//        String feature2 = "SpecifiedRecipient";
//
//        WinVMJConsole.println("Checking possibility of selecting: " + feature1 + " and " + feature2);
//
//        configuration.setManual(feature1, Selection.SELECTED);
//        configuration.setManual(feature2, Selection.SELECTED);
//        
//        
//        ConfigurationPropagator propagator = new ConfigurationPropagator(featureModelManager.getPersistentFormula(), configuration);
//
//        WinVMJConsole.println("Propagator..................");
//        WinVMJConsole.println(propagator.toString());
//        
//        
//        LongRunningWrapper.getRunner(propagator.update(true));
//        
//        final Boolean canBeValid = LongRunningWrapper.runMethod(propagator.isValid());
//        updateFeatures.clear();
//		updateFeatures.addAll(configuration.getFeatures());
//
//        WinVMJConsole.println("Propagator results" + canBeValid);
//
//        if (canBeValid) {
//        	WinVMJConsole.println("1 Feature Model");
//        	WinVMJConsole.println("Selected results" + updateFeatures);
//        		for (final SelectableFeature feature : updateFeatures) {
//        			WinVMJConsole.println("Selected results" + feature + feature.getSelection());
//        		}
//        		WinVMJConsole.println("The selection of " + feature1 + " and " + feature2 + " is POSSIBLE.");
//          
//        } else {
//            WinVMJConsole.println("The selection of " + feature1 + " and " + feature2 + " is NOT POSSIBLE.");
//        }
//		
//        WinVMJConsole.println("Using 2 Feature Model..................");
//        
//		IFile vendorFile = project.getProject().getFile("vendor.uvl");
//		Path vendorPath = Paths.get(vendorFile.getLocation().toOSString());
//		final IFeatureModel vendorFeatureModel = FeatureModelManager.load(vendorPath);
//		
//		WinVMJConsole.println("Payment Gateway Vendor..................");
//		WinVMJConsole.println(vendorFeatureModel.getFeatureOrderList().toString());
//
//		FeatureModelFormula vendorFormula = new FeatureModelFormula(vendorFeatureModel);
//		
//		Configuration vendorConfiguration = new Configuration(vendorFormula);
//        WinVMJConsole.println("Configuration..................");
//        WinVMJConsole.println(vendorConfiguration.toString());
//		
//        
//        
//		IFile featuesFile = project.getProject().getFile("feature.uvl");
//		Path featuesPath = Paths.get(featuesFile.getLocation().toOSString());
//		final IFeatureModel featuresFeatureModel = FeatureModelManager.load(featuesPath);
//		WinVMJConsole.println("Payment Gateway Features..................");
//		WinVMJConsole.println(featuresFeatureModel.getFeatureOrderList().toString());
//		
//		FeatureModelFormula featureFormula = new FeatureModelFormula(featuresFeatureModel);
//		
//		Configuration featureConfiguration = new Configuration(featureFormula);
//        WinVMJConsole.println("Configuration..................");
//        WinVMJConsole.println(featureConfiguration.toString());
//        
//        
//		Map<String, String> featureVendorMapping = new HashMap<>();
//
//		List<String> featureList = featuresFeatureModel.getFeatureOrderList();
//
//		WinVMJConsole.println("features Feature Model Constraint" + featuresFeatureModel.getConstraints());
//		
//		for (IConstraint constraint : featuresFeatureModel.getConstraints()) {
//		    String constraintStr = constraint.toString(); 
//		    
//		    for (String feature : featureList) {
//			    if (featuresFeatureModel.getFeature(feature).getStructure().isAbstract()) {
//		            continue;
//		        }
//			    
//		        for (String vendor : vendorFeatureModel.getFeatureOrderList()) {
//		            if (constraintStr.contains(feature) && constraintStr.contains(vendor)) {
//		                featureVendorMapping.put(feature, vendor);
//		            }
//		        }
//		    }
//		}
//
//		WinVMJConsole.println("Dynamic Feature-Vendor Mapping: " + featureVendorMapping);
//		
//		String selectedVendor = "Flip";
//
//		if (vendorFeatureModel.getFeatureOrderList().contains(selectedVendor)) {
//			vendorConfiguration.setManual(selectedVendor, Selection.SELECTED);
//			WinVMJConsole.println("Select " + selectedVendor);
//		} else {
//		    WinVMJConsole.println("Invalid Vendor Selection!");
//		}
//		
//		String selectedFeature = "MidtransPaymentLink";
//
//		if (featureVendorMapping.containsKey(selectedFeature)) {
//		    String requiredVendor = featureVendorMapping.get(selectedFeature);
//
//		    if (vendorConfiguration.getSelectableFeature(requiredVendor).getSelection() == Selection.SELECTED) {
//		    	featureConfiguration.setManual(selectedFeature, Selection.SELECTED);
//		    	WinVMJConsole.println("Select " + selectedFeature);
//		    } else {
//		        WinVMJConsole.println(selectedFeature + " requires " + requiredVendor + " to be selected first!");
//		    }
//		} else {
//		    WinVMJConsole.println("Invalid Feature Selection!");
//		}
//		
//		ConfigurationPropagator vendorPropagator = new ConfigurationPropagator(vendorFormula, vendorConfiguration);
//		final Boolean isVendorValid = LongRunningWrapper.runMethod(vendorPropagator.canBeValid());
//
//		ConfigurationPropagator featuresPropagator = new ConfigurationPropagator(featureFormula, featureConfiguration);
//		final boolean isFeaturesValid = LongRunningWrapper.runMethod(featuresPropagator.canBeValid());
//
//		 WinVMJConsole.println("Vendor Propagator results" + isVendorValid);
//		 WinVMJConsole.println("Feature Propagator results" + isFeaturesValid);
//		if (isVendorValid && isFeaturesValid) {
//			WinVMJConsole.println("Vendor.................");
//			for (SelectableFeature feature : vendorConfiguration.getManualFeatures()) {
//			    if (feature.getSelection() == Selection.SELECTED) {
//			        WinVMJConsole.println("Selected Feature: " + feature.getFeature().getName());
//			    }
//			}
//			
//			WinVMJConsole.println("Features.................");
//
//			for (SelectableFeature feature : featureConfiguration.getManualFeatures()) {
//			    if (feature.getSelection() == Selection.SELECTED) {
//			        WinVMJConsole.println("Selected Feature: " + feature.getFeature().getName());
//			    }
//			}
//			
//		    WinVMJConsole.println("Final Configuration is VALID! ✅");
//		} else {
//		    WinVMJConsole.println("Final Configuration is INVALID ❌. Check constraints.");
//		}
		
//	}

}
