package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import de.ovgu.featureide.core.winvmj.ui.wizards.DeploymentWizard;

public class DeploymentConfigPage extends WizardPage {

    private Text usernameText;
    private Combo machineTypeCombo;
    private Combo regionCombo;
    private Label regionLabel;
    private Text certificateNameText;
    private Text nginxCertNameText;
    private Text instanceNameText;
    private Text productPrefixText;
    private Text productNameText;


    public DeploymentConfigPage(String pageName) {
        super(pageName);
        setTitle("Deployment Configuration");
        setDescription("Fill in the deployment configuration fields.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false)); // 2 columns: label + input

        usernameText = createField(container, "Username:");
        machineTypeCombo = createComboField(container, "Machine Type:", new String[]{"SMALL", "MEDIUM", "LARGE"});
        
        regionLabel = new Label(container, SWT.NONE);
        regionLabel.setText("Region:");
        regionCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        regionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        regionCombo.setItems(new String[]{"US", "SINGAPORE", "EUROPE", "JAKARTA"});

        certificateNameText = createField(container, "Certificate Name:");
        nginxCertNameText = createField(container, "NGINX Certificate Name:");
        instanceNameText = createField(container, "Instance Name:");
        productPrefixText = createField(container, "Product Prefix:");
        productNameText = createField(container, "Product Name:");


        Listener validationListener = e -> setPageComplete(isPageComplete());

        // Attach listeners for validation
        usernameText.addListener(SWT.Modify, validationListener);
        machineTypeCombo.addListener(SWT.Selection, validationListener);
        regionCombo.addListener(SWT.Selection, validationListener);
        certificateNameText.addListener(SWT.Modify, validationListener);
        nginxCertNameText.addListener(SWT.Modify, validationListener);
        instanceNameText.addListener(SWT.Modify, validationListener);
        productPrefixText.addListener(SWT.Modify, validationListener);
        productNameText.addListener(SWT.Modify, validationListener);

        setControl(container);
        setPageComplete(false);
    }

    private Text createField(Composite parent, String labelText) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText);

        Text text = new Text(parent, SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return text;
    }

    private Combo createComboField(Composite parent, String labelText, String[] items) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText);

        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setItems(items);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return combo;
    }

    @Override
    public boolean isPageComplete() {
        boolean regionValid = !regionCombo.isVisible() || regionCombo.getSelectionIndex() != -1;

        return !usernameText.getText().trim().isEmpty()
                && machineTypeCombo.getSelectionIndex() != -1
                && regionValid
                && !certificateNameText.getText().trim().isEmpty()
                && !nginxCertNameText.getText().trim().isEmpty()
                && !instanceNameText.getText().trim().isEmpty()
                && !productPrefixText.getText().trim().isEmpty()
        		&& !productNameText.getText().trim().isEmpty();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            String deploymentTarget = ((DeploymentWizard) getWizard()).getDeploymentPage().getSelectedOption();

            if ("AWS".equalsIgnoreCase(deploymentTarget)) {
                regionCombo.setItems(new String[] { "US", "EUROPE", "SINGAPORE" });
                regionLabel.setVisible(true);
                regionCombo.setVisible(true);
            } else if ("GCP".equalsIgnoreCase(deploymentTarget)) {
                regionCombo.setItems(new String[] { "US", "EUROPE", "JAKARTA" });
                regionLabel.setVisible(true);
                regionCombo.setVisible(true);
            } else if ("On-Prem".equalsIgnoreCase(deploymentTarget)) {
                regionCombo.setItems(new String[0]);
                regionLabel.setVisible(false);
                regionCombo.setVisible(false);
            }

            if (regionCombo.isVisible() && regionCombo.getItemCount() > 0) {
                regionCombo.select(0); // preselect first item
            }

            setPageComplete(isPageComplete());
        }
    }

    // Getters
    public String getUsername() {
        return usernameText.getText();
    }

    public String getMachineType() {
        return machineTypeCombo.getText();
    }

    public String getRegion() {
        return regionCombo.isVisible() ? regionCombo.getText() : "";
    }

    public String getCertificateName() {
        return certificateNameText.getText();
    }

    public String getNginxCertificateName() {
        return nginxCertNameText.getText();
    }

    public String getInstanceName() {
        return instanceNameText.getText();
    }

    public String getProductPrefix() {
        return productPrefixText.getText();
    }
    
    public String getProductName() {
    	return productNameText.getText();
    }
}
