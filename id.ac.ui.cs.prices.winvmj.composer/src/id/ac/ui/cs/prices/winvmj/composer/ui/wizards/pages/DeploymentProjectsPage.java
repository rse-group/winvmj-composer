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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner.CliResult;
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
                    nameCol.setWidth((int)(totalWidth * 0.12));
                    slugCol.setWidth((int)(totalWidth * 0.15));
                    descCol.setWidth((int)(totalWidth * 0.15));
                    statusCol.setWidth((int)(totalWidth * 0.08));
                    frontendCol.setWidth((int)(totalWidth * 0.25));
                    backendCol.setWidth((int)(totalWidth * 0.25));
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
    
    private void parseProjects(CliResult result) {
        String arrayJson = result.getDataFieldAsString("_array");
        if (arrayJson == null || arrayJson.isEmpty()) {
            return;
        }
        
        try {
            Gson gson = new Gson();
            JsonArray array = gson.fromJson(arrayJson, JsonArray.class);
            
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                ProjectInfo info = new ProjectInfo();
                info.name = obj.has("name") ? obj.get("name").getAsString() : "Unknown";
                info.slug = obj.has("slug") ? obj.get("slug").getAsString() : "";
                info.description = obj.has("description") && !obj.get("description").isJsonNull() 
                    ? obj.get("description").getAsString() : "";
                info.status = obj.has("status") ? obj.get("status").getAsString() : "unknown";
                
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
                
                projects.add(info);
                
                TableItem item = new TableItem(projectsTable, SWT.NONE);
                item.setText(new String[] { info.name, info.slug, info.description, info.status, info.frontendUrls, info.backendUrls });
            }
        } catch (Exception e) {
            WinVMJConsole.println("[PROJECTS] Failed to parse projects: " + e.getMessage());
        }
    }
    
    private void openCreateProjectDialog() {
        CreateProjectDialog dialog = new CreateProjectDialog(getShell(), wizard.getFeatureProject().getProjectName());
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            createProject(dialog.getProjectName(), dialog.getDescription(), 
                         dialog.getCustomFrontendUrl(), dialog.getCustomBackendUrl());
        }
    }
    
    private void createProject(String name, String description, String customFrontendUrl, String customBackendUrl) {
        statusLabel.setText("Creating project '" + name + "'...");
        
        new Thread(() -> {
            WinVMJConsole.println("[PROJECTS] Creating project: " + name);
            CliResult result = cliRunner.createProjectJson(name, description, customFrontendUrl, customBackendUrl);
            
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
        private Text customFrontendText;
        private Text customBackendText;
        
        private String projectName;
        private String description;
        private String customFrontendUrl;
        private String customBackendUrl;
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
            container.setLayout(new GridLayout(2, false));
            
            // Project name
            Label nameLabel = new Label(container, SWT.NONE);
            nameLabel.setText("Project Name:*");
            nameText = new Text(container, SWT.BORDER);
            nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            if (defaultName != null) {
                nameText.setText(defaultName);
            }
            
            // Description
            Label descLabel = new Label(container, SWT.NONE);
            descLabel.setText("Description:");
            descriptionText = new Text(container, SWT.BORDER);
            descriptionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            descriptionText.setText("Deployed from WinVMJ Composer");
            
            // Separator
            Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridData sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            sepGd.horizontalSpan = 2;
            separator.setLayoutData(sepGd);
            
            // Custom URLs section label
            Label urlLabel = new Label(container, SWT.NONE);
            urlLabel.setText("Custom URLs (optional):");
            GridData urlLabelGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            urlLabelGd.horizontalSpan = 2;
            urlLabel.setLayoutData(urlLabelGd);
            
            // Custom frontend URL
            Label frontendLabel = new Label(container, SWT.NONE);
            frontendLabel.setText("Frontend URL:");
            customFrontendText = new Text(container, SWT.BORDER);
            customFrontendText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            customFrontendText.setMessage("e.g., myapp.example.com");
            
            // Custom backend URL
            Label backendLabel = new Label(container, SWT.NONE);
            backendLabel.setText("Backend URL:");
            customBackendText = new Text(container, SWT.BORDER);
            customBackendText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            customBackendText.setMessage("e.g., api.example.com");
            
            return container;
        }
        
        @Override
        protected Point getInitialSize() {
            return new Point(500, 350);
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
            customFrontendUrl = customFrontendText.getText().trim();
            customBackendUrl = customBackendText.getText().trim();
            
            if (projectName.isEmpty()) {
                nameText.setFocus();
                return;
            }
            super.okPressed();
        }
        
        public String getProjectName() { return projectName; }
        public String getDescription() { return description; }
        public String getCustomFrontendUrl() { return customFrontendUrl; }
        public String getCustomBackendUrl() { return customBackendUrl; }
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
        String frontendUrls;
        String backendUrls;
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
            this.project = project;
            this.cliRunner = cliRunner;
            this.onChangeCallback = onChangeCallback;
        }
        
        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Project: " + project.name);
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
            
            Label v = new Label(parent, SWT.NONE);
            String status = active ? " [ACTIVE]" : " [INACTIVE]";
            v.setText(url + status);
            GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            gd.widthHint = 350;
            v.setLayoutData(gd);
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
                                    JsonArray array = new Gson().fromJson(arrayJson, JsonArray.class);
                                    if (array.size() == 0) {
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
        
        @Override
        protected Point getInitialSize() {
            return new Point(700, 550);
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
            container.setLayout(new GridLayout(2, false));
            
            // Name
            new Label(container, SWT.NONE).setText("Name:");
            nameText = new Text(container, SWT.BORDER);
            nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            nameText.setText(project.name != null ? project.name : "");
            
            // Description
            new Label(container, SWT.NONE).setText("Description:");
            descText = new Text(container, SWT.BORDER);
            descText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            descText.setText(project.description != null ? project.description : "");
            
            // Separator
            Label sep = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridData sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            sepGd.horizontalSpan = 2;
            sep.setLayoutData(sepGd);
            
            // Custom Frontend URL
            new Label(container, SWT.NONE).setText("Custom Frontend URL:");
            frontendUrlText = new Text(container, SWT.BORDER);
            frontendUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            
            // Custom Backend URL
            new Label(container, SWT.NONE).setText("Custom Backend URL:");
            backendUrlText = new Text(container, SWT.BORDER);
            backendUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            
            // Status label
            statusLabel = new Label(container, SWT.NONE);
            statusLabel.setText("");
            GridData statusGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            statusGd.horizontalSpan = 2;
            statusLabel.setLayoutData(statusGd);
            
            return container;
        }
        
        @Override
        protected void okPressed() {
            String name = nameText.getText().trim();
            String desc = descText.getText().trim();
            String feUrl = frontendUrlText.getText().trim();
            String beUrl = backendUrlText.getText().trim();
            
            statusLabel.setText("Saving...");
            getButton(IDialogConstants.OK_ID).setEnabled(false);
            
            new Thread(() -> {
                CliResult result = cliRunner.updateProjectJson(project.slug, 
                    name.isEmpty() ? null : name,
                    desc.isEmpty() ? null : desc,
                    feUrl.isEmpty() ? null : feUrl,
                    beUrl.isEmpty() ? null : beUrl);
                
                Display.getDefault().asyncExec(() -> {
                    if (statusLabel.isDisposed()) return;
                    
                    if (result.isSuccess()) {
                        statusLabel.setText("Saved successfully!");
                        // Update local project info
                        if (!name.isEmpty()) project.name = name;
                        if (!desc.isEmpty()) project.description = desc;
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
            return new Point(450, 300);
        }
    }
}
