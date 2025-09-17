package de.ovgu.featureide.core.winvmj.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.ui.wizards.FeatureWizard;

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
