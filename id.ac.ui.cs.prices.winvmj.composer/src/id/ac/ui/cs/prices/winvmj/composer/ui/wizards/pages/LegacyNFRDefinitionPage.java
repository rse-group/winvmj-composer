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
import id.ac.ui.cs.prices.winvmj.composer.ui.handlers.LegacyDeploymentToscaHandler;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.LegacyDeploymentWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.KNNResult;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRKnn3;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.*;

public class LegacyNFRDefinitionPage extends WizardPage {
    private static String EXISTING_INSTANCE_TYPE_FLAG = " (Previously Chosen Instance)";
    private Combo knnCombo;
    private List<KNNResult> results;
    private IFeatureProject project;
    private Text tpsText;
    private Text transactionText;
    private KNNResult selected;

    public LegacyNFRDefinitionPage(String pageName) {
        super(pageName);
        setTitle("Non-Functional Requirement Definition");
        setDescription("These are the machine type recommendations based on your NFR definition");
    }

    public LegacyNFRDefinitionPage(IFeatureProject project, String pageName) {
        this(pageName);
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        Label tpsLabel = new Label(container, SWT.NONE);
        tpsLabel.setText("Target TPS:");
        tpsText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        tpsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        tpsText.setText(NFRDefinition.getTPS().get());
        tpsText.setEnabled(false);

        Label transactionLabel = new Label(container, SWT.NONE);
        transactionLabel.setText("Target Transactions:");
        transactionText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        transactionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        transactionText.setText(NFRDefinition.getTransaction().get());
        transactionText.setEnabled(false);

        Label apdexLabel = new Label(container, SWT.NONE);
        apdexLabel.setText("Minimum Apdex:");

        Text apdexText = new Text(container, SWT.BORDER);
        apdexText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button checkButton = new Button(container, SWT.PUSH);
        checkButton.setText("Check");

        checkButton.addListener(SWT.Selection, e -> {
            String input = apdexText.getText().trim();

            if (isValidApdex(input)) {
                updateKNN(Float.parseFloat(input));
                
                setErrorMessage(null);
                setPageComplete(true);
            } 
            
            else {
                setErrorMessage("Apdex must be a float between 0 and 1.");
                setPageComplete(false);
            }
        });

        Label label = new Label(container, SWT.NONE);
        label.setText("Select a machine recommendation:");
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

        knnCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        knnCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        knnCombo.addListener(SWT.Selection, e -> {
            int index = knnCombo.getSelectionIndex();
            if (index >= 0) {
                selected = results.get(index);
                updateSelectedInformation();
                WinVMJConsole.println("Selected: " + selected);
            }
        });

        setControl(container);
        setPageComplete(false);
    }

    private List<KNNResult> executeKNN(float minApdex) {
        try {
            int targetTPS = Integer.parseInt(NFRDefinition.getTPS().get());
            int targetTransactions = Integer.parseInt(NFRDefinition.getTransaction().get());
            return NFRKnn3.getInstanceKnn3(targetTPS, targetTransactions, minApdex);
        }

        catch (Exception e) {
            WinVMJConsole.println("An error occurred: " + e.getMessage());
            return List.of();
        }
    }

    private boolean isValidApdex(String input) {
        try {
            float value = Float.parseFloat(input);
            return value >= 0.0f && value <= 1.0f;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    private void updateKNN(float minApdex) {
        if (knnCombo != null) {
            knnCombo.removeAll();   
        }

        results = executeKNN(minApdex);
        results.forEach(result -> knnCombo.add(result.toString()));
        
        if (NFRDefinition.getInstance() != null) {
            knnCombo.add(NFRDefinition.getInstance().get() + EXISTING_INSTANCE_TYPE_FLAG);
            results.add(new KNNResult(
                new CloudConfiguration(
                    Integer.parseInt(NFRDefinition.getTPS().get()),
                    Integer.parseInt(NFRDefinition.getTransaction().get()),
                    NFRDefinition.getInstance().get(),
                    NFRDefinition.getRegion().get(),
                    0.0,
                    NFRDefinition.getProvider().get()
                ),
                0
            ));
        }
    }

    private void updateSelectedInformation() {
        NFRDefinition.setProviderType(new Provider(selected.getConfig().getProvider()));
        NFRDefinition.setInstanceType(new Instance(selected.getConfig().getInstance()));
        NFRDefinition.setRegion(new Region(selected.getConfig().getRegion()));
        NFRDefinition.setTransaction(new Transaction(String.valueOf(selected.getConfig().getTransactions())));
        NFRDefinition.setTPS(new TPS(String.valueOf(selected.getConfig().getTPS())));
    }
}
