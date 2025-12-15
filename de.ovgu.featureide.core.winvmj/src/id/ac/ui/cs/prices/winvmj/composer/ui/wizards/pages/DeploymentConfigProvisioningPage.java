package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.NFRDefinition;

public class DeploymentConfigProvisioningPage extends WizardPage {

    private Text usernameText;
    private Text machineTypeText;
    private Text regionText;
    private Text certificateNameText;
    private Text nginxCertNameText;
    private Text instanceNameText;
    private Text productPrefixText;
    private Text productNameText;
    private Text numBackendsText;


    public DeploymentConfigProvisioningPage(String pageName) {
        super(pageName);
        setTitle("Deployment Configuration");
        setDescription("Fill in the deployment configuration fields.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 10;
        layout.marginHeight = 10;
        layout.verticalSpacing = 8;
        container.setLayout(layout);

        usernameText = createField(container, "Username:", "Nama user untuk linux user di VM (Untuk AWS hanya bisa 'ubuntu')");
        machineTypeText = createField(container, "Machine Type:", "Tipe Mesin Berdasarkan Feature Model");
        regionText = createField(container, "Region:", "Lokasi region untuk instance");

        certificateNameText = createField(container, "Certificate Name:", "Nama sertifikat SSL utama yang digunakan");
        nginxCertNameText = createField(container, "NGINX Certificate Name:", "Nama sertifikat yang digunakan oleh NGINX");
        instanceNameText = createField(container, "Instance Name:", "Nama unik untuk VM instance");
        productPrefixText = createField(container, "Product Prefix:", "Prefix folder produk (contoh: 'aisco' atau 'webshop')");
        productNameText = createField(container, "Product Name:", "Nama produk spesifik yang akan di-deploy");
        numBackendsText = createField(container, "Number of Backends:", "Jumlah service backend yang ingin dideploy (isi 1 untuk monolith)");

       
        Listener validationListener = e -> setPageComplete(isPageComplete());

        usernameText.addListener(SWT.Modify, validationListener);
        machineTypeText.addListener(SWT.Selection, validationListener);
        regionText.addListener(SWT.Selection, validationListener);
        certificateNameText.addListener(SWT.Modify, validationListener);
        nginxCertNameText.addListener(SWT.Modify, validationListener);
        instanceNameText.addListener(SWT.Modify, validationListener);
        productPrefixText.addListener(SWT.Modify, validationListener);
        productNameText.addListener(SWT.Modify, validationListener);
        numBackendsText.addListener(SWT.Modify, validationListener);

        setControl(container);
        setPageComplete(false);
    }


    private Text createField(Composite parent, String labelText, String tooltipText) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText);
        label.setToolTipText(tooltipText);

        Text text = new Text(parent, SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        text.setToolTipText(tooltipText);
        return text;
    }

    private Combo createComboField(Composite parent, String labelText, String[] items, String tooltipText) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText);
        label.setToolTipText(tooltipText);

        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        combo.setItems(items);
        combo.setToolTipText(tooltipText);
        return combo;
    }

    @Override
    public boolean isPageComplete() {

        return !usernameText.getText().trim().isEmpty()
                && !machineTypeText.getText().trim().isEmpty()
                && !regionText.getText().trim().isEmpty()
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
            String deploymentTarget = ((DeploymentWizard) getWizard()).getDeploymentPage().getSelectedProvider();
            String deploymentMethod = ((DeploymentWizard) getWizard()).getDeploymentPage().getSelectedDeploymentMethod();
            
            machineTypeText.setText(NFRDefinition.getInstance().get());
            machineTypeText.setEnabled(false);

            if ("AWS".equalsIgnoreCase(deploymentTarget)) {
                // regionCombo.setItems(new String[]{"US", "EUROPE", "SINGAPORE"});
                // regionCombo.setVisible(true);
                regionText.setText(NFRDefinition.getRegion().get());
                regionText.setEnabled(false);

                usernameText.setText("ubuntu");
                usernameText.setEnabled(false);
                usernameText.setToolTipText("Untuk AWS, user harus 'ubuntu'");
            } else if ("GCP".equalsIgnoreCase(deploymentTarget)) {
                // regionCombo.setItems(new String[]{"US", "EUROPE", "JAKARTA"});
                // regionCombo.setVisible(true);
                regionText.setText(NFRDefinition.getRegion().get());
                regionText.setEnabled(false);

                usernameText.setEnabled(true);
                usernameText.setText("");
                usernameText.setToolTipText("Nama user untuk akses SSH (bebas diisi)");
            } else if ("On-Prem".equalsIgnoreCase(deploymentTarget)) {
                regionText.setText("r");
                regionText.setVisible(false);

                usernameText.setEnabled(true);
                usernameText.setText("");
                usernameText.setToolTipText("Nama user lokal untuk akses SSH");
            }

            // if (regionCombo.isVisible() && regionCombo.getItemCount() > 0) {
            //     regionCombo.select(0);
            // }
            
            
            if ("docker".equalsIgnoreCase(deploymentMethod)) {
            	numBackendsText.setEnabled(true);
            } else if ("systemd".equalsIgnoreCase(deploymentMethod)) {
            	numBackendsText.setEnabled(false);
            }

            setPageComplete(isPageComplete());
        }
    }

    // Getters
    public String getUsername() {
        return usernameText.getText();
    }

    public String getMachineType() {
        return machineTypeText.getText();
    }

    public String getRegion() {
        return regionText.getText();
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
    
    public String getNumBackends() {
        return numBackendsText.getText();
    }
    
    
}
