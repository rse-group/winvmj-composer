package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import de.ovgu.featureide.core.winvmj.ui.wizards.DeploymentWizard;

public class CredentialAndProductPage extends WizardPage {

    private Text credentialFileText;
    private Text productFileText;
    private Text privKeyText;
    private Text pubKeyText;
    private Button browsePubKeyButton;
    private Button browseCredButton;
    private String deploymentTarget;

    public CredentialAndProductPage(String pageName) {
        super(pageName);
        setTitle("Select Credential, Product and Key Files");
        setDescription("Choose the credential (.json product (.zip), and key (public and private) files.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        // Credential file
        new Label(container, SWT.NONE).setText("Credential File (.json):");
        credentialFileText = new Text(container, SWT.BORDER);
        credentialFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        browseCredButton = new Button(container, SWT.PUSH);
        browseCredButton.setText("Browse...");
        browseCredButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            dialog.setFilterExtensions(new String[]{"*.json"});
            String selected = dialog.open();
            if (selected != null) {
                credentialFileText.setText(selected);
                setPageComplete(isPageComplete());
            }
        });

        // Product file
        new Label(container, SWT.NONE).setText("Product File (.zip):");
        productFileText = new Text(container, SWT.BORDER);
        productFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button browseProductButton = new Button(container, SWT.PUSH);
        browseProductButton.setText("Browse...");
        browseProductButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            dialog.setFilterExtensions(new String[]{"*.zip"});
            String selected = dialog.open();
            if (selected != null) {
                productFileText.setText(selected);
                setPageComplete(isPageComplete());
            }
        });
        
        // Private key file
        new Label(container, SWT.NONE).setText("Private Key File:");
        privKeyText = new Text(container, SWT.BORDER);
        privKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button browsePrivKeyButton = new Button(container, SWT.PUSH);
        browsePrivKeyButton.setText("Browse...");
        browsePrivKeyButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            String selected = dialog.open();
            if (selected != null) {
            	privKeyText.setText(selected);
                setPageComplete(isPageComplete());
            }
        });
        
        // Public key file
        new Label(container, SWT.NONE).setText("Public Key File:");
        pubKeyText = new Text(container, SWT.BORDER);
        pubKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        browsePubKeyButton = new Button(container, SWT.PUSH);
        browsePubKeyButton.setText("Browse...");
        browsePubKeyButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            String selected = dialog.open();
            if (selected != null) {
            	pubKeyText.setText(selected);
                setPageComplete(isPageComplete());
            }
        });

        setControl(container);
        setPageComplete(false);
    }

    @Override
    public boolean isPageComplete() {
        return (!credentialFileText.getText().trim().isEmpty() &&
               credentialFileText.getText().endsWith(".json") || "existing".equals(deploymentTarget)) &&
               !productFileText.getText().trim().isEmpty() &&
               productFileText.getText().endsWith(".zip") &&
               !privKeyText.getText().trim().isEmpty() &&
               (!pubKeyText.getText().trim().isEmpty() || "existing".equals(deploymentTarget)) ;
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            deploymentTarget = ((DeploymentWizard) getWizard()).getDeploymentTargetPage().getSelectedDeploymentTarget();
                          
            if ("provisioning".equalsIgnoreCase(deploymentTarget)) {
            	pubKeyText.setEnabled(true);
            	browsePubKeyButton.setEnabled(true);
            	credentialFileText.setEnabled(true);
            	browseCredButton.setEnabled(true);
            } else if ("existing".equalsIgnoreCase(deploymentTarget)) {
            	pubKeyText.setEnabled(false);
            	pubKeyText.setText("");
            	browsePubKeyButton.setEnabled(false);
            	credentialFileText.setEnabled(false);
            	credentialFileText.setText("");
            	browseCredButton.setEnabled(false);
            }

            setPageComplete(isPageComplete());
        }
    }

    public String getCredentialFilePath() {
        return credentialFileText.getText();
    }

    public String getProductFilePath() {
        return productFileText.getText();
    }
    
    public String getPrivKeyFilePath() {
        return privKeyText.getText();
    }
    
    public String getPubKeyFilePath() {
        return pubKeyText.getText();
    }
}
