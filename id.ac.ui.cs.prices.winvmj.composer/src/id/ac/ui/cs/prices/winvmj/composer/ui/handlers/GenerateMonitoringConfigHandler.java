package id.ac.ui.cs.prices.winvmj.composer.ui.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.MonitoringConfigWizard;

/**
 * Handler for "Generate Monitoring Configuration" context menu action.
 * Triggered by right-clicking an XML file in the configs/ folder.
 */
public class GenerateMonitoringConfigHandler extends AFeatureProjectHandler {

    @Override
    protected void singleAction(IFeatureProject project) {
        // Get the selected file from the current selection
        IFile configFile = getSelectedConfigFile();
        if (configFile == null) {
            WinVMJConsole.println("[Monitoring] No config XML file selected.");
            return;
        }

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MonitoringConfigWizard wizard = new MonitoringConfigWizard(project, configFile);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.setHelpAvailable(false);
        dialog.open();
    }

    private IFile getSelectedConfigFile() {
        ISelection selection = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getSelectionService()
                .getSelection();

        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof IFile) {
                return (IFile) element;
            }
            // Handle adapter case
            if (element instanceof org.eclipse.core.runtime.IAdaptable) {
                IResource resource = ((org.eclipse.core.runtime.IAdaptable) element)
                        .getAdapter(IResource.class);
                if (resource instanceof IFile) {
                    return (IFile) resource;
                }
            }
        }
        return null;
    }
}
