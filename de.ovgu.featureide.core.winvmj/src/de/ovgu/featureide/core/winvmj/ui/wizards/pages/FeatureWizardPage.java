package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import java.util.HashMap;
import java.util.Map;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizardPage;
import de.ovgu.featureide.fm.ui.wizards.WizardConstants;

import org.eclipse.swt.widgets.List;
import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class FeatureWizardPage extends AbstractWizardPage {
    private ListViewer listViewer;
    private String selectedFile;
    private IFeatureProject project;
    private final Map<String, Object> dataMap = new HashMap<String, Object>();

    public FeatureWizardPage() {
        super("Feature Wizard Page");
        setTitle("Feature Selection Wizard");
        setDescription("Select the Model UVL first!");
    }

    public Map<String, Object> getDataMap() {
        return this.dataMap;
    }

    public void setProject(IFeatureProject project) {
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        Label label = new Label(container, SWT.NONE);
        label.setText("Available UVL Models:");

        listViewer = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL);
        List listViewerData = listViewer.getList();
        listViewerData.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        List uvlFiles = findUvlFiles(container);
        for (String file : uvlFiles.getItems()) {
            listViewerData.add(file);
        }
        setPageComplete(true);
        setControl(container);
    }

    @Override
    protected void putData() {
        dataMap.put(WizardConstants.KEY_OUT_PROJECT, this.project);
        dataMap.put("selectedUvlFile", selectedFile);
    }

    private List findUvlFiles(Composite container) {
        List fileList = new List(container, SWT.BORDER | SWT.V_SCROLL);
        fileList.add(this.project.getModelFile().getName());
        return fileList;
    }
}
