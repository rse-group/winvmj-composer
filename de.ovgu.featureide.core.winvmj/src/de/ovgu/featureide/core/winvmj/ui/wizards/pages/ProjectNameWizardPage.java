package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import java.util.Map;

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
        super("Product Name Page");
        setTitle("Product Name");
        setDescription("Enter Your Product Name:");
    }
    
    public String getProductName() {
        return projectNameText.getText();
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        
        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText("Product Name:");
        
        projectNameText = new Text(container, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        projectNameText.addVerifyListener(e -> {
            String input = e.text;
            if (!input.matches("[a-zA-Z]*")) {
                e.doit = false;
            }
        });
        
        Label infoLabel = new Label(container, SWT.NONE);
        infoLabel.setText("Only alphabetic characters (A–Z, a–z) are allowed.");
        GridData infoGridData = new GridData();
        infoGridData.horizontalSpan = 2;
        infoLabel.setLayoutData(infoGridData);
        
        projectNameText.addModifyListener(e -> {
            String text = projectNameText.getText().trim();
            boolean isValid = text.matches("[a-zA-Z]+");
            setPageComplete(isValid);
        });

        setPageComplete(!projectNameText.getText().trim().isEmpty());
        
        setControl(container);
    }
    
    public String getProjectName() {
        return projectNameText.getText().trim();
    }
}
