package id.ac.ui.cs.prices.winvmj.composer.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.FeatureWizard;
import de.ovgu.featureide.core.IFeatureProject;

public class OpenFeatureWizardHandler extends AFeatureProjectHandler {
    @Override
    protected void singleAction(IFeatureProject project) {
        Shell shell = getShell();
        FeatureWizard featureWizard = new FeatureWizard();
        featureWizard.setProject(project);
        WizardDialog wizardDialog = new WizardDialog(shell, featureWizard);
        wizardDialog.open();
    }

    private Shell getShell() {
        return org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }
}
