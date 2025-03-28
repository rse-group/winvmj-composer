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
        awsButton.addListener(SWT.Selection, e -> selectedOption = "AWS");

        Button gcpButton = new Button(container, SWT.RADIO);
        gcpButton.setText("GCP");
        gcpButton.addListener(SWT.Selection, e -> selectedOption = "GCP");

        Button onPremButton = new Button(container, SWT.RADIO);
        onPremButton.setText("On-Prem");
        onPremButton.addListener(SWT.Selection, e -> selectedOption = "On-Prem");

        setControl(container);
    }

    public String getSelectedOption() {
        return selectedOption;
    }
}
