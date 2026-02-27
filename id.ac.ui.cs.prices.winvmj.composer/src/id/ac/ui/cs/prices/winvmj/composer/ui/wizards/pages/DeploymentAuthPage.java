package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner.CliResult;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;

public class DeploymentAuthPage extends WizardPage {
    
    private final DeploymentWizard wizard;
    private final PricesDeploymentCliRunner cliRunner;
    
    private Composite stackContainer;
    private StackLayout stackLayout;
    
    private Composite authPanel;
    private Composite userInfoPanel;
    private Composite loadingPanel;
    
    // Login tab fields
    private Text loginUsernameText;
    private Text loginPasswordText;
    private Label loginErrorLabel;
    
    // Register tab fields
    private Text registerUsernameText;
    private Text registerEmailText;
    private Text registerPasswordText;
    private Text registerConfirmPasswordText;
    private Label registerErrorLabel;
    
    // User info fields
    private Label userNameLabel;
    private Label userEmailLabel;
    private Label userRoleLabel;
    
    private boolean isAuthenticated = false;
    
    public DeploymentAuthPage(DeploymentWizard wizard) {
        super("Authentication");
        setTitle("Prices Authentication");
        setDescription("Login or register to deploy your project to Prices platform.");
        this.wizard = wizard;
        this.cliRunner = wizard.getCliRunner();
    }
    
    @Override
    public void createControl(Composite parent) {
        stackContainer = new Composite(parent, SWT.NONE);
        stackLayout = new StackLayout();
        stackContainer.setLayout(stackLayout);
        
        createLoadingPanel();
        createAuthPanel();
        createUserInfoPanel();
        
        stackLayout.topControl = loadingPanel;
        setControl(stackContainer);
        
        checkAuthStatus();
    }
    
    private void createLoadingPanel() {
        loadingPanel = new Composite(stackContainer, SWT.NONE);
        loadingPanel.setLayout(new GridLayout(1, false));
        
        Label loadingLabel = new Label(loadingPanel, SWT.CENTER);
        loadingLabel.setText("Checking authentication status...");
        loadingLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    }
    
    private void createAuthPanel() {
        authPanel = new Composite(stackContainer, SWT.NONE);
        authPanel.setLayout(new GridLayout(1, false));
        
        TabFolder tabFolder = new TabFolder(authPanel, SWT.TOP);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        // Login Tab
        TabItem loginTab = new TabItem(tabFolder, SWT.NONE);
        loginTab.setText("Login");
        loginTab.setControl(createLoginTabContent(tabFolder));
        
        // Register Tab
        TabItem registerTab = new TabItem(tabFolder, SWT.NONE);
        registerTab.setText("Register");
        registerTab.setControl(createRegisterTabContent(tabFolder));
    }
    
    private Composite createLoginTabContent(TabFolder parent) {
        Composite loginComposite = new Composite(parent, SWT.NONE);
        loginComposite.setLayout(new GridLayout(1, false));
        
        // Form group
        Group formGroup = new Group(loginComposite, SWT.NONE);
        formGroup.setText("Login to your account");
        formGroup.setLayout(new GridLayout(2, false));
        formGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        // Username
        Label usernameLabel = new Label(formGroup, SWT.NONE);
        usernameLabel.setText("Username or Email:");
        loginUsernameText = new Text(formGroup, SWT.BORDER);
        loginUsernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Password
        Label passwordLabel = new Label(formGroup, SWT.NONE);
        passwordLabel.setText("Password:");
        loginPasswordText = new Text(formGroup, SWT.BORDER | SWT.PASSWORD);
        loginPasswordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Error label
        loginErrorLabel = new Label(formGroup, SWT.WRAP);
        loginErrorLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        GridData errorGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        errorGd.horizontalSpan = 2;
        loginErrorLabel.setLayoutData(errorGd);
        
        // Login button
        new Label(formGroup, SWT.NONE); // spacer
        Button loginButton = new Button(formGroup, SWT.PUSH);
        loginButton.setText("Login");
        loginButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        loginButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                performLogin();
            }
        });
        
        return loginComposite;
    }
    
    private Composite createRegisterTabContent(TabFolder parent) {
        Composite registerComposite = new Composite(parent, SWT.NONE);
        registerComposite.setLayout(new GridLayout(1, false));
        
        // Form group
        Group formGroup = new Group(registerComposite, SWT.NONE);
        formGroup.setText("Create a new account");
        formGroup.setLayout(new GridLayout(2, false));
        formGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        // Username
        Label usernameLabel = new Label(formGroup, SWT.NONE);
        usernameLabel.setText("Username:");
        registerUsernameText = new Text(formGroup, SWT.BORDER);
        registerUsernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Email
        Label emailLabel = new Label(formGroup, SWT.NONE);
        emailLabel.setText("Email:");
        registerEmailText = new Text(formGroup, SWT.BORDER);
        registerEmailText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Password
        Label passwordLabel = new Label(formGroup, SWT.NONE);
        passwordLabel.setText("Password:");
        registerPasswordText = new Text(formGroup, SWT.BORDER | SWT.PASSWORD);
        registerPasswordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Confirm Password
        Label confirmLabel = new Label(formGroup, SWT.NONE);
        confirmLabel.setText("Confirm Password:");
        registerConfirmPasswordText = new Text(formGroup, SWT.BORDER | SWT.PASSWORD);
        registerConfirmPasswordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Error label
        registerErrorLabel = new Label(formGroup, SWT.WRAP);
        registerErrorLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        GridData errorGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        errorGd.horizontalSpan = 2;
        registerErrorLabel.setLayoutData(errorGd);
        
        // Register button
        new Label(formGroup, SWT.NONE); // spacer
        Button registerButton = new Button(formGroup, SWT.PUSH);
        registerButton.setText("Register");
        registerButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        registerButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                performRegister();
            }
        });
        
        return registerComposite;
    }
    
    private void createUserInfoPanel() {
        userInfoPanel = new Composite(stackContainer, SWT.NONE);
        userInfoPanel.setLayout(new GridLayout(1, false));
        
        // User card group
        Group userCard = new Group(userInfoPanel, SWT.NONE);
        userCard.setText("Authenticated User");
        userCard.setLayout(new GridLayout(2, false));
        userCard.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        // Welcome message with larger font
        Label welcomeLabel = new Label(userCard, SWT.NONE);
        welcomeLabel.setText("Welcome back!");
        welcomeLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
        FontData[] fontData = welcomeLabel.getFont().getFontData();
        fontData[0].setHeight(fontData[0].getHeight() + 2);
        fontData[0].setStyle(SWT.BOLD);
        welcomeLabel.setFont(new Font(Display.getCurrent(), fontData[0]));
        GridData welcomeGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        welcomeGd.horizontalSpan = 2;
        welcomeLabel.setLayoutData(welcomeGd);
        
        // Separator
        Label separator = new Label(userCard, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sepGd.horizontalSpan = 2;
        separator.setLayoutData(sepGd);
        
        // Username row
        Label userLabel = new Label(userCard, SWT.NONE);
        userLabel.setText("Username:");
        userLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        userNameLabel = new Label(userCard, SWT.NONE);
        userNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Email row
        Label emailLabelTitle = new Label(userCard, SWT.NONE);
        emailLabelTitle.setText("Email:");
        emailLabelTitle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        userEmailLabel = new Label(userCard, SWT.NONE);
        userEmailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Role row
        Label roleLabelTitle = new Label(userCard, SWT.NONE);
        roleLabelTitle.setText("Role:");
        roleLabelTitle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        userRoleLabel = new Label(userCard, SWT.NONE);
        userRoleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Spacer
        Label spacer = new Label(userCard, SWT.NONE);
        GridData spacerGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        spacerGd.horizontalSpan = 2;
        spacerGd.heightHint = 10;
        spacer.setLayoutData(spacerGd);
        
        // Logout button - right aligned
        new Label(userCard, SWT.NONE); // left spacer
        Button logoutButton = new Button(userCard, SWT.PUSH);
        logoutButton.setText("Logout");
        logoutButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        logoutButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                performLogout();
            }
        });
        
        // Info label below the card
        Label infoLabel = new Label(userInfoPanel, SWT.WRAP);
        infoLabel.setText("You are authenticated. Click 'Next' to continue with your projects.");
        infoLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
        infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }
    
    private void checkAuthStatus() {
        new Thread(() -> {
            WinVMJConsole.println("[AUTH] Checking authentication status...");
            CliResult result = cliRunner.getAuthenticatedUser();
            
            Display.getDefault().asyncExec(() -> {
                if (result.isSuccess()) {
                    isAuthenticated = true;
                    wizard.setUserInfo(result);
                    showUserInfo(result);
                } else {
                    isAuthenticated = false;
                    showAuthPanel();
                }
                updatePageComplete();
            });
        }).start();
    }
    
    private void performLogin() {
        String username = loginUsernameText.getText().trim();
        String password = loginPasswordText.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            loginErrorLabel.setText("Please enter username and password.");
            authPanel.layout(true, true);
            return;
        }
        
        loginErrorLabel.setText("");
        stackLayout.topControl = loadingPanel;
        stackContainer.layout();
        
        new Thread(() -> {
            WinVMJConsole.println("[AUTH] Logging in as: " + username);
            CliResult result = cliRunner.loginJson(username, password);
            
            Display.getDefault().asyncExec(() -> {
                if (result.isSuccess()) {
                    isAuthenticated = true;
                    wizard.setUserInfo(result);
                    showUserInfo(result);
                    WinVMJConsole.println("[AUTH] Login successful!");
                } else {
                    isAuthenticated = false;
                    loginErrorLabel.setText(result.getMessage() != null ? result.getMessage() : "Login failed.");
                    showAuthPanel();
                    WinVMJConsole.println("[AUTH] Login failed: " + result.getMessage());
                }
                updatePageComplete();
            });
        }).start();
    }
    
    private void performRegister() {
        String username = registerUsernameText.getText().trim();
        String email = registerEmailText.getText().trim();
        String password = registerPasswordText.getText();
        String confirmPassword = registerConfirmPasswordText.getText();
        
        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            registerErrorLabel.setText("Please fill in all fields.");
            authPanel.layout(true, true);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            registerErrorLabel.setText("Passwords do not match.");
            authPanel.layout(true, true);
            return;
        }
        
        if (password.length() < 6) {
            registerErrorLabel.setText("Password must be at least 6 characters.");
            authPanel.layout(true, true);
            return;
        }
        
        registerErrorLabel.setText("");
        stackLayout.topControl = loadingPanel;
        stackContainer.layout();
        
        new Thread(() -> {
            WinVMJConsole.println("[AUTH] Registering user: " + username);
            CliResult registerResult = cliRunner.registerJson(username, email, password);
            
            Display.getDefault().asyncExec(() -> {
                if (registerResult.isSuccess()) {
                    WinVMJConsole.println("[AUTH] Registration successful! Logging in...");
                    // Auto-login after successful registration
                    performAutoLoginAfterRegister(username, password);
                } else {
                    registerErrorLabel.setText(registerResult.getMessage() != null ? registerResult.getMessage() : "Registration failed.");
                    showAuthPanel();
                    WinVMJConsole.println("[AUTH] Registration failed: " + registerResult.getMessage());
                }
            });
        }).start();
    }
    
    private void performAutoLoginAfterRegister(String username, String password) {
        new Thread(() -> {
            WinVMJConsole.println("[AUTH] Auto-login after registration...");
            CliResult loginResult = cliRunner.loginJson(username, password);
            
            Display.getDefault().asyncExec(() -> {
                if (loginResult.isSuccess()) {
                    isAuthenticated = true;
                    wizard.setUserInfo(loginResult);
                    showUserInfo(loginResult);
                    WinVMJConsole.println("[AUTH] Auto-login successful!");
                } else {
                    registerErrorLabel.setText("Registered but login failed. Please login manually.");
                    showAuthPanel();
                    WinVMJConsole.println("[AUTH] Auto-login failed: " + loginResult.getMessage());
                }
                updatePageComplete();
            });
        }).start();
    }
    
    private void performLogout() {
        stackLayout.topControl = loadingPanel;
        stackContainer.layout();
        
        new Thread(() -> {
            WinVMJConsole.println("[AUTH] Logging out...");
            cliRunner.logoutJson();
            
            Display.getDefault().asyncExec(() -> {
                isAuthenticated = false;
                wizard.setUserInfo(null);
                // Clear login fields
                loginUsernameText.setText("");
                loginPasswordText.setText("");
                loginErrorLabel.setText("");
                // Clear register fields
                registerUsernameText.setText("");
                registerEmailText.setText("");
                registerPasswordText.setText("");
                registerConfirmPasswordText.setText("");
                registerErrorLabel.setText("");
                
                showAuthPanel();
                updatePageComplete();
                WinVMJConsole.println("[AUTH] Logged out.");
            });
        }).start();
    }
    
    private void showAuthPanel() {
        stackLayout.topControl = authPanel;
        stackContainer.layout();
    }
    
    private void showUserInfo(CliResult userInfo) {
        String username = userInfo.getDataFieldAsString("username");
        String email = userInfo.getDataFieldAsString("email");
        
        String role = userInfo.getDataFieldAsString("role");
        
        userNameLabel.setText(username != null ? username : "-");
        userEmailLabel.setText(email != null ? email : "-");
        userRoleLabel.setText(role != null ? role : "-");
        
        stackLayout.topControl = userInfoPanel;
        stackContainer.layout();
    }
    
    private void updatePageComplete() {
        setPageComplete(isAuthenticated);
    }
    
    @Override
    public boolean isPageComplete() {
        return isAuthenticated;
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
}
