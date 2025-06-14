package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.ovgu.featureide.core.winvmj.ui.wizards.DeploymentWizard;

public class SelectDeploymentPage extends WizardPage {
    private String selectedProvider = "None";
    private String selectedArchitecture = "None";
    private String selectedDeploymentMethod = "None";

    private Button dockerButton;
    private Button systemdButton;
    private Button awsButton;
    private Button gcpButton;
    
    private String deploymentTarget = "";


    public SelectDeploymentPage(String pageName) {
        super(pageName);
        setTitle("Deployment Selection");
        setDescription("Choose your cloud provider, architecture, and deployment method.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // === Provider Selection ===
        new Label(container, SWT.NONE).setText("Select your cloud provider:");
        Composite providerGroup = new Composite(container, SWT.NONE);
        providerGroup.setLayout(new GridLayout(1, false));

        awsButton = new Button(providerGroup, SWT.RADIO);
        awsButton.setText("AWS");
        awsButton.addListener(SWT.Selection, e -> {
            selectedProvider = "AWS";
            setPageComplete(isPageComplete());
        });

        gcpButton = new Button(providerGroup, SWT.RADIO);
        gcpButton.setText("GCP");
        gcpButton.addListener(SWT.Selection, e -> {
            selectedProvider = "GCP";
            setPageComplete(isPageComplete());
        });

        // === Architecture Selection ===
        new Label(container, SWT.NONE).setText("Select architecture:");
        Composite architectureGroup = new Composite(container, SWT.NONE);
        architectureGroup.setLayout(new GridLayout(1, false));

        Button monolithButton = new Button(architectureGroup, SWT.RADIO);
        monolithButton.setText("Monolith");
        monolithButton.addListener(SWT.Selection, e -> {
            selectedArchitecture = "monolith";
            dockerButton.setEnabled(true);
            systemdButton.setEnabled(true);
            setPageComplete(isPageComplete());
        });

        Button microserviceButton = new Button(architectureGroup, SWT.RADIO);
        microserviceButton.setText("Microservice");
        microserviceButton.addListener(SWT.Selection, e -> {
            selectedArchitecture = "microservice";
            dockerButton.setEnabled(true);
            dockerButton.setSelection(true);
            selectedDeploymentMethod = "Docker";
            systemdButton.setEnabled(false);
            systemdButton.setSelection(false);
            setPageComplete(isPageComplete());
        });

        // === Deployment Method Selection ===
        new Label(container, SWT.NONE).setText("Select deployment method:");
        Composite methodGroup = new Composite(container, SWT.NONE);
        methodGroup.setLayout(new GridLayout(1, false));

        dockerButton = new Button(methodGroup, SWT.RADIO);
        dockerButton.setText("Docker");
        dockerButton.addListener(SWT.Selection, e -> {
            if (dockerButton.getSelection()) {
                selectedDeploymentMethod = "Docker";
                setPageComplete(isPageComplete());
            }
        });

        systemdButton = new Button(methodGroup, SWT.RADIO);
        systemdButton.setText("Systemd");
        systemdButton.addListener(SWT.Selection, e -> {
            if (systemdButton.getSelection()) {
                selectedDeploymentMethod = "Systemd";
                setPageComplete(isPageComplete());
            }
        });

        setControl(container);
        setPageComplete(false);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        if (visible) {
            DeploymentTargetPage prevPage = ((DeploymentWizard) getWizard()).getDeploymentTargetPage();
            deploymentTarget = prevPage.getSelectedDeploymentTarget();

            boolean isProvisioning = "provisioning".equalsIgnoreCase(deploymentTarget);
            awsButton.setEnabled(isProvisioning);
            gcpButton.setEnabled(isProvisioning);
            awsButton.setSelection(false);
            gcpButton.setSelection(false);

            if (!isProvisioning) {
                selectedProvider = "None";
            }

            setPageComplete(isPageComplete());
        }
    }



    @Override
    public boolean isPageComplete() {
        return (!selectedProvider.equals("None") || deploymentTarget.equals("existing") || deploymentTarget.equals("amanah")) && !selectedArchitecture.equals("None") && !selectedDeploymentMethod.equals("None");
    }

    public String getSelectedProvider() {
        return selectedProvider;
    }

    public String getSelectedArchitecture() {
        return selectedArchitecture;
    }

    public String getSelectedDeploymentMethod() {
        return selectedDeploymentMethod;
    }
} 
