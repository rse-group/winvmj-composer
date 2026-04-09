package id.ac.ui.cs.prices.winvmj.composer.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.runtime.FileLocator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;

/**
 * Helper class to run prices-deployment-cli commands via ProcessBuilder.
 * The CLI JAR runs in a separate JVM process to avoid dependency conflicts.
 */
public class PricesDeploymentCliRunner {

    private static final String CLI_JAR_NAME = "prices-cli.jar";
    private static final String BUNDLE_ID = "id.ac.ui.cs.prices.winvmj.composer";
    
    public PricesDeploymentCliRunner() {
    }
    
    /**
     * Locate the CLI JAR file from the plugin's libs folder.
     */
    private String locateCliJar() throws Exception {
        Bundle bundle = Platform.getBundle(BUNDLE_ID);
        if (bundle == null) {
            throw new RuntimeException("Bundle not found: " + BUNDLE_ID);
        }
        
        URL jarURL = FileLocator.toFileURL(bundle.getEntry("libs/" + CLI_JAR_NAME));
        if (jarURL == null) {
            throw new RuntimeException("CLI JAR not found: libs/" + CLI_JAR_NAME);
        }
        
        File jarFile = new File(jarURL.toURI());
        if (!jarFile.exists()) {
            throw new RuntimeException("CLI JAR file does not exist: " + jarFile.getAbsolutePath());
        }
        
        return jarFile.getAbsolutePath();
    }
    
    /**
     * Build the base command with java -jar and optional global flags.
     */
    private List<String> buildBaseCommand() throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-jar");
        cmd.add(locateCliJar());
        return cmd;
    }
    
    /**
     * Run a CLI command and stream output to WinVMJConsole.
     * @return exit code
     */
    public int runCommand(String... args) {
        return runCommand(WinVMJConsole::println, args);
    }
    
    /**
     * Run a CLI command with custom output consumer.
     * @return exit code
     */
    public int runCommand(Consumer<String> outputConsumer, String... args) {
        try {
            List<String> cmd = buildBaseCommand();
            for (String arg : args) {
                cmd.add(arg);
            }
            
            outputConsumer.accept("[PRICES CLI] Running: " + String.join(" ", cmd));
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputConsumer.accept(line);
                }
            }
            
            int exitCode = process.waitFor();
            outputConsumer.accept("[PRICES CLI] Exit code: " + exitCode);
            return exitCode;
            
        } catch (Exception e) {
            outputConsumer.accept("[PRICES CLI ERROR] " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Run a CLI command asynchronously.
     */
    public CompletableFuture<Integer> runCommandAsync(String... args) {
        return CompletableFuture.supplyAsync(() -> runCommand(args));
    }
    
    /**
     * Run a CLI command asynchronously with custom output consumer.
     */
    public CompletableFuture<Integer> runCommandAsync(Consumer<String> outputConsumer, String... args) {
        return CompletableFuture.supplyAsync(() -> runCommand(outputConsumer, args));
    }
    
    /**
     * Holds a running interactive process that can receive input.
     */
    public static class InteractiveProcess {
        private final Process process;
        private final OutputStream stdin;
        private volatile boolean running = true;
        
        public InteractiveProcess(Process process) {
            this.process = process;
            this.stdin = process.getOutputStream();
        }
        
        /**
         * Send input to the process (e.g., password response).
         */
        public void sendInput(String input) throws IOException {
            if (running && stdin != null) {
                stdin.write((input + "\n").getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }
        }
        
        /**
         * Check if the process is still running.
         */
        public boolean isRunning() {
            return running && process.isAlive();
        }
        
        /**
         * Wait for the process to complete and return exit code.
         */
        public int waitFor() throws InterruptedException {
            int exitCode = process.waitFor();
            running = false;
            return exitCode;
        }
        
        /**
         * Forcibly terminate the process.
         */
        public void destroy() {
            running = false;
            process.destroyForcibly();
        }
    }
    
    /**
     * Start an interactive CLI command that can receive input.
     * Returns immediately with an InteractiveProcess handle.
     * Output is streamed to the outputConsumer.
     * Call sendInput() on the returned handle to respond to prompts.
     * 
     * @param outputConsumer receives output lines
     * @param completionCallback called with exit code when process finishes
     * @param args command arguments
     * @return InteractiveProcess handle for sending input
     */
    public InteractiveProcess startInteractiveCommand(
            Consumer<String> outputConsumer,
            Consumer<Integer> completionCallback,
            String... args) {
        try {
            List<String> cmd = buildBaseCommand();
            for (String arg : args) {
                cmd.add(arg);
            }
            
            outputConsumer.accept("[PRICES CLI] Running: " + String.join(" ", cmd));
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            InteractiveProcess interactive = new InteractiveProcess(process);
            
            // Start output reader thread
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputConsumer.accept(line);
                    }
                } catch (IOException e) {
                    // Process ended
                }
                
                try {
                    int exitCode = process.waitFor();
                    outputConsumer.accept("[PRICES CLI] Exit code: " + exitCode);
                    if (completionCallback != null) {
                        completionCallback.accept(exitCode);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();
            
            return interactive;
            
        } catch (Exception e) {
            outputConsumer.accept("[PRICES CLI ERROR] " + e.getMessage());
            if (completionCallback != null) {
                completionCallback.accept(-1);
            }
            return null;
        }
    }
    
    /**
     * Run a CLI command with --json flag and parse the result.
     */
    public CliResult runJsonCommand(String... args) {
        StringBuilder output = new StringBuilder();
        
        // Build args with --json flag
        String[] jsonArgs = new String[args.length + 1];
        System.arraycopy(args, 0, jsonArgs, 0, args.length);
        jsonArgs[args.length] = "--json";
        
        int exitCode = runCommand(line -> {
            // Filter out our own logging lines
            if (!line.startsWith("[PRICES CLI]")) {
                output.append(line).append("\n");
            }
        }, jsonArgs);
        
        return CliResult.parse(output.toString().trim(), exitCode);
    }
    
    // ==================== Convenience Methods ====================
    
    /**
     * Check if user is authenticated and get user info.
     * @return CliResult with user info if authenticated
     */
    public CliResult getAuthenticatedUser() {
        return runJsonCommand("me");
    }
    
    /**
     * Check if user is authenticated.
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        CliResult result = getAuthenticatedUser();
        return result.isSuccess();
    }
    
    /**
     * Login to Prices platform with JSON output.
     * @return CliResult with user info on success
     */
    public CliResult loginJson(String identifier, String password) {
        return runJsonCommand("login", "-u", identifier, "-p", password);
    }
    
    /**
     * Logout from Prices platform with JSON output.
     * @return CliResult with success status
     */
    public CliResult logoutJson() {
        return runJsonCommand("logout");
    }
    
    /**
     * Register to Prices platform with JSON output.
     * @return CliResult with user info on success
     */
    public CliResult registerJson(String username, String email, String password) {
        return runJsonCommand("register", "-u", username, "-e", email, "-p", password);
    }
    
    /**
     * Login to Prices platform (legacy, no JSON).
     */
    public int login(String identifier, String password) {
        return runCommand("login", "-u", identifier, "-p", password, "--json");
    }
    
    /**
     * Get current user info.
     */
    public int me() {
        return runCommand("me");
    }
    
    /**
     * Logout from Prices platform.
     */
    public int logout() {
        return runCommand("logout");
    }
    
    /**
     * List all projects.
     */
    public int listProjects() {
        return runCommand("projects");
    }
    
    /**
     * List all projects with JSON output.
     * @return CliResult with projects array on success
     */
    public CliResult listProjectsJson() {
        return runJsonCommand("projects");
    }
    
    /**
     * Get project details.
     */
    public int getProject(String slug) {
        return runCommand("project", slug);
    }
    
    /**
     * Create a new project.
     */
    public int createProject(String name, String description) {
        return runCommand("create", "--name", name, "--desc", description);
    }
    
    /**
     * Create a new project with custom URLs.
     */
    public int createProject(String name, String description, String customFrontendUrl, String customBackendUrl) {
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add("create");
        args.add("--name");
        args.add(name);
        if (description != null && !description.isEmpty()) {
            args.add("--desc");
            args.add(description);
        }
        if (customFrontendUrl != null && !customFrontendUrl.isEmpty()) {
            args.add("--frontend-url");
            args.add(customFrontendUrl);
        }
        if (customBackendUrl != null && !customBackendUrl.isEmpty()) {
            args.add("--backend-url");
            args.add(customBackendUrl);
        }
        return runCommand(args.toArray(new String[0]));
    }
    
    /**
     * Create a new project with JSON output.
     */
    public CliResult createProjectJson(String name, String description, String productLine,
                                        String customFrontendUrl, String customBackendUrl) {
        return createProjectJson(name, description, productLine, customFrontendUrl, customBackendUrl, null, null);
    }
    
    /**
     * Create a new project with JSON output including listening ports.
     * @param frontendListeningPort internal port the frontend listens on (null for default 80)
     * @param backendListeningPort internal port the backend listens on (null for default 7776)
     */
    public CliResult createProjectJson(String name, String description, String productLine,
                                        String customFrontendUrl, String customBackendUrl,
                                        Integer frontendListeningPort, Integer backendListeningPort) {
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add("create");
        args.add("--name");
        args.add(name);
        if (description != null && !description.isEmpty()) {
            args.add("--desc");
            args.add(description);
        }
        if (productLine != null && !productLine.isEmpty()) {
            args.add("--product-line");
            args.add(productLine);
        }
        if (customFrontendUrl != null && !customFrontendUrl.isEmpty()) {
            args.add("--frontend-url");
            args.add(customFrontendUrl);
        }
        if (customBackendUrl != null && !customBackendUrl.isEmpty()) {
            args.add("--backend-url");
            args.add(customBackendUrl);
        }
        if (frontendListeningPort != null) {
            args.add("--frontend-port");
            args.add(String.valueOf(frontendListeningPort));
        }
        if (backendListeningPort != null) {
            args.add("--backend-port");
            args.add(String.valueOf(backendListeningPort));
        }
        return runJsonCommand(args.toArray(new String[0]));
    }
    
    /**
     * Deploy a folder to a project. CLI handles validation and archiving.
     */
    public int deploy(String projectSlug, String folderPath) {
        return runCommand("deploy", folderPath, "--project", projectSlug, "-y");
    }
    
    /**
     * Deploy a folder to a project with custom output consumer for streaming logs.
     */
    public int deploy(Consumer<String> outputConsumer, String projectSlug, String folderPath) {
        return runCommand(outputConsumer, "deploy", folderPath, "--project", projectSlug, "-y");
    }
    
    /**
     * Deploy a folder (interactive mode - will prompt for project selection).
     */
    public int deployInteractive(String folderPath) {
        return runCommand("deploy", folderPath);
    }
    
    /**
     * Get deployment status.
     */
    public int status(String deploymentId) {
        return runCommand("status", deploymentId);
    }
    
    /**
     * Get deployment history for a project.
     */
    public int history(String projectSlug) {
        return runCommand("history", "--project", projectSlug);
    }
    
    /**
     * Get deployment history for a project (JSON output).
     */
    public CliResult historyJson(String projectSlug) {
        return runJsonCommand("history", projectSlug);
    }
    
    /**
     * Get project logs.
     */
    public int logs(String projectSlug) {
        return runCommand("logs", "--project", projectSlug);
    }
    
    /**
     * Get project logs with line limit.
     */
    public int logs(String projectSlug, int lines) {
        return runCommand("logs", "--project", projectSlug, "--lines", String.valueOf(lines));
    }
    
    /**
     * Get project logs and return as string.
     */
    public String getProjectLogs(String projectSlug, int lines) {
        StringBuilder output = new StringBuilder();
        runCommand(line -> {
            if (!line.startsWith("[PRICES CLI]")) {
                output.append(line).append("\n");
            }
        }, "logs", projectSlug, "-n", String.valueOf(lines));
        return output.toString();
    }
    
    /**
     * Stream project logs with follow mode. Returns the Process so it can be cancelled.
     */
    public Process streamProjectLogs(String projectSlug, Consumer<String> lineConsumer) {
        try {
            List<String> cmd = buildBaseCommand();
            cmd.add("logs");
            cmd.add(projectSlug);
            cmd.add("-f");
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Start reader thread
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lineConsumer.accept(line);
                    }
                } catch (Exception e) {
                    // Process was killed or stream closed
                }
            }).start();
            
            return process;
        } catch (Exception e) {
            lineConsumer.accept("[ERROR] Failed to start log streaming: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get environment variables for a project.
     */
    public int envVars(String projectSlug) {
        return runCommand("env-vars", "--project", projectSlug);
    }
    
    /**
     * Get environment variables for a project (JSON output).
     */
    public CliResult envVarsJson(String projectSlug) {
        return runJsonCommand("env", projectSlug);
    }
    
    /**
     * Set environment variable for a project.
     */
    public int setEnvVar(String projectSlug, String key, String value) {
        return runCommand("env-vars", "--project", projectSlug, "--set", key + "=" + value);
    }
    
    /**
     * Set environment variable for a project (JSON output).
     */
    public CliResult setEnvVarJson(String projectSlug, String key, String value) {
        return runJsonCommand("env", projectSlug, "--set", key + "=" + value);
    }
    
    /**
     * Replace all environment variables for a project (JSON output).
     * @param envPairs Array of KEY=VALUE strings
     */
    public CliResult replaceEnvVarsJson(String projectSlug, String... envPairs) {
        List<String> args = new ArrayList<>();
        args.add("env");
        args.add(projectSlug);
        for (String pair : envPairs) {
            args.add("--replace");
            args.add(pair);
        }
        return runJsonCommand(args.toArray(new String[0]));
    }
    
    /**
     * Get project details (JSON output).
     */
    public CliResult getProjectJson(String projectSlug) {
        return runJsonCommand("project", projectSlug);
    }
    
    /**
     * Update project settings (JSON output).
     * @param projectSlug project slug
     * @param name new name (null to skip)
     * @param description new description (null to skip)
     * @param frontendUrl custom frontend URL (null to skip)
     * @param backendUrl custom backend URL (null to skip)
     */
    public CliResult updateProjectJson(String projectSlug, String name, String description, 
                                        String frontendUrl, String backendUrl) {
        return updateProjectJson(projectSlug, name, description, frontendUrl, backendUrl, null, null);
    }
    
    /**
     * Update project settings (JSON output) including listening ports.
     * @param projectSlug project slug
     * @param name new name (null to skip)
     * @param description new description (null to skip)
     * @param frontendUrl custom frontend URL (null to skip)
     * @param backendUrl custom backend URL (null to skip)
     * @param frontendListeningPort internal port the frontend listens on (null to skip)
     * @param backendListeningPort internal port the backend listens on (null to skip)
     */
    public CliResult updateProjectJson(String projectSlug, String name, String description, 
                                        String frontendUrl, String backendUrl,
                                        Integer frontendListeningPort, Integer backendListeningPort) {
        List<String> args = new ArrayList<>();
        args.add("update");
        args.add(projectSlug);
        if (name != null && !name.isEmpty()) {
            args.add("--name");
            args.add(name);
        }
        if (description != null && !description.isEmpty()) {
            args.add("--desc");
            args.add(description);
        }
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            args.add("--frontend-url");
            args.add(frontendUrl);
        }
        if (backendUrl != null && !backendUrl.isEmpty()) {
            args.add("--backend-url");
            args.add(backendUrl);
        }
        if (frontendListeningPort != null) {
            args.add("--frontend-port");
            args.add(String.valueOf(frontendListeningPort));
        }
        if (backendListeningPort != null) {
            args.add("--backend-port");
            args.add(String.valueOf(backendListeningPort));
        }
        return runJsonCommand(args.toArray(new String[0]));
    }
    
    /**
     * Get monitoring dashboard URL for a project (JSON output).
     * @return CliResult with url and expiredAt fields
     */
    public CliResult monitoringJson(String projectSlug) {
        return runJsonCommand("monitoring", projectSlug);
    }
    
    /**
     * Delete a project.
     */
    public int deleteProject(String slug) {
        return runCommand("delete", "-y", slug);
    }
    
    // ==================== SSH Deploy Methods ====================
    
    /**
     * Start interactive SSH deployment - supports password/passphrase prompts.
     * Returns immediately with an InteractiveProcess handle.
     * 
     * @param projectPath local project directory path
     * @param sshHost SSH host from ~/.ssh/config
     * @param projectName project name for deployment
     * @param frontendUrl optional custom frontend URL
     * @param backendUrl optional custom backend URL
     * @param frontendPort frontend listening port
     * @param backendPort backend listening port
     * @param outputConsumer consumer for output lines
     * @param completionCallback called with exit code when process finishes
     * @return InteractiveProcess handle for sending input (password, etc.)
     */
    public InteractiveProcess startSshDeploy(Path projectPath, String sshHost, String projectName,
                                              String frontendUrl, String backendUrl,
                                              int frontendPort, int backendPort,
                                              Consumer<String> outputConsumer,
                                              Consumer<Integer> completionCallback) {
        List<String> args = new ArrayList<>();
        args.add("deploy-ssh");
        args.add(projectPath.toAbsolutePath().toString());
        args.add("--ssh-host");
        args.add(sshHost);
        args.add("--project-name");
        args.add(projectName);
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            args.add("--frontend-url");
            args.add(frontendUrl);
        }
        if (backendUrl != null && !backendUrl.isEmpty()) {
            args.add("--backend-url");
            args.add(backendUrl);
        }
        args.add("--frontend-port");
        args.add(String.valueOf(frontendPort));
        args.add("--backend-port");
        args.add(String.valueOf(backendPort));
        
        return startInteractiveCommand(outputConsumer, completionCallback, args.toArray(new String[0]));
    }
    
    /**
     * Result holder for CLI JSON responses.
     */
    public static class CliResult {
        
        private static final Gson GSON = new Gson();
        
        private final boolean success;
        private final String message;
        private final int exitCode;
        private final Map<String, Object> data;
        
        public CliResult(boolean success, String message, int exitCode, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.exitCode = exitCode;
            this.data = data != null ? data : new HashMap<>();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getExitCode() {
            return exitCode;
        }
        
        public Object getDataField(String fieldName) {
            return data.get(fieldName);
        }
        
        public String getDataFieldAsString(String fieldName) {
            Object value = data.get(fieldName);
            return value != null ? value.toString() : null;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public String getDataAsString() {
            if (data == null || data.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            return sb.toString();
        }
        
        @SuppressWarnings("unchecked")
        public static CliResult parse(String json, int exitCode) {
            if (json == null || json.trim().isEmpty()) {
                return new CliResult(false, "Empty response", exitCode, null);
            }
            
            try {
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                
                boolean success = root.has("success") && root.get("success").getAsBoolean();
                String message = root.has("message") && !root.get("message").isJsonNull() 
                    ? root.get("message").getAsString() : null;
                
                Map<String, Object> data = new HashMap<>();
                if (root.has("data") && !root.get("data").isJsonNull()) {
                    com.google.gson.JsonElement dataElement = root.get("data");
                    if (dataElement.isJsonArray()) {
                        // Handle array data (e.g., projects list)
                        data.put("_array", dataElement.toString());
                        data.put("_count", String.valueOf(dataElement.getAsJsonArray().size()));
                    } else if (dataElement.isJsonObject()) {
                        JsonObject dataObj = dataElement.getAsJsonObject();
                        for (String key : dataObj.keySet()) {
                            if (dataObj.get(key).isJsonNull()) {
                                data.put(key, null);
                            } else if (dataObj.get(key).isJsonPrimitive()) {
                                data.put(key, dataObj.get(key).getAsString());
                            } else {
                                data.put(key, dataObj.get(key).toString());
                            }
                        }
                    }
                }
                
                return new CliResult(success, message, exitCode, data);
            } catch (Exception e) {
                return new CliResult(false, "Failed to parse JSON: " + e.getMessage(), exitCode, null);
            }
        }
    }
    
    /**
     * Launch SSH deploy in external terminal (cross-platform).
     * Opens a terminal window where user can interact with SSH passphrase prompts.
     */
    public void launchSshDeployInTerminal(
            String projectPath,
            String sshHost,
            String projectName,
            String frontendUrl,
            String backendUrl,
            int frontendPort,
            int backendPort) throws Exception {
        
        String jarPath = locateCliJar();
        
        // Build the CLI command
        StringBuilder cmd = new StringBuilder();
        cmd.append("java -jar \"").append(jarPath).append("\" deploy-ssh");
        cmd.append(" \"").append(projectPath).append("\"");
        cmd.append(" --ssh-host ").append(sshHost);
        cmd.append(" --project-name ").append(projectName);
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            cmd.append(" --frontend-url ").append(frontendUrl);
        }
        if (backendUrl != null && !backendUrl.isEmpty()) {
            cmd.append(" --backend-url ").append(backendUrl);
        }
        cmd.append(" --frontend-port ").append(frontendPort);
        cmd.append(" --backend-port ").append(backendPort);
        
        String[] terminalCmd;
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows: use cmd.exe
            terminalCmd = new String[]{"cmd", "/c", "start", "cmd", "/k", cmd.toString()};
        } else if (os.contains("mac")) {
            // macOS: use Terminal.app via osascript
            // Need to escape backslashes first, then double quotes
            String escapedCmd = cmd.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
            String script = """
                tell application "Terminal" to activate
                tell application "Terminal" to do script "%s"
                """.formatted(escapedCmd);
            terminalCmd = new String[]{"osascript", "-e", script};
        } else {
            // Linux: try common terminal emulators
            String bashCmd = cmd.toString();
            if (isCommandAvailable("gnome-terminal")) {
                terminalCmd = new String[]{"gnome-terminal", "--", "bash", "-c", bashCmd + "; exec bash"};
            } else if (isCommandAvailable("konsole")) {
                terminalCmd = new String[]{"konsole", "-e", "bash", "-c", bashCmd + "; exec bash"};
            } else if (isCommandAvailable("xterm")) {
                terminalCmd = new String[]{"xterm", "-hold", "-e", bashCmd};
            } else {
                // Fallback: just run the command directly
                terminalCmd = new String[]{"bash", "-c", bashCmd};
            }
        }
        
        ProcessBuilder pb = new ProcessBuilder(terminalCmd);
        pb.start();
    }
    
    /**
     * Check if a command is available on the system.
     */
    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
