package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class DeploymentTargetPage extends WizardPage {
    private String selectedDeploymentTarget = "None";

    public DeploymentTargetPage(String pageName) {
        super(pageName);
        setTitle("Deployment Selection");
        setDescription("Choose a deployment target: Existing VM, Provision VM, or Amanah Server.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        new Label(container, SWT.NONE).setText("Select your deployment target:");

        Button existButton = new Button(container, SWT.RADIO);
        existButton.setText("Existing VM");
        existButton.addListener(SWT.Selection, e -> {
        	selectedDeploymentTarget = "existing";
            setPageComplete(isPageComplete());
        });

        Button provisionButton = new Button(container, SWT.RADIO);
        provisionButton.setText("Provision VM");
        provisionButton.addListener(SWT.Selection, e -> {
        	selectedDeploymentTarget = "provisioning";
            setPageComplete(isPageComplete());
        });

        Button amanahButton = new Button(container, SWT.RADIO);
        amanahButton.setText("Amanah Server");
        amanahButton.addListener(SWT.Selection, e -> {
        	selectedDeploymentTarget = "amanah";
            setPageComplete(isPageComplete());
        });
        

        setControl(container);
        setPageComplete(false);
    }

    @Override
    public boolean isPageComplete() {
        return !selectedDeploymentTarget.equals("None");
    }


    public String getSelectedDeploymentTarget() {
        return selectedDeploymentTarget;
    }
    


}
