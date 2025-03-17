package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

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

import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.ConfigurationAnalyzer;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.base.IFeature;
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
	private Map<String, Object> dataMap = new HashMap<String, Object>();
	private final HashSet<String> featureNames = new HashSet<String>();

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

					FeatureModelFormula formula = new FeatureModelFormula(project.getFeatureModel());
					Configuration configForChecking = new Configuration(formula);
					for (String feature : featureNames) {
						configForChecking.setManual(feature, Selection.SELECTED);
					}

					ConfigurationAnalyzer analyze = new ConfigurationAnalyzer(formula,configForChecking);
					
					if (!analyze.isValid()) {
						validationLabel.setText("Configuration status: Invalid!");
						setPageComplete(false);
					}

					else {
						validationLabel.setText("Configuration status: Valid!");
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

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			if (featuresTree != null) {
				featuresTree.setItemCount(0);
				featureNames.clear();
				final Object featureProject = this.project;
				if (featureProject != null) {
					addFeaturesToTree(((IFeatureProject) featureProject).getFeatureModel().getStructure().getRoot().getFeature());
				} else {
					setErrorMessage("Please select a Project in the previous page.");
					setPageComplete(false);
				}
			}
		}
		super.setVisible(visible);
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
			addFeaturesToTree(feature.getFeature(), item);
		}
		item.setExpanded(true);
	}

	/**
	 * Add the feature name as an item to the tree.
	 *
	 * @param root the feature to add
	 * @param parent the parent item to add the feature as a child
	 */
	private void addFeaturesToTree(IFeature root, TreeItem parent) {
		final TreeItem item = new TreeItem(parent, SWT.NORMAL);
		item.setText(root.getName());
		item.setData(root);
		item.setExpanded(true);

		for (final IFeatureStructure feature : root.getStructure().getChildren()) {
			addFeaturesToTree(feature.getFeature(), item);
		}

		item.setExpanded(true);
	}

	@Override
	protected void putData() {
		abstractWizard.putData(WizardConstants.KEY_OUT_FEATURES, featureNames);
	}

	@Override
	protected String checkPage() {
		if (featureNames.isEmpty()) {
			return "Select at least one feature.";
		}

		FeatureModelFormula formula = new FeatureModelFormula(project.getFeatureModel());
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

