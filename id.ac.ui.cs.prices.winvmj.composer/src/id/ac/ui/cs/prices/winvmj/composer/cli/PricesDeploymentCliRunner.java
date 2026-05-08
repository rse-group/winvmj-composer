package id.ac.ui.cs.prices.winvmj.composer.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
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
    private static final String CLI_JAR_PATH_IN_BUNDLE = "libs/" + CLI_JAR_NAME;
    
    public PricesDeploymentCliRunner() {
    }
    
    /**
     * Locate the CLI JAR file from the plugin's resources/winvmj-libraries folder.
     *
     * Fails fast with a human-readable message that pinpoints which step broke:
     *  (1) plugin bundle missing      -> host Eclipse didn't load our plugin
     *  (2) bundle entry missing       -> CLI jar wasn't packaged into the plugin (build.properties)
     *  (3) FileLocator unpack failed  -> OSGi couldn't extract the jar from the bundle
     *  (4) file doesn't exist on disk -> unpack returned a path but the file is gone
     */
    private String locateCliJar() throws Exception {
        Bundle bundle = Platform.getBundle(BUNDLE_ID);
        if (bundle == null) {
            throw new RuntimeException("[CLI-NOT-BUNDLED] Plugin bundle not found: " + BUNDLE_ID
                + ". The composer plugin itself is not loaded in this Eclipse instance.");
        }

        URL entry = bundle.getEntry(CLI_JAR_PATH_IN_BUNDLE);
        if (entry == null) {
            throw new RuntimeException("[CLI-NOT-BUNDLED] '" + CLI_JAR_PATH_IN_BUNDLE
                + "' is not present inside bundle " + BUNDLE_ID + " (version "
                + bundle.getVersion() + "). The prices-cli.jar was not packaged with this plugin -"
                + " check build.properties 'bin.includes' and make sure libs/"
                + CLI_JAR_NAME + " exists in the installed plugin.");
        }

        URL jarURL;
        try {
            jarURL = FileLocator.toFileURL(entry);
        } catch (IOException ioe) {
            throw new RuntimeException("[CLI-UNPACK-FAILED] Could not extract "
                + CLI_JAR_PATH_IN_BUNDLE + " from bundle " + BUNDLE_ID
                + ": " + ioe.getMessage(), ioe);
        }
        if (jarURL == null) {
            throw new RuntimeException("[CLI-UNPACK-FAILED] FileLocator.toFileURL returned null for "
                + CLI_JAR_PATH_IN_BUNDLE);
        }

        // Convert URL -> File safely. Using jarURL.toURI() blows up when the
        // Eclipse install path contains spaces or other characters that are
        // illegal in a URI (e.g. "...\eclipse paling paling baru\..."), because
        // FileLocator may return a raw file: URL without %-encoding.
        // Instead, take the path component and URL-decode it ourselves.
        File jarFile = urlToFile(jarURL);
        if (!jarFile.exists()) {
            throw new RuntimeException("[CLI-NOT-ON-DISK] Extracted CLI path does not exist: "
                + jarFile.getAbsolutePath());
        }
        if (jarFile.length() == 0) {
            throw new RuntimeException("[CLI-EMPTY] CLI jar is 0 bytes at "
                + jarFile.getAbsolutePath() + ". Re-install the plugin.");
        }

        return jarFile.getAbsolutePath();
    }

    /**
     * Convert a file: {@link URL} (as returned by {@link FileLocator#toFileURL})
     * into a {@link File}, decoding percent-encoded characters if any.
     *
     * We do NOT go through {@code url.toURI()} because {@code FileLocator} may
     * return URLs whose path is not percent-encoded (e.g. contains literal
     * spaces when Eclipse is installed under "C:\...\eclipse paling baru\..."),
     * which makes {@code toURI()} throw {@code URISyntaxException}.
     */
    private static File urlToFile(URL url) {
        String path = url.getPath();
        try {
            path = java.net.URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // use the raw path
        }
        // On Windows, URL paths look like "/C:/Users/..." - drop the leading slash.
        if (path.length() > 2 && path.charAt(0) == '/' && path.charAt(2) == ':') {
            path = path.substring(1);
        }
        return new File(path);
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
     *
     * This implementation intentionally does NOT use {@code redirectErrorStream(true)}
     * so that JVM startup noise printed to stderr
     * (e.g. "Picked up JAVA_TOOL_OPTIONS", "Picked up _JAVA_OPTIONS",
     *  "WARNING: ...", unnamed-module warnings, etc.) doesn't pollute the JSON on stdout.
     * Any line that is clearly not JSON is also filtered before parsing, so the
     * CLI output is robust across different user environments.
     */
    public CliResult runJsonCommand(String... args) {
        // Build args with --json flag
        String[] jsonArgs = new String[args.length + 1];
        System.arraycopy(args, 0, jsonArgs, 0, args.length);
        jsonArgs[args.length] = "--json";

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        int exitCode;

        try {
            List<String> cmd = buildBaseCommand();
            for (String arg : jsonArgs) {
                cmd.add(arg);
            }

            WinVMJConsole.println("[PRICES CLI] Running: " + String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            // Keep stderr separate so JVM banners don't pollute the JSON body
            pb.redirectErrorStream(false);
            Process process = pb.start();

            Thread outThread = new Thread(() -> readStreamInto(process.getInputStream(), stdout));
            Thread errThread = new Thread(() -> readStreamInto(process.getErrorStream(), stderr));
            outThread.setDaemon(true);
            errThread.setDaemon(true);
            outThread.start();
            errThread.start();

            exitCode = process.waitFor();
            outThread.join();
            errThread.join();

            if (stderr.length() > 0) {
                // Surface stderr to the console for debugging, but never feed it to the parser
                for (String line : stderr.toString().split("\\r?\\n")) {
                    if (!line.isEmpty()) {
                        WinVMJConsole.println("[PRICES CLI][stderr] " + line);
                    }
                }
            }
            WinVMJConsole.println("[PRICES CLI] Exit code: " + exitCode);
        } catch (Exception e) {
            // Could not even start the java process - print full diagnostics so the user
            // immediately knows whether the CLI jar is bundled and whether `java` is on PATH.
            WinVMJConsole.println("[PRICES CLI ERROR] " + e.getMessage());
            printEnvironmentDiagnostics("could not launch CLI");
            return new CliResult(false, e.getMessage(), -1, null);
        }

        CliResult result = CliResult.parse(stdout.toString(), exitCode);

        // If parsing failed OR the CLI itself errored, print a full diagnosis so
        // we can tell whether the CLI was bundled, java exists, etc.
        if (!result.isSuccess() && (exitCode != 0 || stdout.length() == 0
                || (result.getMessage() != null && (
                        result.getMessage().startsWith("CLI did not return JSON")
                     || result.getMessage().startsWith("Failed to parse CLI JSON")
                     || result.getMessage().startsWith("Unexpected CLI response shape"))))) {
            printEnvironmentDiagnostics("exitCode=" + exitCode
                + ", stdoutLen=" + stdout.length()
                + ", stderrLen=" + stderr.length());
        }

        return result;
    }

    private static void readStreamInto(java.io.InputStream in, StringBuilder sink) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sink.append(line).append('\n');
            }
        } catch (IOException ignored) {
            // stream closed / process ended
        }
    }

    // ==================== Diagnostics ====================

    /**
     * Holds a snapshot of everything we need to tell the user *why* the CLI
     * didn't run on their machine: was the jar bundled, is java on PATH, what version, etc.
     */
    public static class DiagnosticsReport {
        public boolean bundleFound;
        public String bundleVersion;
        public boolean cliJarEntryFound;      // entry exists inside the bundle
        public boolean cliJarExtracted;       // FileLocator managed to extract it
        public String cliJarPath;
        public long cliJarSizeBytes = -1;
        public String cliLocateError;         // null if CLI was located successfully

        public boolean javaOnPath;
        public String javaVersion;            // first line of `java -version`
        public String javaLocateError;

        public String osName = System.getProperty("os.name");
        public String osArch = System.getProperty("os.arch");
        public String embeddedJavaVersion = System.getProperty("java.version");
        public String embeddedJavaHome = System.getProperty("java.home");
        public String pathEnv = System.getenv("PATH");

        /** Human-readable, one-conclusion-per-line summary. */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Prices CLI Diagnostics ===\n");
            sb.append("OS: ").append(osName).append(" / ").append(osArch).append("\n");
            sb.append("Eclipse JVM: ").append(embeddedJavaVersion)
              .append(" (").append(embeddedJavaHome).append(")\n");

            sb.append("Bundle '").append(BUNDLE_ID).append("': ")
              .append(bundleFound ? "FOUND (v" + bundleVersion + ")" : "MISSING").append("\n");
            sb.append("CLI jar entry '").append(CLI_JAR_PATH_IN_BUNDLE).append("' in bundle: ")
              .append(cliJarEntryFound ? "PRESENT" : "MISSING").append("\n");
            sb.append("CLI jar extracted to disk: ")
              .append(cliJarExtracted ? "YES" : "NO").append("\n");
            if (cliJarPath != null) {
                sb.append("CLI jar path: ").append(cliJarPath).append("\n");
                sb.append("CLI jar size: ").append(cliJarSizeBytes).append(" bytes\n");
            }
            if (cliLocateError != null) {
                sb.append("CLI locate error: ").append(cliLocateError).append("\n");
            }

            sb.append("`java` on system PATH: ")
              .append(javaOnPath ? "YES" : "NO").append("\n");
            if (javaVersion != null) {
                sb.append("System java -version: ").append(javaVersion).append("\n");
            }
            if (javaLocateError != null) {
                sb.append("java probe error: ").append(javaLocateError).append("\n");
            }

            sb.append("--- Verdict ---\n");
            if (!bundleFound) {
                sb.append("X Composer plugin itself is not loaded. Reinstall the plugin/feature.\n");
            } else if (!cliJarEntryFound) {
                sb.append("X prices-cli.jar is NOT bundled with the installed plugin.\n");
                sb.append("  -> Check build.properties 'bin.includes' contains libs/").append(CLI_JAR_NAME)
                  .append(",\n");
            } else if (!cliJarExtracted) {
                sb.append("X CLI jar entry exists but could not be extracted from the bundle.\n");
            } else if (cliJarSizeBytes == 0) {
                sb.append("X CLI jar is 0 bytes - reinstall the plugin.\n");
            } else if (!javaOnPath) {
                sb.append("X `java` is not on the user's PATH. Install a JDK 17+ or add it to PATH.\n");
            } else {
                sb.append("OK - CLI jar is bundled and java is available.\n");
                sb.append("  If commands still fail, inspect [PRICES CLI][stderr] lines above.\n");
            }
            sb.append("==============================");
            return sb.toString();
        }
    }

    /**
     * Collect a {@link DiagnosticsReport}. Safe to call any time - never throws.
     */
    public DiagnosticsReport diagnose() {
        DiagnosticsReport r = new DiagnosticsReport();

        // --- Bundle / CLI jar checks ---
        try {
            Bundle bundle = Platform.getBundle(BUNDLE_ID);
            r.bundleFound = bundle != null;
            if (bundle != null) {
                r.bundleVersion = String.valueOf(bundle.getVersion());
                URL entry = bundle.getEntry(CLI_JAR_PATH_IN_BUNDLE);
                r.cliJarEntryFound = entry != null;
                if (entry != null) {
                    try {
                        URL fileURL = FileLocator.toFileURL(entry);
                        if (fileURL != null) {
                            File f = urlToFile(fileURL);
                            r.cliJarExtracted = f.exists();
                            r.cliJarPath = f.getAbsolutePath();
                            r.cliJarSizeBytes = f.exists() ? f.length() : -1;
                        }
                    } catch (Exception ex) {
                        r.cliLocateError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    }
                }
            }
        } catch (Exception ex) {
            r.cliLocateError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        // --- `java` availability ---
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            readStreamInto(p.getInputStream(), out);
            boolean exited = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!exited) {
                p.destroyForcibly();
                r.javaLocateError = "java -version timed out after 5s";
            } else {
                r.javaOnPath = (p.exitValue() == 0);
                String full = out.toString().trim();
                int nl = full.indexOf('\n');
                r.javaVersion = nl > 0 ? full.substring(0, nl).trim() : full;
            }
        } catch (Exception ex) {
            r.javaOnPath = false;
            r.javaLocateError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        return r;
    }

    /**
     * Print a diagnostic block to WinVMJConsole. Called automatically whenever
     * {@link #runJsonCommand} fails in a way that suggests an environment problem.
     */
    private void printEnvironmentDiagnostics(String reason) {
        WinVMJConsole.println("[PRICES CLI] Running environment diagnostics (" + reason + ")...");
        DiagnosticsReport report = diagnose();
        for (String line : report.summary().split("\n")) {
            WinVMJConsole.println("[PRICES CLI] " + line);
        }
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
     * Deploy separate frontend and backend folders to a project.
     */
    public int deploy(String projectSlug, String frontendPath, String backendPath) {
        return runCommand("deploy", "--frontend", frontendPath, "--backend", backendPath, "--project", projectSlug, "-y");
    }
    
    /**
     * Deploy a folder to a project with custom output consumer for streaming logs.
     */
    public int deploy(Consumer<String> outputConsumer, String projectSlug, String folderPath) {
        return runCommand(outputConsumer, "deploy", folderPath, "--project", projectSlug, "-y");
    }
    
    /**
     * Deploy separate frontend and backend folders with custom output consumer.
     */
    public int deploy(Consumer<String> outputConsumer, String projectSlug, String frontendPath, String backendPath) {
        return runCommand(outputConsumer, "deploy", "--frontend", frontendPath, "--backend", backendPath, "--project", projectSlug, "-y");
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
     * @param frontendPath local frontend directory path
     * @param backendPath local backend directory path
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
    public InteractiveProcess startSshDeploy(Path frontendPath, Path backendPath, String sshHost, String projectName,
                                              String frontendUrl, String backendUrl,
                                              int frontendPort, int backendPort,
                                              Consumer<String> outputConsumer,
                                              Consumer<Integer> completionCallback) {
        List<String> args = new ArrayList<>();
        args.add("deploy-ssh");
        args.add("--frontend");
        args.add(frontendPath.toAbsolutePath().toString());
        args.add("--backend");
        args.add(backendPath.toAbsolutePath().toString());
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
        
        public static CliResult parse(String rawOutput, int exitCode) {
            if (rawOutput == null || rawOutput.trim().isEmpty()) {
                return new CliResult(false, "Empty response", exitCode, null);
            }

            String json = sanitizeJsonOutput(rawOutput);
            if (json == null || json.isEmpty()) {
                // Surface the raw output so the user can see what actually came back
                String snippet = rawOutput.length() > 400
                        ? rawOutput.substring(0, 400) + "..."
                        : rawOutput;
                return new CliResult(false,
                        "CLI did not return JSON. Raw output: " + snippet,
                        exitCode, null);
            }

            try {
                // Use a lenient reader so trailing whitespace / non-strict JSON
                // (e.g. from some JVM distributions that re-wrap output) still parse
                JsonReader reader = new JsonReader(new StringReader(json));
                reader.setLenient(true);
                JsonElement root = JsonParser.parseReader(reader);

                if (root == null || !root.isJsonObject()) {
                    return new CliResult(false,
                            "Unexpected CLI response shape: " + json,
                            exitCode, null);
                }

                JsonObject obj = root.getAsJsonObject();

                boolean success = obj.has("success")
                        && !obj.get("success").isJsonNull()
                        && obj.get("success").getAsBoolean();
                String message = obj.has("message") && !obj.get("message").isJsonNull()
                        ? obj.get("message").getAsString()
                        : null;

                Map<String, Object> data = new HashMap<>();
                if (obj.has("data") && !obj.get("data").isJsonNull()) {
                    JsonElement dataElement = obj.get("data");
                    if (dataElement.isJsonArray()) {
                        data.put("_array", dataElement.toString());
                        data.put("_count",
                                String.valueOf(dataElement.getAsJsonArray().size()));
                    } else if (dataElement.isJsonObject()) {
                        JsonObject dataObj = dataElement.getAsJsonObject();
                        for (String key : dataObj.keySet()) {
                            JsonElement v = dataObj.get(key);
                            if (v == null || v.isJsonNull()) {
                                data.put(key, null);
                            } else if (v.isJsonPrimitive()) {
                                data.put(key, v.getAsString());
                            } else {
                                data.put(key, v.toString());
                            }
                        }
                    }
                }

                return new CliResult(success, message, exitCode, data);
            } catch (Exception e) {
                String snippet = json.length() > 400
                        ? json.substring(0, 400) + "..."
                        : json;
                return new CliResult(false,
                        "Failed to parse CLI JSON (" + e.getMessage() + "). Output: " + snippet,
                        exitCode, null);
            }
        }

        // ---- JSON sanitization helpers ---------------------------------------

        /** ANSI color / escape sequences. */
        private static final Pattern ANSI_ESCAPE =
                Pattern.compile("\u001B\\[[;\\d]*[ -/]*[@-~]");

        /**
         * Normalize arbitrary CLI stdout into a parseable JSON document.
         *
         * Handles:
         *  - UTF-8 BOM at start of stream
         *  - ANSI color escape codes
         *  - JVM banners printed to stdout on some distributions
         *    (e.g. "Picked up JAVA_TOOL_OPTIONS: ...", "Picked up _JAVA_OPTIONS: ...",
         *     "WARNING: ...", "Unrecognized option: ...")
         *  - Our own "[PRICES CLI]" prefixed log lines
         *  - Any leading / trailing non-JSON text around the real payload
         *
         * Returns the first balanced top-level JSON object or array found,
         * or {@code null} if nothing JSON-like is present.
         */
        static String sanitizeJsonOutput(String raw) {
            if (raw == null) return null;

            // Strip UTF-8 BOM if present
            if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
                raw = raw.substring(1);
            }

            // Strip ANSI escape sequences
            raw = ANSI_ESCAPE.matcher(raw).replaceAll("");

            // Drop obvious JVM / tooling banner lines that some environments
            // print to stdout. This is conservative: we keep any line that
            // looks JSON-related.
            StringBuilder filtered = new StringBuilder(raw.length());
            for (String line : raw.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("[PRICES CLI]")) continue;
                if (trimmed.startsWith("Picked up ")) continue;             // JAVA_TOOL_OPTIONS / _JAVA_OPTIONS
                if (trimmed.startsWith("WARNING:")) continue;                // illegal reflective access etc.
                if (trimmed.startsWith("OpenJDK ")) continue;
                if (trimmed.startsWith("Java HotSpot")) continue;
                if (trimmed.startsWith("Unrecognized option")) continue;
                if (trimmed.startsWith("Error occurred during initialization")) continue;
                filtered.append(line).append('\n');
            }
            String cleaned = filtered.toString();

            // Find the first balanced JSON object/array. This is defensive
            // against any progress output that happens to sneak through before
            // the real JSON payload.
            int startObj = cleaned.indexOf('{');
            int startArr = cleaned.indexOf('[');
            int start;
            char open, close;
            if (startObj < 0 && startArr < 0) {
                return null;
            } else if (startObj < 0) {
                start = startArr; open = '['; close = ']';
            } else if (startArr < 0) {
                start = startObj; open = '{'; close = '}';
            } else if (startObj < startArr) {
                start = startObj; open = '{'; close = '}';
            } else {
                start = startArr; open = '['; close = ']';
            }

            int depth = 0;
            boolean inString = false;
            boolean escape = false;
            for (int i = start; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                if (escape) { escape = false; continue; }
                if (c == '\\' && inString) { escape = true; continue; }
                if (c == '"') { inString = !inString; continue; }
                if (inString) continue;
                if (c == open) depth++;
                else if (c == close) {
                    depth--;
                    if (depth == 0) {
                        return cleaned.substring(start, i + 1).trim();
                    }
                }
            }

            // Unbalanced - return best effort (everything from the first brace/bracket)
            return cleaned.substring(start).trim();
        }
    }
    
    /**
     * Launch SSH deploy in external terminal (cross-platform).
     * Opens a terminal window where user can interact with SSH passphrase prompts.
     */
    public void launchSshDeployInTerminal(
            String frontendPath,
            String backendPath,
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
        cmd.append(" --frontend \"").append(frontendPath).append("\"");
        cmd.append(" --backend \"").append(backendPath).append("\"");
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
