package id.ac.ui.cs.prices.winvmj.composer.ui.handlers;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.NFRDefinition;

public class DeploymentMenuWizardHandler extends AFeatureProjectHandler {
	@Override
    protected void singleAction(IFeatureProject project) {
		WinVMJConsole.showConsole();
        WinVMJConsole.println("\n=== Transforming nfr_to_model.json ===");
        NFRDefinition.processNFRSelectedFeatureIntoNFRModel(project);

        if (!NFRDefinition.validateProviderAndInstanceNotNull()) {
            WinVMJConsole.println("[ERROR] Please define the Provider and Instance!");
            return;
        }
        
        Shell shell = getShell();
        DeploymentWizard deploymentWizard = new DeploymentWizard();
        deploymentWizard.setProject(project);
        WizardDialog wizardDialog = new WizardDialog(shell, deploymentWizard);
        wizardDialog.open();
    }

    private Shell getShell() {
        return org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

}