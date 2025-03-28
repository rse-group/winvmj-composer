package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizardPage;

public class ConfirmationSelectionWizardPage extends WizardPage {
    
    public ConfirmationSelectionWizardPage() {
        super("Confirmation Page");
        setTitle("Important!");
        setDescription("Confirm your Selected UVLs");
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        
        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText("Please double check your selected UVLs since you cannot change the selected UVLs again");

        setPageComplete(true);
        
        setControl(container);
    }
}
