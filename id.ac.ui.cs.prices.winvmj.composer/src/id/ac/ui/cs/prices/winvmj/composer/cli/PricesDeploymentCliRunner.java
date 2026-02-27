package id.ac.ui.cs.prices.winvmj.composer.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
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

    private static final String CLI_JAR_NAME = "prices-deployment-cli-1.0.0.jar";
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
     * Run a CLI command with --json flag and parse the result.
     */
    public CliResult runJsonCommand(String... args) {
        WinVMJConsole.println("[DEBUG] runJsonCommand START");
        StringBuilder output = new StringBuilder();
        
        // Build args with --json flag
        String[] jsonArgs = new String[args.length + 1];
        System.arraycopy(args, 0, jsonArgs, 0, args.length);
        jsonArgs[args.length] = "--json";
        
        WinVMJConsole.println("[DEBUG] About to run CLI command...");
        int exitCode = runCommand(line -> {
            // Filter out our own logging lines
            if (!line.startsWith("[PRICES CLI]")) {
                output.append(line).append("\n");
            }
            WinVMJConsole.println("[DEBUG CLI OUTPUT] " + line);
        }, jsonArgs);
        
        WinVMJConsole.println("[DEBUG] CLI finished with exitCode=" + exitCode);
        WinVMJConsole.println("[DEBUG] Raw output: " + output.toString());
        
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
    public CliResult createProjectJson(String name, String description, String customFrontendUrl, String customBackendUrl) {
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
        return runJsonCommand(args.toArray(new String[0]));
    }
    
    /**
     * Deploy a project.
     */
    public int deploy(String projectSlug, Path archivePath) {
        return runCommand("deploy", "--project", projectSlug, "--path", archivePath.toString());
    }
    
    /**
     * Deploy a project with version.
     */
    public int deploy(String projectSlug, Path archivePath, String version) {
        return runCommand("deploy", 
            "--project", projectSlug, 
            "--path", archivePath.toString(),
            "--version", version);
    }
    
    /**
     * Deploy a project (interactive mode - will prompt for project selection).
     */
    public int deployInteractive(Path archivePath) {
        return runCommand("deploy", "--path", archivePath.toString());
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
        return runJsonCommand(args.toArray(new String[0]));
    }
    
    /**
     * Delete a project.
     */
    public int deleteProject(String slug) {
        return runCommand("delete", "-y", slug);
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
}
