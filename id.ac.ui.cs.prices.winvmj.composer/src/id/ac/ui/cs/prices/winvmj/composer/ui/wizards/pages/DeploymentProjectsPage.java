package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.program.Program;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner.CliResult;
import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;

public class DeploymentProjectsPage extends WizardPage {
    
    private DeploymentWizard wizard;
    private PricesDeploymentCliRunner cliRunner;
    
    private Table projectsTable;
    private Label statusLabel;
    private Button refreshButton;
    
    private List<ProjectInfo> projects = new ArrayList<>();
    private String selectedSlug = null;
    
    public DeploymentProjectsPage(DeploymentWizard wizard) {
        super("Projects");
        setTitle("Select Project");
        setDescription("Select an existing project or create a new one for deployment.");
        this.wizard = wizard;
        this.cliRunner = wizard.getCliRunner();
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        
        // Status label
        statusLabel = new Label(container, SWT.NONE);
        statusLabel.setText("Loading projects...");
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        // Projects table
        projectsTable = new Table(container, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        projectsTable.setHeaderVisible(true);
        projectsTable.setLinesVisible(true);
        GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableGd.heightHint = 200;
        projectsTable.setLayoutData(tableGd);
        
        TableColumn nameCol = new TableColumn(projectsTable, SWT.NONE);
        nameCol.setText("Name");
        nameCol.setWidth(120);
        
        TableColumn slugCol = new TableColumn(projectsTable, SWT.NONE);
        slugCol.setText("Slug");
        slugCol.setWidth(100);
        
        TableColumn descCol = new TableColumn(projectsTable, SWT.NONE);
        descCol.setText("Description");
        descCol.setWidth(120);
        
        TableColumn statusCol = new TableColumn(projectsTable, SWT.NONE);
        statusCol.setText("Status");
        statusCol.setWidth(80);
        
        TableColumn typeCol = new TableColumn(projectsTable, SWT.NONE);
        typeCol.setText("Type");
        typeCol.setWidth(70);
        
        TableColumn plCol = new TableColumn(projectsTable, SWT.NONE);
        plCol.setText("Product Line");
        plCol.setWidth(120);
        
        TableColumn frontendCol = new TableColumn(projectsTable, SWT.NONE);
        frontendCol.setText("Frontend URLs");
        frontendCol.setWidth(180);
        
        TableColumn backendCol = new TableColumn(projectsTable, SWT.NONE);
        backendCol.setText("Backend URLs");
        backendCol.setWidth(180);
        
        // Make columns responsive
        projectsTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                int totalWidth = projectsTable.getClientArea().width;
                if (totalWidth > 0) {
                    nameCol.setWidth((int)(totalWidth * 0.10));
                    slugCol.setWidth((int)(totalWidth * 0.11));
                    descCol.setWidth((int)(totalWidth * 0.11));
                    statusCol.setWidth((int)(totalWidth * 0.07));
                    typeCol.setWidth((int)(totalWidth * 0.07));
                    plCol.setWidth((int)(totalWidth * 0.12));
                    frontendCol.setWidth((int)(totalWidth * 0.21));
                    backendCol.setWidth((int)(totalWidth * 0.21));
                }
            }
        });
        
        projectsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = projectsTable.getSelectionIndex();
                if (index >= 0 && index < projects.size()) {
                    selectedSlug = projects.get(index).slug;
                    wizard.setSelectedProjectSlug(selectedSlug);
                    updatePageComplete();
                }
            }
        });
        
        // Double-click to open project detail dialog
        projectsTable.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                int index = projectsTable.getSelectionIndex();
                if (index >= 0 && index < projects.size()) {
                    openProjectDetailDialog(projects.get(index));
                }
            }
        });
        
        // Button bar
        Composite buttonBar = new Composite(container, SWT.NONE);
        buttonBar.setLayout(new GridLayout(2, false));
        buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        refreshButton = new Button(buttonBar, SWT.PUSH);
        refreshButton.setText("Refresh");
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                loadProjects();
            }
        });
        
        Button createButton = new Button(buttonBar, SWT.PUSH);
        createButton.setText("Create New Project");
        createButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openCreateProjectDialog();
            }
        });
        
        setControl(container);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            loadProjects();
        }
    }
    
    private void setLoadingState(boolean loading) {
        Display.getDefault().asyncExec(() -> {
            if (projectsTable.isDisposed()) return;
            projectsTable.setEnabled(!loading);
            refreshButton.setEnabled(!loading);
            Cursor cursor = loading ? Display.getDefault().getSystemCursor(SWT.CURSOR_WAIT) : null;
            getShell().setCursor(cursor);
        });
    }
    
    private void loadProjects() {
        statusLabel.setText("Loading projects...");
        projectsTable.removeAll();
        projects.clear();
        selectedSlug = null;
        updatePageComplete();
        setLoadingState(true);
        
        new Thread(() -> {
            WinVMJConsole.println("[PROJECTS] Fetching projects list...");
            CliResult result = cliRunner.listProjectsJson();
            
            Display.getDefault().asyncExec(() -> {
                setLoadingState(false);
                if (result.isSuccess()) {
                    parseProjects(result);
                    statusLabel.setText("Select a project to deploy. Double-click for details.");
                    WinVMJConsole.println("[PROJECTS] Found " + projects.size() + " projects.");
                } else {
                    statusLabel.setText("Failed to load projects: " + result.getMessage());
                    WinVMJConsole.println("[PROJECTS] Error: " + result.getMessage());
                }
            });
        }).start();
    }
    
    private void openProjectDetailDialog(ProjectInfo project) {
        ProjectDetailDialog dialog = new ProjectDetailDialog(getShell(), project, cliRunner, () -> loadProjects());
        dialog.open();
    }
    
    /**
     * Opens the project detail dialog for the currently selected project.
     * Can be called from other pages.
     */
    public void openSelectedProjectDetail() {
        String slug = wizard.getSelectedProjectSlug();
        if (slug == null) return;
        
        for (ProjectInfo project : projects) {
            if (slug.equals(project.slug)) {
                openProjectDetailDialog(project);
                return;
            }
        }
    }
    
    private void parseProjects(CliResult result) {
        String arrayJson = result.getDataFieldAsString("_array");
        if (arrayJson == null || arrayJson.isEmpty()) {
            return;
        }
        
        try {
            JsonArray array = parseJsonArraySafe(arrayJson);
            if (array == null) {
                WinVMJConsole.println("[PROJECTS] Empty or invalid projects payload");
                return;
            }
            
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                ProjectInfo info = new ProjectInfo();
                info.name = obj.has("name") ? obj.get("name").getAsString() : "Unknown";
                info.slug = obj.has("slug") ? obj.get("slug").getAsString() : "";
                info.description = obj.has("description") && !obj.get("description").isJsonNull() 
                    ? obj.get("description").getAsString() : "";
                info.status = obj.has("status") ? obj.get("status").getAsString() : "unknown";
                info.projectType = obj.has("projectType") && !obj.get("projectType").isJsonNull() 
                    ? obj.get("projectType").getAsString() : "regular";
                info.productLine = obj.has("productLine") && !obj.get("productLine").isJsonNull()
                    ? obj.get("productLine").getAsString() : "";
                
                // Build frontend URLs - custom first, then default
                List<String> frontendUrls = new ArrayList<>();
                if (obj.has("customFrontendActive") && obj.get("customFrontendActive").getAsBoolean()) {
                    if (obj.has("customFrontendUrl") && !obj.get("customFrontendUrl").isJsonNull()) {
                        frontendUrls.add(obj.get("customFrontendUrl").getAsString());
                    }
                }
                if (obj.has("defaultFrontendActive") && obj.get("defaultFrontendActive").getAsBoolean()) {
                    if (obj.has("defaultFrontendUrl") && !obj.get("defaultFrontendUrl").isJsonNull()) {
                        frontendUrls.add(obj.get("defaultFrontendUrl").getAsString());
                    }
                }
                info.frontendUrls = String.join(", ", frontendUrls);
                
                // Build backend URLs - custom first, then default
                List<String> backendUrls = new ArrayList<>();
                if (obj.has("customBackendActive") && obj.get("customBackendActive").getAsBoolean()) {
                    if (obj.has("customBackendUrl") && !obj.get("customBackendUrl").isJsonNull()) {
                        backendUrls.add(obj.get("customBackendUrl").getAsString());
                    }
                }
                if (obj.has("defaultBackendActive") && obj.get("defaultBackendActive").getAsBoolean()) {
                    if (obj.has("defaultBackendUrl") && !obj.get("defaultBackendUrl").isJsonNull()) {
                        backendUrls.add(obj.get("defaultBackendUrl").getAsString());
                    }
                }
                info.backendUrls = String.join(", ", backendUrls);
                
                // Store custom URLs for editing
                if (obj.has("customFrontendUrl") && !obj.get("customFrontendUrl").isJsonNull()) {
                    info.customFrontendUrl = obj.get("customFrontendUrl").getAsString();
                }
                if (obj.has("customBackendUrl") && !obj.get("customBackendUrl").isJsonNull()) {
                    info.customBackendUrl = obj.get("customBackendUrl").getAsString();
                }
                
                // Parse listening ports
                if (obj.has("frontendListeningPort") && !obj.get("frontendListeningPort").isJsonNull()) {
                    info.frontendListeningPort = obj.get("frontendListeningPort").getAsInt();
                }
                if (obj.has("backendListeningPort") && !obj.get("backendListeningPort").isJsonNull()) {
                    info.backendListeningPort = obj.get("backendListeningPort").getAsInt();
                }
                
                projects.add(info);
                
                TableItem item = new TableItem(projectsTable, SWT.NONE);
                item.setText(new String[] { info.name, info.slug, info.description, info.status, info.projectType, info.productLine, info.frontendUrls, info.backendUrls });
            }
        } catch (Exception e) {
            WinVMJConsole.println("[PROJECTS] Failed to parse projects: " + e.getMessage());
        }
    }

    /**
     * Lenient parser for JSON arrays coming back from the CLI.
     *
     * The CLI output is already sanitized upstream by
     * {@link PricesDeploymentCliRunner.CliResult#parse} (BOM / ANSI / JVM banners
     * are stripped there), but we use a lenient {@link JsonReader} here as a
     * second line of defense so a single malformed byte on an unusual device
     * never breaks the whole UI with "malformed JSON".
     */
    private static JsonArray parseJsonArraySafe(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JsonReader reader = new JsonReader(new java.io.StringReader(json));
            reader.setLenient(true);
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonArray()) {
                return null;
            }
            return element.getAsJsonArray();
        } catch (Exception e) {
            WinVMJConsole.println("[PROJECTS] parseJsonArraySafe failed: " + e.getMessage());
            return null;
        }
    }
    
    private void openCreateProjectDialog() {
        CreateProjectDialog dialog = new CreateProjectDialog(getShell(), wizard.getFeatureProject().getProjectName());
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            createProject(dialog.getProjectName(), dialog.getDescription(), dialog.getProductLine(),
                         dialog.getCustomFrontendUrl(), dialog.getCustomBackendUrl(),
                         dialog.getFrontendListeningPort(), dialog.getBackendListeningPort());
        }
    }
    
    private void createProject(String name, String description, String productLine,
                               String customFrontendUrl, String customBackendUrl,
                               Integer frontendListeningPort, Integer backendListeningPort) {
        statusLabel.setText("Creating project '" + name + "'...");
        
        new Thread(() -> {
            WinVMJConsole.println("[PROJECTS] Creating project: " + name);
            CliResult result = cliRunner.createProjectJson(name, description, productLine, customFrontendUrl, customBackendUrl,
                                                            frontendListeningPort, backendListeningPort);
            
            Display.getDefault().asyncExec(() -> {
                if (result.isSuccess()) {
                    WinVMJConsole.println("[PROJECTS] Project created successfully: " + result.getMessage());
                    loadProjects();
                } else {
                    statusLabel.setText("Failed to create project: " + result.getMessage());
                    WinVMJConsole.println("[PROJECTS] Project creation failed: " + result.getMessage());
                }
            });
        }).start();
    }
    
    /**
     * Custom dialog for creating a project with optional custom URLs.
     */
    private static class CreateProjectDialog extends Dialog {
        private Text nameText;
        private Text descriptionText;
        private Text productLineText;
        private Text customFrontendText;
        private Text customBackendText;
        private Text frontendPortText;
        private Text backendPortText;
        
        private String projectName;
        private String description;
        private String productLine;
        private String customFrontendUrl;
        private String customBackendUrl;
        private Integer frontendListeningPort;
        private Integer backendListeningPort;
        private String defaultName;
        
        public CreateProjectDialog(Shell parentShell, String defaultName) {
            super(parentShell);
            this.defaultName = defaultName;
        }
        
        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Create New Project");
        }
        
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(3, false));
            
            // Project name
            Label nameLabel = new Label(container, SWT.NONE);
            nameLabel.setText("Project Name:*");
            nameText = new Text(container, SWT.BORDER);
            nameText.setLayoutData(spanTwoColumns());
            if (defaultName != null) {
                nameText.setText(defaultName);
            }
            
            // Description
            Label descLabel = new Label(container, SWT.NONE);
            descLabel.setText("Description:");
            descriptionText = new Text(container, SWT.BORDER);
            descriptionText.setLayoutData(spanTwoColumns());
            descriptionText.setText("Deployed from WinVMJ Composer");
            
            // Product Line
            Label plLabel = new Label(container, SWT.NONE);
            plLabel.setText("Product Line:*");
            productLineText = new Text(container, SWT.BORDER);
            productLineText.setLayoutData(spanTwoColumns());
            productLineText.setMessage("e.g., BankAccount");
            
            // Separator
            Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridData sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            sepGd.horizontalSpan = 3;
            separator.setLayoutData(sepGd);
            
            // Custom URLs section label
            Label urlLabel = new Label(container, SWT.NONE);
            urlLabel.setText("Custom URL slugs (optional):");
            GridData urlLabelGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            urlLabelGd.horizontalSpan = 3;
            urlLabel.setLayoutData(urlLabelGd);
            
            // Custom frontend URL
            customFrontendText = createCustomDomainField(container, "Frontend slug/domain:", "e.g., myapp");
            
            // Custom backend URL
            customBackendText = createCustomDomainField(container, "Backend slug/domain:", "e.g., api-myapp");
            
            // Separator for ports
            Label portSep = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridData portSepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            portSepGd.horizontalSpan = 3;
            portSep.setLayoutData(portSepGd);
            
            // Internal Listening Ports section label
            Label portLabel = new Label(container, SWT.NONE);
            portLabel.setText("Internal Listening Ports (optional):");
            GridData portLabelGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            portLabelGd.horizontalSpan = 3;
            portLabel.setLayoutData(portLabelGd);
            
            // Frontend listening port
            Label fePortLabel = new Label(container, SWT.NONE);
            fePortLabel.setText("Frontend Port:");
            frontendPortText = new Text(container, SWT.BORDER);
            frontendPortText.setLayoutData(spanTwoColumns());
            frontendPortText.setMessage("default: 80");
            
            // Backend listening port
            Label bePortLabel = new Label(container, SWT.NONE);
            bePortLabel.setText("Backend Port:");
            backendPortText = new Text(container, SWT.BORDER);
            backendPortText.setLayoutData(spanTwoColumns());
            backendPortText.setMessage("default: 7776");
            
            return container;
        }
        
        @Override
        protected Point getInitialSize() {
            return new Point(560, 450);
        }

        private Text createCustomDomainField(Composite container, String labelText, String message) {
            new Label(container, SWT.NONE).setText(labelText);

            Text text = new Text(container, SWT.BORDER);
            text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            text.setMessage(message);

            Label suffixLabel = new Label(container, SWT.NONE);
            suffixLabel.setText(Utils.DEPLOYMENT_PARENT_DOMAIN_SUFFIX);
            suffixLabel.setToolTipText("Leave the field empty to skip custom URL, or enter a full domain to use another host.");

            return text;
        }

        private GridData spanTwoColumns() {
            GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            gd.horizontalSpan = 2;
            return gd;
        }
        
        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, "Create", true);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }
        
        @Override
        protected void okPressed() {
            projectName = nameText.getText().trim();
            description = descriptionText.getText().trim();
            productLine = productLineText.getText().trim();
            customFrontendUrl = customFrontendText.getText().trim();
            customBackendUrl = customBackendText.getText().trim();
            
            // Parse ports
            String fePortStr = frontendPortText.getText().trim();
            String bePortStr = backendPortText.getText().trim();
            frontendListeningPort = fePortStr.isEmpty() ? null : parsePort(fePortStr);
            backendListeningPort = bePortStr.isEmpty() ? null : parsePort(bePortStr);
            
            if (projectName.isEmpty()) {
                nameText.setFocus();
                return;
            }
            if (productLine.isEmpty()) {
                productLineText.setFocus();
                return;
            }
            super.okPressed();
        }
        
        private Integer parsePort(String portStr) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        public String getProjectName() { return projectName; }
        public String getDescription() { return description; }
        public String getProductLine() { return productLine; }
        public String getCustomFrontendUrl() { return customFrontendUrl; }
        public String getCustomBackendUrl() { return customBackendUrl; }
        public Integer getFrontendListeningPort() { return frontendListeningPort; }
        public Integer getBackendListeningPort() { return backendListeningPort; }
    }
    
    private void updatePageComplete() {
        setPageComplete(selectedSlug != null && !selectedSlug.isEmpty());
    }
    
    @Override
    public boolean isPageComplete() {
        return selectedSlug != null && !selectedSlug.isEmpty();
    }
    
    public String getSelectedProjectSlug() {
        return selectedSlug;
    }
    
    private static class ProjectInfo {
        String name;
        String slug;
        String description;
        String status;
        String projectType;
        String productLine;
        String frontendUrls;
        String backendUrls;
        String customFrontendUrl;
        String customBackendUrl;
        Integer frontendListeningPort;
        Integer backendListeningPort;
    }
    
    /**
     * Dialog for viewing project details with tabs.
     */
    private static class ProjectDetailDialog extends Dialog {
        private final ProjectInfo project;
        private final PricesDeploymentCliRunner cliRunner;
        private final Runnable onChangeCallback;
        
        public ProjectDetailDialog(Shell parentShell, ProjectInfo project, PricesDeploymentCliRunner cliRunner, Runnable onChangeCallback) {
            super(parentShell);
            setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
            this.project = project;
            this.cliRunner = cliRunner;
            this.onChangeCallback = onChangeCallback;
        }
        
        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Project: " + project.name);
            shell.setMaximized(true);
        }
        
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(1, false));
            
            TabFolder tabFolder = new TabFolder(container, SWT.NONE);
            tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            
            // Detail tab
            TabItem detailTab = new TabItem(tabFolder, SWT.NONE);
            detailTab.setText("Details");
            detailTab.setControl(createDetailTab(tabFolder));
            
            // Deployments tab
            TabItem deploymentsTab = new TabItem(tabFolder, SWT.NONE);
            deploymentsTab.setText("Deployments");
            deploymentsTab.setControl(createDeploymentsTab(tabFolder));
            
            // Env Vars tab
            TabItem envVarsTab = new TabItem(tabFolder, SWT.NONE);
            envVarsTab.setText("Env Variables");
            envVarsTab.setControl(createEnvVarsTab(tabFolder));
            
            // Logs tab
            TabItem logsTab = new TabItem(tabFolder, SWT.NONE);
            logsTab.setText("Logs");
            logsTab.setControl(createLogsTab(tabFolder));
            
            // Monitoring tab
            TabItem monitoringTab = new TabItem(tabFolder, SWT.NONE);
            monitoringTab.setText("Monitoring");
            monitoringTab.setControl(createMonitoringTab(tabFolder));
            
            return container;
        }
        
        private Composite createDetailTab(TabFolder parent) {
            Composite comp = new Composite(parent, SWT.NONE);
            comp.setLayout(new GridLayout(1, false));
            
            // Info area (loaded async)
            Composite infoComp = new Composite(comp, SWT.NONE);
            infoComp.setLayout(new GridLayout(2, false));
            infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            
            // Loading runnable for refresh
            Runnable loadDetails = () -> {
                // Clear existing children
                for (Control c : infoComp.getChildren()) {
                    c.dispose();
                }
                
                Label loadingLabel = new Label(infoComp, SWT.NONE);
                loadingLabel.setText("Loading project details...");
                GridData loadGd = new GridData(SWT.FILL, SWT.TOP, true, false);
                loadGd.horizontalSpan = 2;
                loadingLabel.setLayoutData(loadGd);
                infoComp.layout(true, true);
                
                new Thread(() -> {
                    CliResult result = cliRunner.getProjectJson(project.slug);
                    Display.getDefault().asyncExec(() -> {
                        if (infoComp.isDisposed()) return;
                        loadingLabel.dispose();
                        
                        if (result.isSuccess()) {
                            Map<String, Object> data = result.getData();
                            
                            // Basic info
                            createLabelPair(infoComp, "Name:", getStr(data, "name"));
                            createLabelPair(infoComp, "Slug:", getStr(data, "slug"));
                            createLabelPair(infoComp, "Description:", getStr(data, "description"));
                            createLabelPair(infoComp, "Status:", getStr(data, "status"));
                            createLabelPair(infoComp, "Product Line:", getStr(data, "productLine"));
                            
                            // Separator
                            Label sep1 = new Label(infoComp, SWT.SEPARATOR | SWT.HORIZONTAL);
                            GridData sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
                            sepGd.horizontalSpan = 2;
                            sep1.setLayoutData(sepGd);
                            
                            // Frontend URLs section
                            Label feHeader = new Label(infoComp, SWT.NONE);
                            feHeader.setText("Frontend URLs:");
                            GridData feHGd = new GridData(SWT.FILL, SWT.TOP, true, false);
                            feHGd.horizontalSpan = 2;
                            feHeader.setLayoutData(feHGd);
                            
                            String defFe = getStr(data, "defaultFrontendUrl");
                            boolean defFeActive = "true".equals(getStr(data, "defaultFrontendActive"));
                            if (defFe != null && !defFe.isEmpty() && !"-".equals(defFe)) {
                                createUrlRow(infoComp, "  Default:", defFe, defFeActive);
                            }
                            
                            String custFe = getStr(data, "customFrontendUrl");
                            boolean custFeActive = "true".equals(getStr(data, "customFrontendActive"));
                            if (custFe != null && !custFe.isEmpty() && !"-".equals(custFe)) {
                                createUrlRow(infoComp, "  Custom:", custFe, custFeActive);
                            }
                            
                            // Backend URLs section
                            Label beHeader = new Label(infoComp, SWT.NONE);
                            beHeader.setText("Backend URLs:");
                            GridData beHGd = new GridData(SWT.FILL, SWT.TOP, true, false);
                            beHGd.horizontalSpan = 2;
                            beHeader.setLayoutData(beHGd);
                            
                            String defBe = getStr(data, "defaultBackendUrl");
                            boolean defBeActive = "true".equals(getStr(data, "defaultBackendActive"));
                            if (defBe != null && !defBe.isEmpty() && !"-".equals(defBe)) {
                                createUrlRow(infoComp, "  Default:", defBe, defBeActive);
                            }
                            
                            String custBe = getStr(data, "customBackendUrl");
                            boolean custBeActive = "true".equals(getStr(data, "customBackendActive"));
                            if (custBe != null && !custBe.isEmpty() && !"-".equals(custBe)) {
                                createUrlRow(infoComp, "  Custom:", custBe, custBeActive);
                            }
                            
                            // Internal Listening Ports section
                            Label sep2 = new Label(infoComp, SWT.SEPARATOR | SWT.HORIZONTAL);
                            GridData sep2Gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
                            sep2Gd.horizontalSpan = 2;
                            sep2.setLayoutData(sep2Gd);
                            
                            Label portHeader = new Label(infoComp, SWT.NONE);
                            portHeader.setText("Internal Listening Ports:");
                            GridData portHGd = new GridData(SWT.FILL, SWT.TOP, true, false);
                            portHGd.horizontalSpan = 2;
                            portHeader.setLayoutData(portHGd);
                            
                            String fePort = getStr(data, "frontendListeningPort");
                            String bePort = getStr(data, "backendListeningPort");
                            createLabelPair(infoComp, "  Frontend:", (fePort == null || "-".equals(fePort)) ? "80 (default)" : fePort);
                            createLabelPair(infoComp, "  Backend:", (bePort == null || "-".equals(bePort)) ? "7776 (default)" : bePort);
                            
                            infoComp.layout(true, true);
                            infoComp.getParent().layout(true, true);
                        } else {
                            Label errLabel = new Label(infoComp, SWT.NONE);
                            errLabel.setText("Failed to load: " + result.getMessage());
                        }
                    });
                }).start();
            };
            
            // Initial load
            loadDetails.run();
            
            // Button bar at bottom
            Composite buttonBar = new Composite(comp, SWT.NONE);
            buttonBar.setLayout(new GridLayout(3, false));
            buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
            
            Button refreshButton = new Button(buttonBar, SWT.PUSH);
            refreshButton.setText("Refresh");
            refreshButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    loadDetails.run();
                }
            });
            
            Button editButton = new Button(buttonBar, SWT.PUSH);
            editButton.setText("Edit Project");
            editButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    openEditProjectDialog(loadDetails);
                }
            });
            
            Button deleteButton = new Button(buttonBar, SWT.PUSH);
            deleteButton.setText("Delete Project");
            deleteButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    openDeleteProjectConfirmation();
                }
            });
            
            return comp;
        }
        
        private String getStr(Map<String, Object> data, String key) {
            Object val = data.get(key);
            if (val == null) return "-";
            String s = val.toString();
            return s.isEmpty() ? "-" : s;
        }
        
        private void createUrlRow(Composite parent, String label, String url, boolean active) {
            Label l = new Label(parent, SWT.NONE);
            l.setText(label);
            l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            
            // Create composite for URL + copy button
            Composite urlComp = new Composite(parent, SWT.NONE);
            GridLayout urlLayout = new GridLayout(2, false);
            urlLayout.marginWidth = 0;
            urlLayout.marginHeight = 0;
            urlComp.setLayout(urlLayout);
            urlComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            
            Label v = new Label(urlComp, SWT.NONE);
            String status = active ? " [ACTIVE]" : " [INACTIVE]";
            v.setText(url + status);
            GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            gd.widthHint = 300;
            v.setLayoutData(gd);
            
            Button copyBtn = new Button(urlComp, SWT.PUSH);
            copyBtn.setText("Copy");
            copyBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Clipboard clipboard = new Clipboard(parent.getDisplay());
                    clipboard.setContents(new Object[] { url }, new Transfer[] { TextTransfer.getInstance() });
                    clipboard.dispose();
                }
            });
        }
        
        private void createLabelPair(Composite parent, String label, String value) {
            Label l = new Label(parent, SWT.NONE);
            l.setText(label);
            l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
            
            Label v = new Label(parent, SWT.WRAP);
            v.setText(value);
            GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
            gd.widthHint = 350;
            v.setLayoutData(gd);
        }
        
        private void openEditProjectDialog(Runnable refreshDetails) {
            Shell shell = getShell();
            EditProjectDialog dialog = new EditProjectDialog(shell, project, cliRunner);
            if (dialog.open() == org.eclipse.jface.window.Window.OK) {
                // Refresh details tab and main page
                refreshDetails.run();
                if (onChangeCallback != null) {
                    onChangeCallback.run();
                }
            }
        }
        
        private void openDeleteProjectConfirmation() {
            Shell shell = getShell();
            MessageBox confirmBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
            confirmBox.setText("Delete Project");
            confirmBox.setMessage("Are you sure you want to delete project '" + project.name + "'?\nThis action cannot be undone.");
            
            if (confirmBox.open() == SWT.YES) {
                // Delete project
                new Thread(() -> {
                    int exitCode = cliRunner.deleteProject(project.slug);
                    Display.getDefault().asyncExec(() -> {
                        if (shell.isDisposed()) return;
                        
                        if (exitCode == 0) {
                            // Refresh main page and close dialog
                            if (onChangeCallback != null) {
                                onChangeCallback.run();
                            }
                            shell.close();
                        } else {
                            MessageBox errorBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                            errorBox.setText("Delete Failed");
                            errorBox.setMessage("Failed to delete project. Exit code: " + exitCode);
                            errorBox.open();
                        }
                    });
                }).start();
            }
        }
        
        private Composite createDeploymentsTab(TabFolder parent) {
            Composite comp = new Composite(parent, SWT.NONE);
            comp.setLayout(new GridLayout(1, false));
            
            Label infoLabel = new Label(comp, SWT.NONE);
            infoLabel.setText("Loading deployment history...");
            infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            
            Table deploymentsTable = new Table(comp, SWT.BORDER | SWT.FULL_SELECTION);
            deploymentsTable.setHeaderVisible(true);
            deploymentsTable.setLinesVisible(true);
            GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true);
            tableGd.heightHint = 150;
            deploymentsTable.setLayoutData(tableGd);
            
            TableColumn idCol = new TableColumn(deploymentsTable, SWT.NONE);
            idCol.setText("ID");
            idCol.setWidth(60);
            
            TableColumn statusCol = new TableColumn(deploymentsTable, SWT.NONE);
            statusCol.setText("Status");
            statusCol.setWidth(100);
            
            TableColumn durationCol = new TableColumn(deploymentsTable, SWT.NONE);
            durationCol.setText("Duration");
            durationCol.setWidth(80);
            
            TableColumn dateCol = new TableColumn(deploymentsTable, SWT.NONE);
            dateCol.setText("Created At");
            dateCol.setWidth(180);
            
            // Load deployments runnable
            Runnable loadDeployments = () -> {
                infoLabel.setText("Loading deployment history...");
                new Thread(() -> {
                    CliResult result = cliRunner.historyJson(project.slug);
                    Display.getDefault().asyncExec(() -> {
                        if (deploymentsTable.isDisposed()) return;
                        deploymentsTable.removeAll();
                        
                        if (result.isSuccess()) {
                            String arrayJson = result.getDataFieldAsString("_array");
                            if (arrayJson != null && !arrayJson.isEmpty()) {
                                try {
                                    JsonArray array = parseJsonArraySafe(arrayJson);
                                    if (array == null || array.size() == 0) {
                                        infoLabel.setText("No deployments found for " + project.slug);
                                    } else {
                                        infoLabel.setText("Deployment history for " + project.slug + " (" + array.size() + ")");
                                        for (JsonElement elem : array) {
                                            JsonObject obj = elem.getAsJsonObject();
                                            TableItem item = new TableItem(deploymentsTable, SWT.NONE);
                                            item.setText(new String[] {
                                                obj.has("id") ? String.valueOf(obj.get("id").getAsInt()) : "-",
                                                obj.has("status") ? obj.get("status").getAsString() : "-",
                                                obj.has("duration") && !obj.get("duration").isJsonNull() ? obj.get("duration").getAsString() : "-",
                                                obj.has("createdAt") && !obj.get("createdAt").isJsonNull() ? obj.get("createdAt").getAsString() : "-"
                                            });
                                        }
                                    }
                                } catch (Exception e) {
                                    infoLabel.setText("Failed to parse deployments");
                                }
                            } else {
                                infoLabel.setText("No deployments found for " + project.slug);
                            }
                        } else {
                            infoLabel.setText("Failed to load: " + result.getMessage());
                        }
                    });
                }).start();
            };
            
            // Initial load
            loadDeployments.run();
            
            // Button bar with Refresh
            Composite buttonBar = new Composite(comp, SWT.NONE);
            buttonBar.setLayout(new GridLayout(1, false));
            buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
            
            Button refreshButton = new Button(buttonBar, SWT.PUSH);
            refreshButton.setText("Refresh");
            refreshButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    loadDeployments.run();
                }
            });
            
            return comp;
        }
        
        private Composite createEnvVarsTab(TabFolder parent) {
            Composite comp = new Composite(parent, SWT.NONE);
            comp.setLayout(new GridLayout(1, false));
            
            Label infoLabel = new Label(comp, SWT.NONE);
            infoLabel.setText("Loading environment variables...");
            infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            
            Label helpLabel = new Label(comp, SWT.WRAP);
            helpLabel.setText("Format: KEY=VALUE (one per line). Click Save to replace all env vars.");
            GridData helpGd = new GridData(SWT.FILL, SWT.TOP, true, false);
            helpGd.widthHint = 400;
            helpLabel.setLayoutData(helpGd);
            
            // Raw text editor for env vars
            Text envEditor = new Text(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            GridData editorGd = new GridData(SWT.FILL, SWT.FILL, true, true);
            editorGd.heightHint = 180;
            envEditor.setLayoutData(editorGd);
            
            // Status label for save feedback
            Label statusLabel = new Label(comp, SWT.NONE);
            statusLabel.setText("");
            statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            
            // Button bar
            Composite buttonBar = new Composite(comp, SWT.NONE);
            buttonBar.setLayout(new GridLayout(2, false));
            buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
            
            Button refreshButton = new Button(buttonBar, SWT.PUSH);
            refreshButton.setText("Refresh");
            
            Button saveButton = new Button(buttonBar, SWT.PUSH);
            saveButton.setText("Save (Replace All)");
            
            // Load env vars async
            Runnable loadEnvVars = () -> {
                infoLabel.setText("Loading...");
                envEditor.setEnabled(false);
                saveButton.setEnabled(false);
                
                new Thread(() -> {
                    CliResult result = cliRunner.envVarsJson(project.slug);
                    Display.getDefault().asyncExec(() -> {
                        if (envEditor.isDisposed()) return;
                        
                        if (result.isSuccess()) {
                            Map<String, Object> data = result.getData();
                            StringBuilder sb = new StringBuilder();
                            int count = 0;
                            if (data != null) {
                                for (Map.Entry<String, Object> entry : data.entrySet()) {
                                    if (entry.getKey().startsWith("_")) continue;
                                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                                    count++;
                                }
                            }
                            envEditor.setText(sb.toString());
                            infoLabel.setText("Environment variables for " + project.slug + " (" + count + ")");
                        } else {
                            infoLabel.setText("Failed to load: " + result.getMessage());
                            envEditor.setText("");
                        }
                        envEditor.setEnabled(true);
                        saveButton.setEnabled(true);
                    });
                }).start();
            };
            
            // Initial load
            loadEnvVars.run();
            
            // Refresh button
            refreshButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    statusLabel.setText("");
                    loadEnvVars.run();
                }
            });
            
            // Save button - replace all env vars
            saveButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String content = envEditor.getText().trim();
                    String[] lines = content.isEmpty() ? new String[0] : content.split("\\n");
                    
                    // Parse and validate
                    List<String> envPairs = new ArrayList<>();
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        if (!line.contains("=")) {
                            statusLabel.setText("Invalid format: " + line);
                            return;
                        }
                        envPairs.add(line);
                    }
                    
                    statusLabel.setText("Saving...");
                    saveButton.setEnabled(false);
                    
                    new Thread(() -> {
                        CliResult result = cliRunner.replaceEnvVarsJson(project.slug, envPairs.toArray(new String[0]));
                        Display.getDefault().asyncExec(() -> {
                            if (statusLabel.isDisposed()) return;
                            if (result.isSuccess()) {
                                statusLabel.setText("Saved successfully!");
                                loadEnvVars.run();
                            } else {
                                statusLabel.setText("Failed: " + result.getMessage());
                                saveButton.setEnabled(true);
                            }
                        });
                    }).start();
                }
            });
            
            return comp;
        }
        
        private Composite createLogsTab(TabFolder parent) {
            Composite comp = new Composite(parent, SWT.NONE);
            comp.setLayout(new GridLayout(1, false));
            
            // Info label
            Label infoLabel = new Label(comp, SWT.NONE);
            infoLabel.setText("Project Logs");
            infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            
            // Log viewer (read-only text area)
            Text logViewer = new Text(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
            logViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            logViewer.setFont(new org.eclipse.swt.graphics.Font(Display.getDefault(), "Consolas", 9, SWT.NORMAL));
            
            // Button bar
            Composite buttonBar = new Composite(comp, SWT.NONE);
            buttonBar.setLayout(new GridLayout(3, false));
            buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
            
            Button refreshButton = new Button(buttonBar, SWT.PUSH);
            refreshButton.setText("Refresh");
            
            Button followButton = new Button(buttonBar, SWT.TOGGLE);
            followButton.setText("Follow");
            
            Button clearButton = new Button(buttonBar, SWT.PUSH);
            clearButton.setText("Clear");
            
            // Streaming state
            final Process[] streamProcess = {null};
            
            // Load logs async
            Runnable loadLogs = () -> {
                infoLabel.setText("Loading logs...");
                logViewer.setText("");
                
                new Thread(() -> {
                    String logs = cliRunner.getProjectLogs(project.slug, 200);
                    Display.getDefault().asyncExec(() -> {
                        if (logViewer.isDisposed()) return;
                        
                        if (logs != null && !logs.trim().isEmpty()) {
                            logViewer.setText(logs);
                            infoLabel.setText("Logs for " + project.slug + " (last 200 lines)");
                            // Scroll to bottom
                            logViewer.setSelection(logViewer.getText().length());
                        } else {
                            logViewer.setText("No logs available.");
                            infoLabel.setText("Logs for " + project.slug);
                        }
                    });
                }).start();
            };
            
            // Stop streaming
            Runnable stopStreaming = () -> {
                if (streamProcess[0] != null) {
                    streamProcess[0].destroyForcibly();
                    streamProcess[0] = null;
                }
            };
            
            // Initial load
            loadLogs.run();
            
            // Refresh button
            refreshButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    stopStreaming.run();
                    followButton.setSelection(false);
                    loadLogs.run();
                }
            });
            
            // Follow toggle button
            followButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (followButton.getSelection()) {
                        // Start streaming
                        infoLabel.setText("Streaming logs... (click Follow again to stop)");
                        refreshButton.setEnabled(false);
                        
                        streamProcess[0] = cliRunner.streamProjectLogs(project.slug, line -> {
                            Display.getDefault().asyncExec(() -> {
                                if (logViewer.isDisposed()) {
                                    stopStreaming.run();
                                    return;
                                }
                                logViewer.append(line + "\n");
                                logViewer.setSelection(logViewer.getText().length());
                            });
                        });
                    } else {
                        // Stop streaming
                        stopStreaming.run();
                        infoLabel.setText("Streaming stopped");
                        refreshButton.setEnabled(true);
                    }
                }
            });
            
            // Clear button
            clearButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    logViewer.setText("");
                }
            });
            
            // Stop streaming when dialog closes
            comp.addDisposeListener(e -> stopStreaming.run());
            
            return comp;
        }
        
        private Composite createMonitoringTab(TabFolder parent) {
            Composite comp = new Composite(parent, SWT.NONE);
            comp.setLayout(new GridLayout(1, false));
            
            // Info/status label
            Label infoLabel = new Label(comp, SWT.WRAP);
            infoLabel.setText("Loading monitoring dashboard...");
            GridData infoGd = new GridData(SWT.FILL, SWT.TOP, true, false);
            infoGd.widthHint = 500;
            infoLabel.setLayoutData(infoGd);
            
            // Expiry label
            Label expiryLabel = new Label(comp, SWT.NONE);
            expiryLabel.setText("");
            expiryLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            
            // Button bar: Regenerate, Open in Browser, Copy URL
            Composite buttonBar = new Composite(comp, SWT.NONE);
            buttonBar.setLayout(new GridLayout(3, false));
            buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            
            Button generateButton = new Button(buttonBar, SWT.PUSH);
            generateButton.setText("Regenerate Monitoring URL");
            generateButton.setEnabled(false);
            
            Button openBrowserButton = new Button(buttonBar, SWT.PUSH);
            openBrowserButton.setText("Open in Browser");
            openBrowserButton.setEnabled(false);
            
            Button copyUrlButton = new Button(buttonBar, SWT.PUSH);
            copyUrlButton.setText("Copy URL");
            copyUrlButton.setEnabled(false);
            
            // Embedded browser widget (SWT.EDGE forces Chromium engine on Windows)
            Browser browser = new Browser(comp, SWT.EDGE);
            GridData browserGd = new GridData(SWT.FILL, SWT.FILL, true, true);
            browserGd.heightHint = 300;
            browser.setLayoutData(browserGd);
            
            // Show loading placeholder in embedded browser
            browser.setText("<html><body style='font-family:sans-serif;color:#666;display:flex;align-items:center;"
                + "justify-content:center;height:100vh;margin:0;background:#f5f5f5;'>"
                + "<div style='text-align:center;'>"
                + "<p style='font-size:16px;'>Loading monitoring dashboard...</p>"
                + "</div></body></html>");
            
            // Track the generated URL
            final String[] monitoringUrl = {null};
            
            // Reusable generate/regenerate logic
            Runnable doGenerate = () -> {
                Display.getDefault().asyncExec(() -> {
                    if (comp.isDisposed()) return;
                    infoLabel.setText("Generating monitoring URL...");
                    generateButton.setEnabled(false);
                    openBrowserButton.setEnabled(false);
                    copyUrlButton.setEnabled(false);
                });
                
                WinVMJConsole.println("[MONITORING] Generating monitoring URL for " + project.slug + "...");
                CliResult result = cliRunner.monitoringJson(project.slug);
                
                Display.getDefault().asyncExec(() -> {
                    if (comp.isDisposed()) return;
                    generateButton.setEnabled(true);
                    
                    if (result.isSuccess()) {
                        Map<String, Object> data = result.getData();
                        String url = data.get("url") != null ? data.get("url").toString() : null;
                        String expiredAt = data.get("expiredAt") != null ? data.get("expiredAt").toString() : null;
                        
                        if (url != null) {
                            monitoringUrl[0] = url;
                            
                            infoLabel.setText("Monitoring dashboard loaded. URL generated successfully.");
                            if (expiredAt != null) {
                                expiryLabel.setText("Expires: " + expiredAt);
                            }
                            
                            // Enable action buttons
                            openBrowserButton.setEnabled(true);
                            copyUrlButton.setEnabled(true);
                            
                            // Load in embedded browser
                            browser.setUrl(url);
                            
                            WinVMJConsole.println("[MONITORING] URL: " + url);
                            WinVMJConsole.println("[MONITORING] Expires: " + expiredAt);
                        } else {
                            infoLabel.setText("Failed: No URL returned from server.");
                        }
                    } else {
                        infoLabel.setText("Failed to generate monitoring URL: " + result.getMessage());
                        WinVMJConsole.println("[MONITORING] Error: " + result.getMessage());
                    }
                });
            };
            
            // Check deployment status, then auto-generate if has deployments
            new Thread(() -> {
                CliResult historyResult = cliRunner.historyJson(project.slug);
                
                boolean hasDeployments = false;
                if (historyResult.isSuccess()) {
                    String arrayJson = historyResult.getDataFieldAsString("_array");
                    if (arrayJson != null && !arrayJson.isEmpty()) {
                        try {
                            JsonArray array = parseJsonArraySafe(arrayJson);
                            hasDeployments = array != null && array.size() > 0;
                        } catch (Exception e) {
                            // parse error, assume no deployments
                        }
                    }
                }
                
                if (hasDeployments) {
                    // Auto-generate monitoring URL
                    doGenerate.run();
                } else {
                    Display.getDefault().asyncExec(() -> {
                        if (comp.isDisposed()) return;
                        infoLabel.setText("This project has not been deployed yet. "
                            + "Deploy your project first before generating a monitoring dashboard.");
                        generateButton.setEnabled(false);
                        browser.setText("<html><body style='font-family:sans-serif;color:#999;display:flex;align-items:center;"
                            + "justify-content:center;height:100vh;margin:0;background:#f5f5f5;'>"
                            + "<div style='text-align:center;'>"
                            + "<p style='font-size:18px;font-weight:bold;'>No Deployments Found</p>"
                            + "<p style='font-size:14px;'>This project has not been deployed yet.<br/>"
                            + "Deploy your project first to access the monitoring dashboard.</p>"
                            + "</div></body></html>");
                    });
                }
            }).start();
            
            // Regenerate button handler
            generateButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    new Thread(doGenerate).start();
                }
            });
            
            // Open in Browser button handler
            openBrowserButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (monitoringUrl[0] != null) {
                        Program.launch(monitoringUrl[0]);
                        WinVMJConsole.println("[MONITORING] Opened in external browser: " + monitoringUrl[0]);
                    }
                }
            });
            
            // Copy URL button handler
            copyUrlButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (monitoringUrl[0] != null) {
                        Clipboard clipboard = new Clipboard(comp.getDisplay());
                        clipboard.setContents(new Object[] { monitoringUrl[0] }, new Transfer[] { TextTransfer.getInstance() });
                        clipboard.dispose();
                        WinVMJConsole.println("[MONITORING] URL copied to clipboard");
                    }
                }
            });
            
            return comp;
        }
        
        @Override
        protected Point getInitialSize() {
            // Fallback size if not maximized
            return new Point(900, 700);
        }
        
        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, "Close", true);
        }
    }
    
    /**
     * Dialog for editing project settings.
     */
    private static class EditProjectDialog extends Dialog {
        private final ProjectInfo project;
        private final PricesDeploymentCliRunner cliRunner;
        
        private Text nameText;
        private Text descText;
        private Text frontendUrlText;
        private Text backendUrlText;
        private Text frontendPortText;
        private Text backendPortText;
        private Label statusLabel;
        
        public EditProjectDialog(Shell parentShell, ProjectInfo project, PricesDeploymentCliRunner cliRunner) {
            super(parentShell);
            this.project = project;
            this.cliRunner = cliRunner;
        }
        
        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Edit Project: " + project.name);
        }
        
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(3, false));
            
            // Name
            new Label(container, SWT.NONE).setText("Name:");
            nameText = new Text(container, SWT.BORDER);
            nameText.setLayoutData(spanTwoColumns());
            nameText.setText(project.name != null ? project.name : "");
            
            // Description
            new Label(container, SWT.NONE).setText("Description:");
            descText = new Text(container, SWT.BORDER);
            descText.setLayoutData(spanTwoColumns());
            descText.setText(project.description != null ? project.description : "");
            
            // Separator
            Label sep = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridData sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            sepGd.horizontalSpan = 3;
            sep.setLayoutData(sepGd);
            
            // Custom Frontend URL
            frontendUrlText = createCustomDomainField(container, "Custom frontend slug/domain:", "e.g., myapp");
            if (project.customFrontendUrl != null) {
                frontendUrlText.setText(toDomainInput(project.customFrontendUrl));
            }
            
            // Custom Backend URL
            backendUrlText = createCustomDomainField(container, "Custom backend slug/domain:", "e.g., api-myapp");
            if (project.customBackendUrl != null) {
                backendUrlText.setText(toDomainInput(project.customBackendUrl));
            }
            
            // Separator for ports
            Label portSep = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridData portSepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            portSepGd.horizontalSpan = 3;
            portSep.setLayoutData(portSepGd);
            
            // Internal Listening Ports section label
            Label portSectionLabel = new Label(container, SWT.NONE);
            portSectionLabel.setText("Internal Listening Ports:");
            GridData portSectionGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            portSectionGd.horizontalSpan = 3;
            portSectionLabel.setLayoutData(portSectionGd);
            
            // Frontend listening port
            new Label(container, SWT.NONE).setText("Frontend Port:");
            frontendPortText = new Text(container, SWT.BORDER);
            frontendPortText.setLayoutData(spanTwoColumns());
            frontendPortText.setMessage("default: 80");
            if (project.frontendListeningPort != null) {
                frontendPortText.setText(String.valueOf(project.frontendListeningPort));
            }
            
            // Backend listening port
            new Label(container, SWT.NONE).setText("Backend Port:");
            backendPortText = new Text(container, SWT.BORDER);
            backendPortText.setLayoutData(spanTwoColumns());
            backendPortText.setMessage("default: 7776");
            if (project.backendListeningPort != null) {
                backendPortText.setText(String.valueOf(project.backendListeningPort));
            }
            
            // Status label
            statusLabel = new Label(container, SWT.NONE);
            statusLabel.setText("");
            GridData statusGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            statusGd.horizontalSpan = 3;
            statusLabel.setLayoutData(statusGd);
            
            return container;
        }
        
        private Integer parsePort(String portStr) {
            try {
                return portStr.isEmpty() ? null : Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        @Override
        protected void okPressed() {
            String name = nameText.getText().trim();
            String desc = descText.getText().trim();
            String feUrl = frontendUrlText.getText().trim();
            String beUrl = backendUrlText.getText().trim();
            Integer fePort = parsePort(frontendPortText.getText().trim());
            Integer bePort = parsePort(backendPortText.getText().trim());
            
            statusLabel.setText("Saving...");
            getButton(IDialogConstants.OK_ID).setEnabled(false);
            
            new Thread(() -> {
                CliResult result = cliRunner.updateProjectJson(project.slug, 
                    name.isEmpty() ? null : name,
                    desc.isEmpty() ? null : desc,
                    feUrl.isEmpty() ? null : feUrl,
                    beUrl.isEmpty() ? null : beUrl,
                    fePort, bePort);
                
                Display.getDefault().asyncExec(() -> {
                    if (statusLabel.isDisposed()) return;
                    
                    if (result.isSuccess()) {
                        statusLabel.setText("Saved successfully!");
                        // Update local project info
                        if (!name.isEmpty()) project.name = name;
                        if (!desc.isEmpty()) project.description = desc;
                        if (fePort != null) project.frontendListeningPort = fePort;
                        if (bePort != null) project.backendListeningPort = bePort;
                        super.okPressed();
                    } else {
                        statusLabel.setText("Failed: " + result.getMessage());
                        getButton(IDialogConstants.OK_ID).setEnabled(true);
                    }
                });
            }).start();
        }
        
        @Override
        protected Point getInitialSize() {
            return new Point(560, 400);
        }

        private Text createCustomDomainField(Composite container, String labelText, String message) {
            new Label(container, SWT.NONE).setText(labelText);

            Text text = new Text(container, SWT.BORDER);
            text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            text.setMessage(message);

            Label suffixLabel = new Label(container, SWT.NONE);
            suffixLabel.setText(Utils.DEPLOYMENT_PARENT_DOMAIN_SUFFIX);
            suffixLabel.setToolTipText("Leave the field empty to keep custom URL unset, or enter a full domain to use another host.");

            return text;
        }

        private GridData spanTwoColumns() {
            GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            gd.horizontalSpan = 2;
            return gd;
        }

        private String toDomainInput(String domain) {
            String suffix = Utils.DEPLOYMENT_PARENT_DOMAIN_SUFFIX;
            if (domain != null && domain.endsWith(suffix)) {
                return domain.substring(0, domain.length() - suffix.length());
            }
            return domain;
        }
    }
}
