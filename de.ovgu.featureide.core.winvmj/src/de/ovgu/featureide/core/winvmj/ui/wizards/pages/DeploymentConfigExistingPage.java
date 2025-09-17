package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import de.ovgu.featureide.core.winvmj.ui.wizards.DeploymentWizard;

public class DeploymentConfigExistingPage extends WizardPage {
	
	private Text usernameText;
    private Text instanceIPText;
    private Text certificateNameText;
    private Text nginxCertNameText;
    private Text productPrefixText;
    private Text productNameText;
    private Text numBackendsText;


    public DeploymentConfigExistingPage(String pageName) {
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
        
        usernameText = createField(container, "Username:", "Linux User");
        instanceIPText = createField(container, "Instance IP:", "IP address server)");

        certificateNameText = createField(container, "Certificate Name:", "Nama sertifikat SSL utama yang digunakan");
        nginxCertNameText = createField(container, "NGINX Certificate Name:", "Nama sertifikat yang digunakan oleh NGINX");
        productPrefixText = createField(container, "Product Prefix:", "Prefix folder produk (contoh: 'aisco' atau 'webshop')");
        productNameText = createField(container, "Product Name:", "Nama produk spesifik yang akan di-deploy");
        numBackendsText = createField(container, "Number of Backends:", "Jumlah service backend yang ingin dideploy (isi 1 untuk monolith)");

       
        Listener validationListener = e -> setPageComplete(isPageComplete());

        usernameText.addListener(SWT.Modify, validationListener);
        instanceIPText.addListener(SWT.Modify, validationListener);
        certificateNameText.addListener(SWT.Modify, validationListener);
        nginxCertNameText.addListener(SWT.Modify, validationListener);
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


    @Override
    public boolean isPageComplete() {

        return !instanceIPText.getText().trim().isEmpty()
        		&& !usernameText.getText().trim().isEmpty()
                && !certificateNameText.getText().trim().isEmpty()
                && !nginxCertNameText.getText().trim().isEmpty()
                && !productPrefixText.getText().trim().isEmpty()
                && !productNameText.getText().trim().isEmpty();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            String deploymentMethod = ((DeploymentWizard) getWizard()).getDeploymentPage().getSelectedDeploymentMethod();
                          
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
    
    public String getInstanceIP() {
        return instanceIPText.getText();
    }

    public String getCertificateName() {
        return certificateNameText.getText();
    }

    public String getNginxCertificateName() {
        return nginxCertNameText.getText();
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
