package de.ovgu.featureide.core.winvmj.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class NFRDefinitionPage extends WizardPage {
    private static String[] AVAILABE_REGIONS = new String[] { "US-East", "US-West", "Europe", "Asia" };

    private Text processedTransactionPerSecondText;
    private Text transactionsInASecondText;
    private Combo machineRegionCombo;

    public NFRDefinitionPage(String pageName) {
        super(pageName);
        setTitle("Non-Functional Requirement Definition");
        setDescription("Fill in the NFR Definition Fields");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        Label transactionsInASecondLabel = new Label(container, SWT.NONE);
        transactionsInASecondLabel.setText("Number of Transactions in a Second:");
        transactionsInASecondText = new Text(container, SWT.BORDER);
        transactionsInASecondText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label processedTransactionLabel = new Label(container, SWT.NONE);
        processedTransactionLabel.setText("Processed Transactions per Second:");
        processedTransactionPerSecondText = new Text(container, SWT.BORDER);
        processedTransactionPerSecondText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label machineRegionLabel = new Label(container, SWT.NONE);
        machineRegionLabel.setText("Machine Region:");
        machineRegionCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        machineRegionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        machineRegionCombo.setItems(AVAILABE_REGIONS);
        
        ModifyListener modifyListener = e -> validatePage();

        transactionsInASecondText.addModifyListener(modifyListener);
        processedTransactionPerSecondText.addModifyListener(modifyListener);
        machineRegionCombo.addModifyListener(modifyListener);

        setControl(container);
        setPageComplete(false);
    }

    private void validatePage() {
        String transactionsInASecondStr = transactionsInASecondText.getText().trim();
        String processedTransactionPerSecondStr = processedTransactionPerSecondText.getText().trim();
        String region = machineRegionCombo.getText();

        if (!transactionsInASecondStr.matches("\\d+") || !isPositiveDecimal(Integer.parseInt(transactionsInASecondStr))) {
            setErrorMessage("Number of Transactions in a Second must be a valid positive number.");
            setPageComplete(false);
            return;
        }

        if (!processedTransactionPerSecondStr.matches("\\d+") || !isPositiveDecimal(Integer.parseInt(processedTransactionPerSecondStr))) {
            setErrorMessage("Processed Transactions per Second must be a valid positive number.");
            setPageComplete(false);
            return;
        }

        if (region == null || region.isEmpty()) {
            setErrorMessage("Please select a Machine Region.");
            setPageComplete(false);
            return;
        }

        setErrorMessage(null);
        setPageComplete(true);
    }

    private boolean isPositiveDecimal(int input) {
        return input > 0;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            return;
        }
    }


}
