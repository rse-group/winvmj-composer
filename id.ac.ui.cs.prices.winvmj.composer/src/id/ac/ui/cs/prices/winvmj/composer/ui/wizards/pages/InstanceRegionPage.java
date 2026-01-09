package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.CloudConfiguration;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.handlers.DeploymentToscaHandler;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.KNNResult;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRKnn3;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.*;

public class InstanceRegionPage extends WizardPage {
    private Combo instanceCombo;
    private Combo regionCombo;

    private String selectedInstance;
    private String selectedRegion;

    private IFeatureProject project;

    public InstanceRegionPage(String pageName) {
        super(pageName);
        setTitle("Non-Functional Requirement Definition");
        setDescription("These are the machine type recommendations based on your NFR definition");
    }

    public InstanceRegionPage(IFeatureProject project, String pageName) {
        this(pageName);
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        Label instanceLabel = new Label(container, SWT.NONE);
        instanceLabel.setText("Instance Type:");

        instanceCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        instanceCombo.setLayoutData(
            new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        instanceCombo.addListener(SWT.Selection, e -> {
            NFRDefinition.setInstanceType(
                new Instance(instanceCombo.getText())
            );

            validatePage();
        });

        Label regionLabel = new Label(container, SWT.NONE);
        regionLabel.setText("Region:");

        regionCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        regionCombo.setLayoutData(
            new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        regionCombo.addListener(SWT.Selection, e -> {
            NFRDefinition.setRegion(
                new Region(regionCombo.getText())
            );
            validatePage();
        });

        setControl(container);
        setPageComplete(false);
    }

    private void validatePage() {
        if (instanceCombo.getText().isEmpty() || regionCombo.getText().isEmpty()) {
            setErrorMessage("Please select both instance type and region.");
            setPageComplete(false);
            return;
        }

        setErrorMessage(null);
        setPageComplete(true);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        instanceCombo.setItems(NFRDefinition.getInstances());
        regionCombo.setItems(NFRDefinition.getRegions());
    }
}

