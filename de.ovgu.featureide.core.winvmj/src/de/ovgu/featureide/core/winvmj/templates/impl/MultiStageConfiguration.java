package de.ovgu.featureide.core.winvmj.templates.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.TreeItem;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.FeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.ConfigurationPropagator;
import de.ovgu.featureide.fm.core.configuration.SelectableFeature;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.manager.IFeatureModelManager;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;

public class MultiStageConfiguration {	
	
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

        WinVMJConsole.println(featureModels.toString());
        return featureModels;
    }

    public static IFeatureModel loadFeatureModel(IFile file) {
        Path featurePath = Paths.get(file.getLocation().toOSString());
        return FeatureModelManager.load(featurePath);
    }
	
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
//		IFeatureModelManager featureModelPath = project.getFeatureModelManager();
//		WinVMJConsole.println("Feature Model FeatureModelManager PersistentFormula");
//		WinVMJConsole.println(featureModelPath.getPersistentFormula().toString());
//		WinVMJConsole.println("Feature Model FeatureModelManager PersistentFormula");	
//		
//		//feature model
//		IFeatureModel featureModel = project.getFeatureModel();
//		
//		WinVMJConsole.println("Feature Model");
//		WinVMJConsole.println(featureModel.toString());
//		WinVMJConsole.println("Feature Model");		
//		
//		List<IConstraint> features = featureModel.getConstraints();
//		WinVMJConsole.println("Feature Model Constraints");
//		WinVMJConsole.println(features.toString());
//		WinVMJConsole.println("Feature Model Constraints");	
//		
//		
//		List<String> featuresList = featureModel.getFeatureOrderList();
//		WinVMJConsole.println("Feature Model Feature Order List");
//		WinVMJConsole.println(featuresList.toString());
//		WinVMJConsole.println("Feature Model Feature Order List");
		
        final HashSet<SelectableFeature> updateFeatures = new HashSet<>(); 
		WinVMJConsole.println("Using 1 Feature Model....................");
		IFeatureModel featureModel = project.getFeatureModel();
		IFeatureModelManager featureModelManager = project.getFeatureModelManager();

		Configuration configuration = new Configuration(featureModelManager.getPersistentFormula());
		
		WinVMJConsole.println("Formula..................");
		WinVMJConsole.println(featureModelManager.getPersistentFormula().toString());
		
		
        WinVMJConsole.println("Configuration..................");
        WinVMJConsole.println(configuration.toString());
        
 
        List<String> featuresList = featureModel.getFeatureOrderList();
        WinVMJConsole.println("Available Features: " + featuresList);

        // Choose two features (modify these according to the actual model)
        String feature1 = "Midtrans";
        String feature2 = "SpecifiedRecipient";

        WinVMJConsole.println("Checking possibility of selecting: " + feature1 + " and " + feature2);

        configuration.setManual(feature1, Selection.SELECTED);
        configuration.setManual(feature2, Selection.SELECTED);
        
        
        ConfigurationPropagator propagator = new ConfigurationPropagator(featureModelManager.getPersistentFormula(), configuration);

        WinVMJConsole.println("Propagator..................");
        WinVMJConsole.println(propagator.toString());
        
        
        LongRunningWrapper.getRunner(propagator.update(true));
        
        final Boolean canBeValid = LongRunningWrapper.runMethod(propagator.isValid());
        updateFeatures.clear();
		updateFeatures.addAll(configuration.getFeatures());

        WinVMJConsole.println("Propagator results" + canBeValid);

        if (canBeValid) {
        	WinVMJConsole.println("1 Feature Model");
        	WinVMJConsole.println("Selected results" + updateFeatures);
        		for (final SelectableFeature feature : updateFeatures) {
        			WinVMJConsole.println("Selected results" + feature + feature.getSelection());
        		}
        		WinVMJConsole.println("The selection of " + feature1 + " and " + feature2 + " is POSSIBLE.");
          
        } else {
            WinVMJConsole.println("The selection of " + feature1 + " and " + feature2 + " is NOT POSSIBLE.");
        }
		
        WinVMJConsole.println("Using 2 Feature Model..................");
        
		IFile vendorFile = project.getProject().getFile("vendor.uvl");
		Path vendorPath = Paths.get(vendorFile.getLocation().toOSString());
		final IFeatureModel vendorFeatureModel = FeatureModelManager.load(vendorPath);
		
		WinVMJConsole.println("Payment Gateway Vendor..................");
		WinVMJConsole.println(vendorFeatureModel.getFeatureOrderList().toString());

		FeatureModelFormula vendorFormula = new FeatureModelFormula(vendorFeatureModel);
		
		Configuration vendorConfiguration = new Configuration(vendorFormula);
        WinVMJConsole.println("Configuration..................");
        WinVMJConsole.println(vendorConfiguration.toString());
		
        
        
		IFile featuesFile = project.getProject().getFile("feature.uvl");
		Path featuesPath = Paths.get(featuesFile.getLocation().toOSString());
		final IFeatureModel featuresFeatureModel = FeatureModelManager.load(featuesPath);
		WinVMJConsole.println("Payment Gateway Features..................");
		WinVMJConsole.println(featuresFeatureModel.getFeatureOrderList().toString());
		
		FeatureModelFormula featureFormula = new FeatureModelFormula(featuresFeatureModel);
		
		Configuration featureConfiguration = new Configuration(featureFormula);
        WinVMJConsole.println("Configuration..................");
        WinVMJConsole.println(featureConfiguration.toString());
        
        
		Map<String, String> featureVendorMapping = new HashMap<>();

		List<String> featureList = featuresFeatureModel.getFeatureOrderList();

		WinVMJConsole.println("features Feature Model Constraint" + featuresFeatureModel.getConstraints());
		
		for (IConstraint constraint : featuresFeatureModel.getConstraints()) {
		    String constraintStr = constraint.toString(); 
		    
		    for (String feature : featureList) {
			    if (featuresFeatureModel.getFeature(feature).getStructure().isAbstract()) {
		            continue;
		        }
			    
		        for (String vendor : vendorFeatureModel.getFeatureOrderList()) {
		            if (constraintStr.contains(feature) && constraintStr.contains(vendor)) {
		                featureVendorMapping.put(feature, vendor);
		            }
		        }
		    }
		}

		WinVMJConsole.println("Dynamic Feature-Vendor Mapping: " + featureVendorMapping);
		
		String selectedVendor = "Flip";

		if (vendorFeatureModel.getFeatureOrderList().contains(selectedVendor)) {
			vendorConfiguration.setManual(selectedVendor, Selection.SELECTED);
			WinVMJConsole.println("Select " + selectedVendor);
		} else {
		    WinVMJConsole.println("Invalid Vendor Selection!");
		}
		
		String selectedFeature = "MidtransPaymentLink";

		if (featureVendorMapping.containsKey(selectedFeature)) {
		    String requiredVendor = featureVendorMapping.get(selectedFeature);

		    if (vendorConfiguration.getSelectableFeature(requiredVendor).getSelection() == Selection.SELECTED) {
		    	featureConfiguration.setManual(selectedFeature, Selection.SELECTED);
		    	WinVMJConsole.println("Select " + selectedFeature);
		    } else {
		        WinVMJConsole.println(selectedFeature + " requires " + requiredVendor + " to be selected first!");
		    }
		} else {
		    WinVMJConsole.println("Invalid Feature Selection!");
		}
		
		ConfigurationPropagator vendorPropagator = new ConfigurationPropagator(vendorFormula, vendorConfiguration);
		final Boolean isVendorValid = LongRunningWrapper.runMethod(vendorPropagator.canBeValid());

		ConfigurationPropagator featuresPropagator = new ConfigurationPropagator(featureFormula, featureConfiguration);
		final boolean isFeaturesValid = LongRunningWrapper.runMethod(featuresPropagator.canBeValid());

		 WinVMJConsole.println("Vendor Propagator results" + isVendorValid);
		 WinVMJConsole.println("Feature Propagator results" + isFeaturesValid);
		if (isVendorValid && isFeaturesValid) {
			WinVMJConsole.println("Vendor.................");
			for (SelectableFeature feature : vendorConfiguration.getManualFeatures()) {
			    if (feature.getSelection() == Selection.SELECTED) {
			        WinVMJConsole.println("Selected Feature: " + feature.getFeature().getName());
			    }
			}
			
			WinVMJConsole.println("Features.................");

			for (SelectableFeature feature : featureConfiguration.getManualFeatures()) {
			    if (feature.getSelection() == Selection.SELECTED) {
			        WinVMJConsole.println("Selected Feature: " + feature.getFeature().getName());
			    }
			}
			
		    WinVMJConsole.println("Final Configuration is VALID! ✅");
		} else {
		    WinVMJConsole.println("Final Configuration is INVALID ❌. Check constraints.");
		}
		
	}

}
