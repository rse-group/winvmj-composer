package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizardPage;

public class ProjectNameWizardPage extends WizardPage {
    private Text projectNameText;
    private Label errorLabel;
    private Button checkButton;
    private IFeatureProject project;
    
    public ProjectNameWizardPage() {
        super("Product Name Page");
        setTitle("Product Name");
        setDescription("Enter Your Product Name:");
    }
    
    public String getProductName() {
        return projectNameText.getText();
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));
        
        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText("Product Name:");
        
        projectNameText = new Text(container, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        checkButton = new Button(container, SWT.PUSH);
        checkButton.setText("Check Name");
        checkButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        
        projectNameText.addVerifyListener(e -> {
            String input = e.text;
            if (!input.matches("[a-zA-Z]*")) {
                e.doit = false;
            }
        });
        
        Label infoLabel = new Label(container, SWT.NONE);
        infoLabel.setText("Only alphabetic characters (A–Z, a–z) are allowed.");
        GridData infoGridData = new GridData();
        infoGridData.horizontalSpan = 3;
        infoLabel.setLayoutData(infoGridData);
        
        errorLabel = new Label(container, SWT.NONE);
        errorLabel.setText("");
        errorLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_RED));
        GridData errorGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        errorGridData.horizontalSpan = 3;
        errorLabel.setLayoutData(errorGridData);
        
        checkButton.addListener(SWT.Selection, event -> {
            validateInput(container);
        });

        setPageComplete(false);
        
        setControl(container);
    }
    
    private void validateInput(Composite container) {
        String name = projectNameText.getText().trim();

        if (doesProjectExist(name)) {
            errorLabel.setText("This name is already taken in the configs folder.");
            errorLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_RED));
            setPageComplete(false);
        } 
        else {
            errorLabel.setText("Name is available.");
            errorLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_GREEN));
            setPageComplete(true);
        }
        errorLabel.getParent().layout(true, true);
    }
    
    private boolean doesProjectExist(String name) {
        if (this.project == null) return false;

        IFolder configFolder = this.project.getConfigFolder();

        try {
			for (IResource res : configFolder.members()) {
			    if (res instanceof IFile) {
			        IFile file = (IFile) res;
			        String fileName = file.getName();

			        if (fileName.endsWith(".xml")) {
			            String baseName = fileName.substring(0, fileName.length() - 4);
			            if (baseName.toLowerCase().equals(name.toLowerCase())) {
			                return true;
			            }
			        }
			    }
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

        return false;
    }


    
    public String getProjectName() {
        return projectNameText.getText().trim();
    }
    
    public void setProject(IFeatureProject project) {
        this.project = project;
    }
}
