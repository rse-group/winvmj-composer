package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.ovgu.featureide.core.winvmj.templates.impl.MultiStageConfiguration;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.base.IConstraint;
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
import org.eclipse.swt.widgets.Button;

public class SelectAllUvlWizardPage extends AbstractWizardPage {
    private String selectedFile;
    private String selectedAvailFile;
    private ListViewer listViewerAvail;
    private ListViewer listViewerSelected;
    private IFeatureProject project;
    private MultiStageConfiguration multiStageConfiguration = new MultiStageConfiguration();
    private final Map<String, Object> dataMap = new HashMap<String, Object>();
    private boolean isFilteredUvlSelected = false;

    public SelectAllUvlWizardPage() {
        super("Feature Wizard Page");
        setTitle("Feature Selection Wizard");
        setDescription("Select all the wanted Model UVL first!");
    }

    public Map<String, Object> getDataMap() {
        return this.dataMap;
    }

    public void setProject(IFeatureProject project) {
        this.project = project;
    }

    public boolean getFilter() {
        return isFilteredUvlSelected;
    }
    


    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        Label labelAvailable = new Label(container, SWT.NONE);
        labelAvailable.setText("Available UVL Models:");
        labelAvailable.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        new Label(container, SWT.NONE);

        Label labelSelected = new Label(container, SWT.NONE);
        labelSelected.setText("Selected UVL Models:");
        labelSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        listViewerAvail = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
        List listViewerData = listViewerAvail.getList();
        listViewerData.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        for (String file : findUvlFiles()) {
            listViewerData.add(file);
        }

        Composite buttonComposite = new Composite(container, SWT.NONE);
        buttonComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        buttonComposite.setLayout(new GridLayout(1, false));
        
        Button addButton = new Button(buttonComposite, SWT.PUSH);
        addButton.setText("Add");
        addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button removeButton = new Button(buttonComposite, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button moveUpButton = new Button(buttonComposite, SWT.PUSH);
        moveUpButton.setText("Move Up");
        moveUpButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button moveDownButton = new Button(buttonComposite, SWT.PUSH);
        moveDownButton.setText("Move Down");
        moveDownButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Button filteredUvlCheckbox = new Button(buttonComposite, SWT.CHECK);
        filteredUvlCheckbox.setText("Filtered UVL");
        filteredUvlCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        filteredUvlCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                isFilteredUvlSelected = filteredUvlCheckbox.getSelection();
                updatePage();
            }
        });

        listViewerSelected = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
        listViewerSelected.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        listViewerSelected.getList().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectedIndex = listViewerSelected.getList().getSelectionIndex();
                if (selectedIndex >= 0) {
                    selectedFile = listViewerSelected.getList().getItem(selectedIndex);
                    listViewerData.deselectAll();
                }

                updatePage();
            }
        });

        listViewerData.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectedIndex = listViewerData.getSelectionIndex();
                if (selectedIndex >= 0) {
                    selectedAvailFile = listViewerData.getItem(selectedIndex);
                    listViewerSelected.getList().deselectAll();
                }

                updatePage();
            }
        });

        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (selectedAvailFile != null) {
                    listViewerSelected.getList().add(selectedAvailFile);
                    listViewerAvail.getList().remove(selectedAvailFile);
                    selectedAvailFile = null;
                }

                updatePage();
            }
        });

        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (selectedFile != null) {
                    listViewerSelected.getList().remove(selectedFile);
                    listViewerAvail.getList().add(selectedFile);
                    selectedFile = null;
                }

                updatePage();
            }
        });

        moveUpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List activeList;
                if (listViewerAvail.getList().getSelectionCount() > 0 ) {
                    activeList = listViewerAvail.getList();
                }

                else if (listViewerSelected.getList().getSelectionCount() > 0) {
                    activeList = listViewerSelected.getList();
                }

                else  {
                    activeList = null;
                }

                if (activeList != null) {
                    int selectedIndex = activeList.getSelectionIndex();
                    if (selectedIndex > 0) {
                        String selectedItem = activeList.getItem(selectedIndex);
                        String itemAbove = activeList.getItem(selectedIndex - 1);
                        activeList.setItem(selectedIndex - 1, selectedItem);
                        activeList.setItem(selectedIndex, itemAbove);
                        activeList.deselectAll();
                    }
                }
                updatePage();
            }
        });

        moveDownButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List activeList;
                if (listViewerAvail.getList().getSelectionCount() > 0 ) {
                    activeList = listViewerAvail.getList();
                }

                else if (listViewerSelected.getList().getSelectionCount() > 0) {
                    activeList = listViewerSelected.getList();
                }

                else  {
                    activeList = null;
                }

                if (activeList != null) {
                    int selectedIndex = activeList.getSelectionIndex();
                    if (selectedIndex < activeList.getItemCount() - 1) {
                        String selectedItem = activeList.getItem(selectedIndex);
                        String itemBelow = activeList.getItem(selectedIndex + 1);
                        activeList.setItem(selectedIndex + 1, selectedItem);
                        activeList.setItem(selectedIndex, itemBelow);
                        activeList.deselectAll();
                    }
                }
                updatePage();
            }
        });

        setPageComplete(true);
        setControl(container);
    }

    @Override
    protected void putData() {
        dataMap.put(WizardConstants.KEY_OUT_PROJECT, this.project);
        // dataMap.put("selectedFile", selectedFile);
    }

    @Override
    public boolean canFlipToNextPage() {
        return !getSelected().isEmpty();
    }

    private ArrayList<String> findUvlFiles() {
        ArrayList<String> list = new ArrayList<>();
        for (IFile file : multiStageConfiguration.getAllFeatureModelNames(this.project)) {
            list.add(file.getName());
        }
        return list;
    }

    public ArrayList<String> getSelected() {
        ArrayList<String> uvlModels = new ArrayList<>();
        for (String uvlModel : listViewerSelected.getList().getItems()) {
            uvlModels.add(uvlModel);
        }

        return uvlModels;
    }
}
