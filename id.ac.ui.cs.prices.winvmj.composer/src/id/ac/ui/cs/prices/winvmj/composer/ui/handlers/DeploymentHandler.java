package id.ac.ui.cs.prices.winvmj.composer.ui.handlers;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;

public class DeploymentHandler extends AFeatureProjectHandler {
    
    @Override
    protected void singleAction(IFeatureProject project) {
        WinVMJConsole.showConsole();
        WinVMJConsole.println("[Deploy] Opening deployment wizard...");
        
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        DeploymentWizard wizard = new DeploymentWizard(project);
        WizardDialog dialog = new WizardDialog(shell, wizard) {
            @Override
            protected void createButtonsForButtonBar(Composite parent) {
                super.createButtonsForButtonBar(parent);

                Button finishButton = getButton(IDialogConstants.FINISH_ID);
                if (finishButton != null) {
                    finishButton.setVisible(false);
                    Object layoutData = finishButton.getLayoutData();
                    if (layoutData instanceof GridData) {
                        ((GridData) layoutData).exclude = true;
                    }
                }

                Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
                if (cancelButton != null) {
                    cancelButton.setText("Close");
                }

                parent.layout(true, true);
            }
        };
        dialog.setHelpAvailable(false);
        dialog.open();
    }
}
