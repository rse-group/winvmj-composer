package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.ovgu.featureide.core.winvmj.templates.impl.MultiStageConfiguration;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizardPage;
import de.ovgu.featureide.fm.ui.wizards.WizardConstants;

import org.eclipse.core.resources.IFile;
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
    private MultiStageConfiguration multiStageConfiguration = new MultiStageConfiguration();
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

        for (String file : findUvlFiles()) {
            listViewerData.add(file);
        }

        listViewerData.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectedIndex = listViewerData.getSelectionIndex();
                if (selectedIndex >= 0) {
                    selectedFile = listViewerData.getItem(selectedIndex);
                }
                dataMap.put("selectedFile", selectedFile);
            }
        });
        
        setPageComplete(true);
        setControl(container);
    }

    @Override
    protected void putData() {
        dataMap.put(WizardConstants.KEY_OUT_PROJECT, this.project);
        dataMap.put("selectedFile", selectedFile);
    }

    private ArrayList<String> findUvlFiles() {
        ArrayList<String> fileList = new ArrayList<>();
        for (IFile file : multiStageConfiguration.getAllFeatureModelNames(this.project)) {
            fileList.add(file.getName());
        }
        // fileList.add(this.project.getModelFile().getName());
        return fileList;
    }
    
}
