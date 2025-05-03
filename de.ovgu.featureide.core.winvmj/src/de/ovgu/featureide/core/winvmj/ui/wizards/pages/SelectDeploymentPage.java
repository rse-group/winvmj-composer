package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class SelectDeploymentPage extends WizardPage {
    private String selectedOption = "None";

    public SelectDeploymentPage(String pageName) {
        super(pageName);
        setTitle("Deployment Selection");
        setDescription("Choose a deployment option: AWS, GCP, or On-Prem.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        new Label(container, SWT.NONE).setText("Select your deployment target:");

        Button awsButton = new Button(container, SWT.RADIO);
        awsButton.setText("AWS");
        awsButton.addListener(SWT.Selection, e -> {
            selectedOption = "AWS";
            setPageComplete(isPageComplete());
        });

        Button gcpButton = new Button(container, SWT.RADIO);
        gcpButton.setText("GCP");
        gcpButton.addListener(SWT.Selection, e -> {
            selectedOption = "GCP";
            setPageComplete(isPageComplete());
        });

        Button onPremButton = new Button(container, SWT.RADIO);
        onPremButton.setText("On-Prem (Amanah Server)");
        onPremButton.addListener(SWT.Selection, e -> {
            selectedOption = "amanah";
            setPageComplete(isPageComplete());
        });
        
        Button microServiceButton = new Button(container, SWT.RADIO);
        microServiceButton.setText("Microservice (Docker)");
        microServiceButton.addListener(SWT.Selection, e -> {
            selectedOption = "microservice";
            setPageComplete(isPageComplete());
        });

        setControl(container);
        setPageComplete(false);
    }

    @Override
    public boolean isPageComplete() {
        return !selectedOption.equals("None");
    }


    public String getSelectedOption() {
        return selectedOption;
    }
    


}
