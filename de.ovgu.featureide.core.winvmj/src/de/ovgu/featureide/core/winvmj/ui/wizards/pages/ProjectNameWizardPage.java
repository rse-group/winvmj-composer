package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizardPage;

public class ProjectNameWizardPage extends WizardPage {
    private Text projectNameText;
    
    public ProjectNameWizardPage() {
        super("Project Name Page");
        setTitle("Project Name");
        setDescription("Enter Your Project Name:");
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        
        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText("Project Name:");
        
        projectNameText = new Text(container, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        projectNameText.addModifyListener(e -> {
            setPageComplete(!projectNameText.getText().trim().isEmpty());
        });

        setPageComplete(!projectNameText.getText().trim().isEmpty());
        
        setControl(container);
    }
    
    public String getProjectName() {
        return projectNameText.getText().trim();
    }
}
