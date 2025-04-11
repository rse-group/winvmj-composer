package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */

import java.util.HashSet;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.IWizardPage;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.solver.AdvancedSatSolver;
import de.ovgu.featureide.fm.core.analysis.cnf.solver.ISatSolver.SelectionStrategy;
import de.ovgu.featureide.fm.core.analysis.cnf.solver.ISimpleSatSolver.SatResult;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.ConfigurationAnalyzer;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.io.manager.IFeatureModelManager;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.MultiStageConfiguration;
import de.ovgu.featureide.core.winvmj.ui.wizards.FeatureWizard;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizardPage;
import de.ovgu.featureide.fm.ui.wizards.WizardConstants;
import de.ovgu.featureide.core.IFeatureProject;

/**
 * A Wizard Page to select the features from the other project to create the interface.
 *
 * @author Christoph Giesel
 * @author Sebastian Krieter
 */
public class SelectFeaturesWizardPage extends AbstractWizardPage {

	private IFeatureProject project;
	private Tree featuresTree;
	private IFile selectedFile;
	private MultiStageConfiguration multiStageConfiguration = new MultiStageConfiguration();
	private Map<String, Object> dataMap = new HashMap<String, Object>();
	private final HashSet<String> featureNames = new HashSet<String>();
	private HashSet<String> allowedParentFeatures = new HashSet<String>();
	private boolean isFirst = false;
	private FeatureWizard featureWizard;
	private SelectAllUvlWizardPage selectAllUvlWizardPage;
	private Label noFeaturesLabel;

    public HashSet<String> getFeatureNames(){
    	return featureNames;
    }
    
	public SelectFeaturesWizardPage() {
		super("Select Features");
		setTitle("Select Features");
		setDescription("Select the features you want to have in the product!");
	}

	public void setProject(IFeatureProject project){
		this.project = project;
	}

	public void setDataMap(Map<String, Object> map) {
		this.dataMap = map;
	}

	public void setFirst() {
		isFirst = true;
	}

    
	public void setAllowedParent(HashSet<String> allowedParent) {
		this.allowedParentFeatures = allowedParent;
	}

	public HashSet<String> getAllowedParent() {
		return this.featureNames;
	}

	public String getSelectedFile() {
		return this.selectedFile.getName();
	}

	public void setSelectedFile() {
		for (IFile file : multiStageConfiguration.getAllFeatureModelNames(this.project)) {
			if (file.getName().equals(dataMap.get("selectedFile"))) {
				this.selectedFile = file;
			}
		}
	}

	public void setSelectedFile(String selectedFile) {
		for (IFile file : multiStageConfiguration.getAllFeatureModelNames(this.project)) {
			if (file.getName().equals(selectedFile)) {
				this.selectedFile = file;
			}
		}
	}

	@Override
	public void createControl(Composite parent) {		
		final Composite container = new Composite(parent, SWT.NONE);

		final GridLayout layout = new GridLayout();
		container.setLayout(layout);
		setControl(container);

		Label validationLabel = new Label(container, SWT.NONE);
		validationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    	validationLabel.setText("Configuration status: Unknown");

		featuresTree = new Tree(container, SWT.MULTI | SWT.CHECK);
		featuresTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		noFeaturesLabel = new Label(container, SWT.NONE);
		noFeaturesLabel.setText("No features available to select for this level.");
		noFeaturesLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_BLUE));
		noFeaturesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		noFeaturesLabel.setVisible(false);
	    
		featuresTree.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.CHECK) {
					final TreeItem item = (TreeItem) e.item;
					if (item.getChecked()) {
						featureNames.add(item.getText());
					} else {
						featureNames.remove(item.getText());
					}

					FeatureModelFormula formula = new FeatureModelFormula(multiStageConfiguration.loadFeatureModel(selectedFile));
					Configuration configForChecking = new Configuration(formula);
					for (String feature : featureNames) {
						configForChecking.setManual(feature, Selection.SELECTED);
					}
					
					ConfigurationAnalyzer analyze = new ConfigurationAnalyzer(formula,configForChecking);
					
					if (!analyze.isValid()) {
						validationLabel.setText("Local Configuration status: Invalid!");
						validationLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_RED));
						setPageComplete(false);
					}

					else {
						validationLabel.setText("Local Configuration status: Valid!");
						validationLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_GREEN));
						
						setPageComplete(true);
					}
					validationLabel.getParent().layout();
					updatePage();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				updatePage();
			}
		});

		final Composite buttonGroup = new Composite(container, 0);
		buttonGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		buttonGroup.setLayout(gridLayout);

		final Button selectAllButton = new Button(buttonGroup, SWT.PUSH);
		selectAllButton.setText("Select All");
		selectAllButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				checkItems(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		final Button deselectAllButton = new Button(buttonGroup, SWT.PUSH);
		deselectAllButton.setText("Deselect All");
		deselectAllButton.addSelectionListener(new SelectionListener() {


			@Override
			public void widgetSelected(SelectionEvent e) {
				checkItems(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		// buttonGroup.pack();
		// container.pack();
		setPageComplete(false);
	}

	private void checkItems(boolean checkStatus) {
		final TreeItem[] items = featuresTree.getItems();
		for (int i = 0; i < items.length; i++) {
			check(items[i], checkStatus);
		}
		updatePage();
	}

	private void check(TreeItem parent, boolean checkStatus) {
		parent.setChecked(checkStatus);
		if (checkStatus) {
			featureNames.add(parent.getText());
		} else {
			featureNames.remove(parent.getText());
		}
		final TreeItem[] items = parent.getItems();
		for (int i = 0; i < items.length; i++) {
			check(items[i], checkStatus);
		}
	}
	
	private void clearCheckedStateRecursive(TreeItem item) {
		item.setChecked(false);
		for (TreeItem child : item.getItems()) {
			clearCheckedStateRecursive(child);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			if (featuresTree != null) {
				featuresTree.removeAll();
				final Object featureProject = this.project;
				if (featureProject != null) {
					WinVMJConsole.println(allowedParentFeatures.toString());
					if (allowedParentFeatures.isEmpty()) {
						addFeaturesToTree(multiStageConfiguration.loadFeatureModel(selectedFile).getStructure().getRoot().getFeature());
					}

					else {
						HashSet<String> filteredFeatures = filteredFeaturesBasedOnConstraint(allowedParentFeatures);
					    
						addFilteredFeaturesToTree(multiStageConfiguration.loadFeatureModel(selectedFile).getStructure().getRoot().getFeature(), filteredFeatures);
					}
					
					for (TreeItem item : featuresTree.getItems()) {
						WinVMJConsole.println("item " + item);
						clearCheckedStateRecursive(item);
					}
					
					restoreCheckedState(featuresTree.getItems());
					
//					if (featuresTree.getItemCount() == 0) {
//						noFeaturesLabel.setVisible(true);
//						setPageComplete(true);
//					} else {
//						noFeaturesLabel.setVisible(false);
//						setPageComplete(false); 
//					}
					
				} 
				else {
					setErrorMessage("Please select a Project in the previous page.");
					setPageComplete(false);
				}
			}
		}		
//		else {
//			IWizardPage current = getWizard().getContainer().getCurrentPage();
//			FeatureWizard wizard = (FeatureWizard) getWizard();
//
//			int currentIndex = wizard.getPageIndex(current);
//			int myIndex = wizard.getPageIndex(this);
//
//			WinVMJConsole.println("currentIndex " + currentIndex);
//			WinVMJConsole.println("myIndex " + myIndex);
//			if (currentIndex < myIndex) {
//				Map<String, HashSet<String>> selectedFeaturesMap = wizard.getSelectedFeaturesMap();
//				if (selectedFeaturesMap != null) {
//					WinVMJConsole.println("Looking for selectedFile key: " + getSelectedFile());
//					WinVMJConsole.println("Before delete selectedFeatures map keys: " + selectedFeaturesMap.keySet());
//					
//					selectedFeaturesMap.remove(getSelectedFile());
//					
//					WinVMJConsole.println("After delete selectedFeatures map keys: " + selectedFeaturesMap.keySet());
//					WinVMJConsole.println("🔄 Going back — removed selections for: " + getSelectedFile());
//				}
//			}
//		}
		super.setVisible(visible);
	}
	

	private void restoreCheckedState(TreeItem[] items) {
		for (TreeItem item : items) {
			if (featureNames.contains(item.getText())) {
				item.setChecked(true);
			}

			if (item.getItemCount() > 0) {
				restoreCheckedState(item.getItems());
			}
		}
	}

	/**
	 * Add the feature name as an item to the tree.
	 *
	 * @param root the feature to add
	 */
	private void addFeaturesToTree(IFeature root) {
		final TreeItem item = new TreeItem(featuresTree, SWT.NORMAL);
		item.setText(root.getName());
		item.setData(root);

		for (final IFeatureStructure feature : root.getStructure().getChildren()) {
			addFeaturesToTree(feature.getFeature(), item, null);
		}
		item.setExpanded(true);
	}

	/**
	 * Add the feature name as an item to the tree.
	 *
	 * @param root the feature to add
	 * @param parent the parent item to add the feature as a child
	 */
	private void addFeaturesToTree(IFeature root, TreeItem parent, HashSet<String> combinedFeatures) {
		final TreeItem item = new TreeItem(parent, SWT.NORMAL);
		item.setText(root.getName());
		item.setData(root);
		item.setExpanded(true);

		for (final IFeatureStructure feature : root.getStructure().getChildren()) {
			if (combinedFeatures == null) {
				addFeaturesToTree(feature.getFeature(), item, null);
			}
			else if (combinedFeatures.contains(feature.getFeature().getName())) {
				addFeaturesToTree(feature.getFeature(), item, combinedFeatures);
			}
		}

		item.setExpanded(true);
	}

	private void addFilteredFeaturesToTree(IFeature root, HashSet<String> combinedFeatures) {
		if (combinedFeatures.contains(root.getName())) {
			final TreeItem item = new TreeItem(featuresTree, SWT.NORMAL);
			item.setText(root.getName());
			item.setData(root);
			
			for (final IFeatureStructure feature : root.getStructure().getChildren()) {
				if (combinedFeatures.contains(feature.getFeature().getName())) {
					addFeaturesToTree(feature.getFeature(), item, combinedFeatures);
				}
			}
			item.setExpanded(true);
		} 
		else {
			for (final IFeatureStructure feature : root.getStructure().getChildren()) {
				addFilteredFeaturesToTree(feature.getFeature(), combinedFeatures);
			}
		}		
	}
	
	private Map<String, String> getFeatureNameMapping(FeatureModelFormula formula) {
		Map<String, String> mapping = new HashMap<>();

		for (IFeature feature : formula.getFeatureModel().getStructure().getFeaturesPreorder()) {
			String fullName = feature.getName();       
			String shortName = fullName.contains(".") ? fullName.substring(fullName.indexOf('.') + 1) : fullName;
			mapping.put(shortName, fullName);          
		}

		return mapping;
	}
	
	private HashSet<String> filteredFeaturesBasedOnConstraint(HashSet<String> targetFeatures) {
		HashSet<String> selectedFeatures = new HashSet<>();
		
		FeatureModelFormula formula = new FeatureModelFormula(project.getFeatureModel());
		CNF cnf = formula.getCNF();
		AdvancedSatSolver solver = new AdvancedSatSolver(cnf);
		Map<String, String> nameMap = getFeatureNameMapping(formula);
		for (String shortName : targetFeatures) {
			String fullName = nameMap.get(shortName);
			if (fullName != null) {
				int literal = solver.getSatInstance().getVariables().getVariable(fullName, true);
				solver.assignmentPush(literal);
			}
		}
		
		if (solver.hasSolution() == SatResult.TRUE) {
			solver.setSelectionStrategy(SelectionStrategy.ORG);
			int[] findsolutionORG = solver.findSolution();
			for (int lit : findsolutionORG) {
				String fullName = solver.getSatInstance().getVariables().getName(lit);
				if (lit > 0) {
					String shortName = fullName.contains(".") ? fullName.substring(fullName.indexOf('.') + 1) : fullName;
					if (!targetFeatures.contains(shortName)) {
						selectedFeatures.add(shortName);
					}
				}
			}
		}
		
		String rootFullName = project.getFeatureModel().getStructure().getRoot().getFeature().getName();
		String rootShortName = rootFullName.contains(".") ? rootFullName.substring(rootFullName.indexOf('.') + 1) : rootFullName;
		selectedFeatures.remove(rootShortName);
		
		WinVMJConsole.println("selectedFeatures " + selectedFeatures);
		return selectedFeatures;
	}
	
	
//	private HashSet<String> filteredFeaturesBasedOnConstraint(HashSet<String> targetFeatures) {
//		HashSet<String> selectedFeatures = new HashSet<>();
//		
//		WinVMJConsole.println("Constraints Count" +  project.getFeatureModel().getConstraintCount());
//		WinVMJConsole.println("Constraints" +  project.getFeatureModel().getConstraints());
//		for (IConstraint constraint : project.getFeatureModel().getConstraints()) {
//		    String node = constraint.getNode().toString();
//		    String[] parts = node.split("=>");
//
//		    if (parts.length == 2) {
//		        String leftSide = parts[0].trim();
//		        String rightSide = parts[1].trim();
//
//		        HashSet<String> rightFeatures = new HashSet<>();
//		        for (String feature : rightSide.split("\\|")) {
//		            feature = feature.trim();
//		            if (feature.contains(".")) {
//		                feature = feature.substring(feature.indexOf('.') + 1); 
//		            }
//		            rightFeatures.add(feature);
//		        }
//		        if (!Collections.disjoint(targetFeatures, rightFeatures)) {
//		            for (String feature : leftSide.split("\\|")) {
//		                feature = feature.trim();
//		                if (feature.contains(".")) {
//		                    feature = feature.substring(feature.indexOf('.') + 1); 
//		                }
//		                selectedFeatures.add(feature);
//		            }
//		        }
//		    }
//		}
//		return selectedFeatures;
//	}
	
//	private HashSet<String> getRequiredParents(HashSet<String> filteredFeatures) {
//	    HashSet<String> requiredParents = new HashSet<>();
//	    
//	    Collection<IFeature> features = multiStageConfiguration.loadFeatureModel(selectedFile).getStructure().getFeaturesPreorder();
//	    for (IFeature feature : features) {
//	    	if (feature.getStructure().getParent() == null) {
//	    		requiredParents.add(feature.getName());
//	    	}
//	    	else if (filteredFeatures.contains(feature.getName()) && feature.getStructure().getParent() != null) {
//	            requiredParents.add(feature.getStructure().getParent().getFeature().getName());
//	        }
//	    }
//	    return requiredParents;
//	}



	@Override
	protected void putData() {
		abstractWizard.putData(WizardConstants.KEY_OUT_FEATURES, featureNames);
	}

	@Override
	protected String checkPage() {
		if (featureNames.isEmpty()) {
			return "Select at least one feature.";
		}

		FeatureModelFormula formula = new FeatureModelFormula(multiStageConfiguration.loadFeatureModel(selectedFile));
		Configuration configForChecking = new Configuration(formula);
		for (String feature : featureNames) {
			configForChecking.setManual(feature, Selection.SELECTED);
		}

		ConfigurationAnalyzer analyze = new ConfigurationAnalyzer(formula,configForChecking);
		
		if (!analyze.isValid()) {
			return "Configuration is not valid!";
		}

		return null;
	}

	public HashSet<String> getFeatureName() {
		return this.featureNames;
	}
}

