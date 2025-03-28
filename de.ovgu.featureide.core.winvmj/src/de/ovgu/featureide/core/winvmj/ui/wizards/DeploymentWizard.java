package de.ovgu.featureide.core.winvmj.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.ui.wizards.pages.SelectDeploymentPage;

public class DeploymentWizard extends Wizard {
    private SelectDeploymentPage selectionPage;

    public DeploymentWizard() {
        setWindowTitle("Deployment Wizard");
    }

    @Override
    public void addPages() {
        selectionPage = new SelectDeploymentPage("Deployment Selection");
        addPage(selectionPage);
    }

    @Override
    public boolean performFinish() {
        String selectedOption = selectionPage.getSelectedOption();
        WinVMJConsole.println("Selected Deployment Option: " + selectedOption);
        return true;
    }
}
