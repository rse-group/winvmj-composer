package de.ovgu.featureide.core.winvmj.ui.handlers;

import org.eclipse.jface.wizard.WizardDialog;

import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import org.eclipse.swt.widgets.Shell;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import de.ovgu.featureide.core.winvmj.ui.wizards.DeploymentWizard;

public class DeploymentMenuWizardHandler extends AFeatureProjectHandler {
	@Override
    protected void singleAction(IFeatureProject project) {
		WinVMJConsole.showConsole();
        Shell shell = getShell();
        DeploymentWizard deploymentWizard = new DeploymentWizard();
        WizardDialog wizardDialog = new WizardDialog(shell, deploymentWizard);
        wizardDialog.open();
    }

    private Shell getShell() {
        return org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

}