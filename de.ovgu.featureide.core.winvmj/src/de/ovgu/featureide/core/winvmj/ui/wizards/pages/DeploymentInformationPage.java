package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.ui.handlers.DeploymentV2Handler;

public class DeploymentInformationPage extends WizardPage {
    private IFeatureProject project;

    private Text providerText;
    private Text instanceText;
    private Text regionText;

    public DeploymentInformationPage(IFeatureProject project) {
        super("Summary");
        setTitle("Specification Summary");
        setDescription("Review the deployment specification.");
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Provider Type:");
        providerText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        providerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        providerText.setEnabled(false);

        new Label(container, SWT.NONE).setText("Instance Type:");
        instanceText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        instanceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        instanceText.setEnabled(false);

        new Label(container, SWT.NONE).setText("Region:");
        regionText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        regionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        regionText.setEnabled(false);

        setControl(container);
        setPageComplete(true);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            providerText.setText(DeploymentV2Handler.getProvider().get());
            instanceText.setText(DeploymentV2Handler.getInstance().get());
            regionText.setText(DeploymentV2Handler.getRegion().get());
        }
    }
}
