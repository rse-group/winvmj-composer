package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class AmanahDeploymentPage extends WizardPage {

    private Text tunnelPortText;
    private Text privateKeyText;
    private Text usernameText;
    private Text productNameText;
    private Text productPrefixText;
    private Text productFileText;
    private Text numBackendsText;

    public AmanahDeploymentPage(String pageName) {
        super(pageName);
        setTitle("Amanah SSH Configuration");
        setDescription("Enter SSH tunnel port, private key file, username, product name and product file dir for Amanah deployment.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        // Tunnel port
        new Label(container, SWT.NONE).setText("Local Tunnel Port:");
        tunnelPortText = new Text(container, SWT.BORDER);
        tunnelPortText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tunnelPortText.setToolTipText("Port number used for SSH tunneling");
        new Label(container, SWT.NONE); // Empty cell for alignment

        // Private Key File
        new Label(container, SWT.NONE).setText("Private Key File:");
        privateKeyText = new Text(container, SWT.BORDER);
        privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        privateKeyText.setToolTipText("Private key for amanah server");

        Button browseKeyButton = new Button(container, SWT.PUSH);
        browseKeyButton.setText("Browse...");
        browseKeyButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
            String selected = dialog.open();
            if (selected != null) {
                privateKeyText.setText(selected);
                setPageComplete(isPageComplete());
            }
        });

        // Username
        new Label(container, SWT.NONE).setText("Amanah Username:");
        usernameText = new Text(container, SWT.BORDER);
        usernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        usernameText.setToolTipText("Username for access to amanah server");
        new Label(container, SWT.NONE); // Empty cell for alignment
        
        // Product Name
        new Label(container, SWT.NONE).setText("Product Name:");
        productNameText = new Text(container, SWT.BORDER);
        productNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        productNameText.setToolTipText("Name of the product that will be deployed");
        new Label(container, SWT.NONE); // Empty cell for alignment
        
        // Product Prefix
        new Label(container, SWT.NONE).setText("Product Prefix:");
        productPrefixText = new Text(container, SWT.BORDER);
        productPrefixText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        productPrefixText.setToolTipText("Prefix of the product (ex:aisco or webshop)");
        new Label(container, SWT.NONE); // Empty cell for alignment
        
        // Num Backends
        new Label(container, SWT.NONE).setText("Num Backends:");
        numBackendsText = new Text(container, SWT.BORDER);
        numBackendsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        numBackendsText.setToolTipText("Num of Backend Service (ignore for systemd, for monolith fill 1)");
        new Label(container, SWT.NONE); // Empty cell for alignment
        
        // Product File
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
        
        // Tip Label (Reminder for SSH port forwarding)
        Label tipLabel = new Label(container, SWT.WRAP);
        tipLabel.setText("Tip: Pastikan Anda sudah membuka koneksi SSH port forwarding ke server Kawung sebelum melakukan deployment.\n");
        GridData gdTipLabel = new GridData(SWT.FILL, SWT.TOP, true, false);
        gdTipLabel.horizontalSpan = 3;
        gdTipLabel.widthHint = 500; 
        tipLabel.setLayoutData(gdTipLabel);

        setControl(container);
        setPageComplete(false);
    }

    @Override
    public boolean isPageComplete() {
        return !tunnelPortText.getText().trim().isEmpty() &&
               !privateKeyText.getText().trim().isEmpty() &&
               !productFileText.getText().trim().isEmpty() &&
               !productNameText.getText().trim().isEmpty() &&
               !productPrefixText.getText().trim().isEmpty() &&
               !usernameText.getText().trim().isEmpty();
    }

    public String getTunnelPort() {
        return tunnelPortText.getText();
    }

    public String getPrivateKeyPath() {
        return privateKeyText.getText();
    }

    public String getUsername() {
        return usernameText.getText();
    }
    
    public String getProductName() {
        return productNameText.getText();
    }
    
    public String getProductPrefix() {
        return productPrefixText.getText();
    }
    
    public String getProductFile() {
        return productFileText.getText();
    }
    
    public String getNumBackends() {
    	return numBackendsText.getText();
    }
    
    
}
