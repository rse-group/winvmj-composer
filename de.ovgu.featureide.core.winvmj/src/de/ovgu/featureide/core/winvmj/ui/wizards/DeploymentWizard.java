package de.ovgu.featureide.core.winvmj.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.ui.wizards.pages.*;

public class DeploymentWizard extends Wizard {
    private SelectDeploymentPage deploymentPage;
    private CredentialAndProductPage filePage;
    private DeploymentConfigPage configPage;

    public DeploymentWizard() {
        setWindowTitle("Deployment Wizard");
    }

    @Override
    public void addPages() {
    	deploymentPage = new SelectDeploymentPage("Deployment Selection");
        filePage = new CredentialAndProductPage("File Selection");
        configPage = new DeploymentConfigPage("Configuration");

        addPage(deploymentPage);
        addPage(filePage);
        addPage(configPage);
    }

    @Override
    public boolean performFinish() {
        String selectedOption = deploymentPage.getSelectedOption();
        String credentialPath = filePage.getCredentialFilePath();
        String productZipPath = filePage.getProductFilePath();
        String username = configPage.getUsername();
        String machineType = configPage.getMachineType();
        String region = configPage.getRegion();
        String certificateName = configPage.getCertificateName();
        String nginxCertName = configPage.getNginxCertificateName();
        String instanceName = configPage.getInstanceName();
        String productPrefix = configPage.getProductPrefix();

        WinVMJConsole.println("=== Deployment Wizard Configuration ===");
        WinVMJConsole.println("Selected Deployment Option: " + selectedOption);
        WinVMJConsole.println("Credential File Path: " + credentialPath);
        WinVMJConsole.println("Product ZIP File Path: " + productZipPath);
        WinVMJConsole.println("Username: " + username);
        WinVMJConsole.println("Machine Type: " + machineType);
        WinVMJConsole.println("Region: " + region);
        WinVMJConsole.println("Certificate Name: " + certificateName);
        WinVMJConsole.println("NGINX Certificate Name: " + nginxCertName);
        WinVMJConsole.println("Instance Name: " + instanceName);
        WinVMJConsole.println("Product Prefix: " + productPrefix);

        return true;
    }
    
    @Override
    public boolean canFinish() {
        return deploymentPage.isPageComplete()
            && filePage.isPageComplete()
            && configPage.isPageComplete();
    }
    
    public SelectDeploymentPage getDeploymentPage() {
        return deploymentPage;
    }


}
