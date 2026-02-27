package id.ac.ui.cs.prices.winvmj.composer.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;

public class PricesDeploymentWizard extends Wizard {
    
    private IFeatureProject project;

    public PricesDeploymentWizard() {
        setWindowTitle("Deployment");
    }

    public void setProject(IFeatureProject project) {
        this.project = project;
    }
    
    public IFeatureProject getProject() {
        return this.project;
    }

    @Override
    public void addPages() {
        addPage(new TestPage());
    }

    @Override
    public boolean performFinish() {
        WinVMJConsole.println("[Deployment] Finished.");
        return true;
    }
    
    private class TestPage extends WizardPage {
        protected TestPage() {
            super("TestPage");
            setTitle("Deployment");
            setDescription("Deployment wizard loaded successfully!");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(1, false));
            
            Label label = new Label(container, SWT.NONE);
            label.setText("Project: " + (project != null ? project.getProjectName() : "null"));
            label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            
            setControl(container);
        }
    }
}
