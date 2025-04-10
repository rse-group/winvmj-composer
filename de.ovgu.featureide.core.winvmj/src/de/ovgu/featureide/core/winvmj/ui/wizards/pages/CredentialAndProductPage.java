package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class CredentialAndProductPage extends WizardPage {

    private Text credentialFileText;
    private Text productFileText;

    public CredentialAndProductPage(String pageName) {
        super(pageName);
        setTitle("Select Credential and Product Files");
        setDescription("Choose the credential (.json) and product (.zip) files.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        // Credential file
        new Label(container, SWT.NONE).setText("Credential File (.json):");
        credentialFileText = new Text(container, SWT.BORDER);
        credentialFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button browseCredButton = new Button(container, SWT.PUSH);
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

        setControl(container);
        setPageComplete(false);
    }

    @Override
    public boolean isPageComplete() {
        return !credentialFileText.getText().trim().isEmpty() &&
               credentialFileText.getText().endsWith(".json") &&
               !productFileText.getText().trim().isEmpty() &&
               productFileText.getText().endsWith(".zip");
    }

    public String getCredentialFilePath() {
        return credentialFileText.getText();
    }

    public String getProductFilePath() {
        return productFileText.getText();
    }
}
