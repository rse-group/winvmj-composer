package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.core.impl.ProductToCompose;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.handlers.LegacyDeploymentToscaHandler;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.LegacyDeploymentToscaWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.LegacyDeploymentWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.NFRDefinition;

public class LegacyCredentialToscaPage extends WizardPage {

    private Text credentialFileText;
    private Text frontendProductFileText;
    private Text productFileText;
    private Text privKeyText;
    private Text pubKeyText;
    private Button browsePubKeyButton;
    private Button browseCredButton;
    private String deploymentTarget;
    private boolean isV2 = false;
    private String defaultProductPath;
    private IFeatureProject project;
    
    // TOSCA mode flag
    private boolean isToscaMode = false;
    
    // TOSCA-specific fields
    private Combo providerCombo;
    private Text regionText;
    private Text templateName;
    private Text toscaTemplatePath;
    
    // Database fields
    private Text dbNameText;
    private Text dbUsernameText;
    private Text dbPasswordText;
    
    // AWS-specific fields
    private Group awsGroup;
    private Text awsAccessKeyText;
    private Text awsSecretKeyText;
    private Text awsSessionTokenText;
    private Text awsSshKeyNameText;
    
    // GCP-specific fields
    private Group gcpGroup;
    private Text gcpServiceAccountText;
    private Text gcpProjectText;
    private Text gcpZoneText;
    private Text gcpSshUserText;

    public LegacyCredentialToscaPage(String pageName) {
        super(pageName);
        setTitle("Select Credential, Product and Key Files");
        setDescription("Choose the credential (.json), product (.zip), and key (public and private) files.");
    }

    public LegacyCredentialToscaPage() {
        super("Credential Input");
        defaultProductPath = LegacyDeploymentToscaHandler.getZipPath();
        isV2 = true;
        setTitle("Select Credential and Key Files");
        setDescription("Choose the credential (.json) and key (public and private) files.");
    }
    
    public LegacyCredentialToscaPage(boolean toscaMode, IFeatureProject project) {
        super("Credential Input");
        defaultProductPath = LegacyDeploymentToscaHandler.getZipPath();
        this.project = project;
        isV2 = true;
        isToscaMode = toscaMode;
        setTitle("TOSCA Deployment Configuration");
        setDescription("Configure cloud credentials and deployment settings for TOSCA-based deployment.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        if (isToscaMode) {
            createToscaFields(container);
        } else {
            createOriginalFields(container);
        }

        setControl(container);
        setPageComplete(false);
    }
    
    private void createToscaFields(Composite container) {
        // === Cloud Provider Section ===
        createSectionLabel(container, "Cloud Provider Configuration");
        
        createLabel(container, "Provider:");
        providerCombo = new Combo(container, SWT.READ_ONLY);
        providerCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        providerCombo.setEnabled(false);
        providerCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateProviderFields();
            }
        });
        
        createLabel(container, "Region:");
        regionText = new Text(container, SWT.BORDER);
        regionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        regionText.setEnabled(false);
        
        // === Product Configuration ===
        createSeparator(container);
        createSectionLabel(container, "Product Configuration");
        
        createLabel(container, "Product File (.zip):");
        productFileText = new Text(container, SWT.BORDER);
        productFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        productFileText.setText(defaultProductPath);
        productFileText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));

        Button browseProductButton = new Button(container, SWT.PUSH);
        browseProductButton.setText("Browse...");
        browseProductButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            dialog.setFilterExtensions(new String[]{"*.zip"});
            dialog.setFilterNames(new String[]{"ZIP Files (*.zip)"});
            String selected = dialog.open();
            if (selected != null) {
                productFileText.setText(selected);
                setPageComplete(validateToscaPage());
            }
        });

        createLabel(container, "Front End Zip (.zip):");
        frontendProductFileText = new Text(container, SWT.BORDER);
        frontendProductFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        frontendProductFileText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));

        Button browseFrontendProductButton = new Button(container, SWT.PUSH);
        browseFrontendProductButton.setText("Browse...");
        browseFrontendProductButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            dialog.setFilterExtensions(new String[]{"*.zip"});
            dialog.setFilterNames(new String[]{"ZIP Files (*.zip)"});
            String selected = dialog.open();
            if (selected != null) {
                frontendProductFileText.setText(selected);
                setPageComplete(validateToscaPage());
            }
        });
                
        createLabel(container, "SSH Private Key:");
        privKeyText = new Text(container, SWT.BORDER);
        privKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        privKeyText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));

        Button browsePrivKeyButton = new Button(container, SWT.PUSH);
        browsePrivKeyButton.setText("Browse...");
        browsePrivKeyButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            dialog.setFilterExtensions(new String[]{"*.pem", "*"});
            dialog.setFilterNames(new String[]{"PEM Files (*.pem)", "All Files (*)"});
            String selected = dialog.open();
            if (selected != null) {
                privKeyText.setText(selected);
                setPageComplete(validateToscaPage());
            }
        });

        // === TOSCA Environment Configuration ===
        createSeparator(container);
        createSectionLabel(container, "TOSCA Environment Configuration");

        createLabel(container, "Template Name:");
        templateName = new Text(container, SWT.BORDER);
        templateName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        templateName.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        createLabel(container, "Template Path:");
        toscaTemplatePath = new Text(container, SWT.BORDER);
        toscaTemplatePath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        toscaTemplatePath.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));

        // === Database Configuration ===
        createSeparator(container);
        createSectionLabel(container, "Database Configuration");
        
        createLabel(container, "Database Name:");
        WinVMJProduct product = new ProductToCompose(project, project.getCurrentConfiguration());
        dbNameText = new Text(container, SWT.BORDER);
        dbNameText.setText(product.getProductQualifiedName().replace(".", "_"));
        dbNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        dbNameText.setEnabled(false);

        createLabel(container, "Database Username:");
        dbUsernameText = new Text(container, SWT.BORDER);
        dbUsernameText.setText("postgres");
        dbUsernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        dbUsernameText.setEnabled(false);

        createLabel(container, "Database Password:");
        dbPasswordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
        dbPasswordText.setText("postgres");
        dbPasswordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        dbPasswordText.setEnabled(false);

        // === AWS Configuration ===
        createSeparator(container);
        awsGroup = new Group(container, SWT.NONE);
        awsGroup.setText("AWS Configuration");
        awsGroup.setLayout(new GridLayout(3, false));
        awsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        
        createLabel(awsGroup, "Access Key ID:");
        awsAccessKeyText = new Text(awsGroup, SWT.BORDER);
        awsAccessKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        awsAccessKeyText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        createLabel(awsGroup, "Secret Access Key:");
        awsSecretKeyText = new Text(awsGroup, SWT.BORDER | SWT.PASSWORD);
        awsSecretKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        awsSecretKeyText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        createLabel(awsGroup, "Session Token:");
        awsSessionTokenText = new Text(awsGroup, SWT.BORDER | SWT.PASSWORD);
        awsSessionTokenText.setMessage("Required for AWS Academy");
        awsSessionTokenText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        awsSessionTokenText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        createLabel(awsGroup, "SSH Key Pair Name:");
        awsSshKeyNameText = new Text(awsGroup, SWT.BORDER);
        awsSshKeyNameText.setMessage("e.g., vockey for AWS Academy");
        awsSshKeyNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        awsSshKeyNameText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        // === GCP Configuration ===
        createSeparator(container);
        gcpGroup = new Group(container, SWT.NONE);
        gcpGroup.setText("GCP Configuration");
        gcpGroup.setLayout(new GridLayout(3, false));
        gcpGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        
        createLabel(gcpGroup, "Service Account File:");
        gcpServiceAccountText = new Text(gcpGroup, SWT.BORDER);
        gcpServiceAccountText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        gcpServiceAccountText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        Button browseGcpCredButton = new Button(gcpGroup, SWT.PUSH);
        browseGcpCredButton.setText("Browse...");
        browseGcpCredButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            dialog.setFilterExtensions(new String[]{"*.json"});
            dialog.setFilterNames(new String[]{"JSON Files (*.json)"});
            String selected = dialog.open();
            if (selected != null) {
                gcpServiceAccountText.setText(selected);
                setPageComplete(validateToscaPage());
            }
        });
        
        createLabel(gcpGroup, "Project ID:");
        gcpProjectText = new Text(gcpGroup, SWT.BORDER);
        gcpProjectText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        gcpProjectText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        createLabel(gcpGroup, "Zone:");
        gcpZoneText = new Text(gcpGroup, SWT.BORDER);
        gcpZoneText.setText("us-central1-a");
        gcpZoneText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        gcpZoneText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        createLabel(gcpGroup, "SSH Username:");
        gcpSshUserText = new Text(gcpGroup, SWT.BORDER);
        gcpSshUserText.setText("ubuntu");
        gcpSshUserText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        gcpSshUserText.addListener(SWT.Modify, e -> setPageComplete(validateToscaPage()));
        
        // Initially hide GCP fields
        gcpGroup.setVisible(false);
        ((GridData) gcpGroup.getLayoutData()).exclude = true;
    }
    
    private void createOriginalFields(Composite container) {
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

        if (isV2) {
            productFileText.setText(defaultProductPath);
            productFileText.setEnabled(false);
            browseProductButton.setEnabled(false);
        }
        
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
    }
    
    private void updateProviderFields() {
        String provider = providerCombo.getText();
        regionText.setText(NFRDefinition.getRegion().get());
        if ("AWS".equals(provider)) {
            awsGroup.setVisible(true);
            ((GridData) awsGroup.getLayoutData()).exclude = false;
            
            gcpGroup.setVisible(false);
            ((GridData) gcpGroup.getLayoutData()).exclude = true;
            
        } else if ("GCP".equals(provider)) {
            awsGroup.setVisible(false);
            ((GridData) awsGroup.getLayoutData()).exclude = true;
            
            gcpGroup.setVisible(true);
            ((GridData) gcpGroup.getLayoutData()).exclude = false;
            
        }
        
        awsGroup.getParent().layout(true, true);
        setPageComplete(validateToscaPage());
    }
    
    private boolean validateToscaPage() {
        if (productFileText.getText().trim().isEmpty()) {
            return false;
        }
        
        if (privKeyText.getText().trim().isEmpty()) {
            return false;
        }
        
        String provider = providerCombo.getText();
        
        if ("AWS".equals(provider)) {
            return !awsAccessKeyText.getText().trim().isEmpty() &&
                   !awsSecretKeyText.getText().trim().isEmpty() &&
                   !awsSshKeyNameText.getText().trim().isEmpty();
        } else if ("GCP".equals(provider)) {
            return !gcpServiceAccountText.getText().trim().isEmpty() &&
                   !gcpProjectText.getText().trim().isEmpty() &&
                   !gcpZoneText.getText().trim().isEmpty();
        }
        
        return false;
    }
    
    private void createLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    }
    
    private void createSectionLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        org.eclipse.swt.graphics.FontData[] fontData = label.getFont().getFontData();
        for (org.eclipse.swt.graphics.FontData fd : fontData) {
            fd.setStyle(SWT.BOLD);
        }
        label.setFont(new org.eclipse.swt.graphics.Font(label.getDisplay(), fontData));
    }
    
    private void createSeparator(Composite parent) {
        Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    }

    @Override
    public boolean isPageComplete() {
        if (isToscaMode) {
            return validateToscaPage();
        }
        
        return (!credentialFileText.getText().trim().isEmpty() &&
               credentialFileText.getText().endsWith(".json") || "existing".equals(deploymentTarget)) &&
               !productFileText.getText().trim().isEmpty() &&
               productFileText.getText().endsWith(".zip") &&
               !privKeyText.getText().trim().isEmpty() &&
               (!pubKeyText.getText().trim().isEmpty() || "existing".equals(deploymentTarget));
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (isToscaMode) {
            providerCombo.setItems(NFRDefinition.getProvider().get());
            providerCombo.select(0);

            updateProviderFields();
        }

        if (visible && !isToscaMode) {
            if (!isV2) {
                deploymentTarget = ((LegacyDeploymentWizard) getWizard()).getDeploymentTargetPage().getSelectedDeploymentTarget();
                            
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
            }

            setPageComplete(isPageComplete());
        }
    }

    // Getters for original fields
    public String getCredentialFilePath() {
        return credentialFileText != null ? credentialFileText.getText() : "";
    }

    public String getProductFilePath() {
        return productFileText.getText();
    }

    public String getFrontEndPath() {
        return frontendProductFileText.getText();
    }
    
    public String getPrivKeyFilePath() {
        return privKeyText.getText();
    }
    
    public String getPubKeyFilePath() {
        return pubKeyText != null ? pubKeyText.getText() : "";
    }
    
    // Getters for TOSCA fields
    public String getProvider() {
        return isToscaMode ? providerCombo.getText() : "";
    }
    
    public String getRegion() {
        return isToscaMode ? regionText.getText() : "";
    }
    
    public String getSshKeyPath() {
        return privKeyText.getText();
    }
    
    public String getDbName() {
        return isToscaMode ? dbNameText.getText() : "javaapp";
    }
    
    public String getDbUsername() {
        return isToscaMode ? dbUsernameText.getText() : "postgres";
    }
    
    public String getDbPassword() {
        return isToscaMode ? dbPasswordText.getText() : "password123";
    }
    
    // AWS getters
    public String getAwsAccessKey() {
        return isToscaMode ? awsAccessKeyText.getText() : "";
    }
    
    public String getAwsSecretKey() {
        return isToscaMode ? awsSecretKeyText.getText() : "";
    }
    
    public String getAwsSessionToken() {
        return isToscaMode ? awsSessionTokenText.getText() : "";
    }
    
    public String getAwsSshKeyName() {
        return isToscaMode ? awsSshKeyNameText.getText() : "";
    }
    
    // GCP getters
    public String getGcpServiceAccountFile() {
        return isToscaMode ? gcpServiceAccountText.getText() : "";
    }
    
    public String getGcpProject() {
        return isToscaMode ? gcpProjectText.getText() : "";
    }
    
    public String getGcpZone() {
        return isToscaMode ? gcpZoneText.getText() : "";
    }
    
    public String getGcpSshUser() {
        return isToscaMode ? gcpSshUserText.getText() : "ubuntu";
    }

    public String getTemplateName() {
        return templateName.getText();
    }

    public String getToscaTemplatePath() {
        return toscaTemplatePath.getText();
    }
}