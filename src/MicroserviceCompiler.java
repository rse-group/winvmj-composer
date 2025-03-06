import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class MicroserviceCompiler {
    private static final String MODULES_DIR = "modules";
    private static final String RESOURCES_DIR = "resources";

    private static final Path WINVMJ_LIB_DIR = Path.of( "winvmj-libraries");
    private static final Path SERVICE_DEFINITION_PATH = Path.of(RESOURCES_DIR, "services-def.json");

    private static final String OUT_DIR = "src-gen";

    private static final JsonParser jsonParser = new JsonParser();

    private static Map<String, JsonObject> featureMappings = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Load JSON
        String serviceJson = Files.readString(SERVICE_DEFINITION_PATH);

        JsonObject serviceObj = jsonParser.parse(serviceJson).getAsJsonObject();
        Set<String> compiledModules = new HashSet<>();

        for (JsonElement serviceEl : serviceObj.get("services").getAsJsonArray()) {
            JsonObject service = serviceEl.getAsJsonObject();
            String productName = service.get("productName").getAsString();

            System.out.println(" ");
            System.out.println("======== " + " Start Generating " + productName + " ========");

            // Get required modules
            Set<String> modules = new LinkedHashSet<>();
            for (JsonElement featureEl : service.get("features").getAsJsonArray()) {
                JsonObject feature = featureEl.getAsJsonObject();

                String splName = feature.get("spl").getAsString();
                String featureName = feature.get("feature").getAsString();

                JsonArray featureModules = getModules(splName, featureName);
                for (JsonElement mod : featureModules) {
                    Set<String> dependencies = getModuleDependencies(mod.getAsString(), new HashSet<>());
                    modules.addAll(dependencies);
                    modules.add(mod.getAsString());
                }
            }
            System.out.println("Modules: " + modules);

            Path compiledProductDir = Path.of(OUT_DIR, productName);

            // Import WinVMJ
            System.out.println("Import WinVMJ Libraries ...");
            copyWinVMJLib(compiledProductDir);

            // Compile and Package Modules
            for (String module : modules) {
                // Compile Modules
                if (!compiledModules.contains(module)) {
                    List<String> compileCmd = constructCompileCommand(module,compiledProductDir.toString());
                    executeCommand(compileCmd);
                    compiledModules.add(module);
                }

                // Create JAR
                List<String> jarCmd = constructJARCommand(module, module, compiledProductDir.toString(), "");
                executeCommand(jarCmd);
            }

            String productModule = service.get("productModule").getAsString();;

            // Compile Service/Product
            List<String> compileCmd = constructCompileCommand(productModule,compiledProductDir.toString());
            executeCommand(compileCmd);

            // Create final Product JAR with MainClass
            String mainClass = productModule + "." + productName;
            List<String> finalJarCmd = constructJARCommand(productModule, productName, compiledProductDir.toString(), mainClass);
            executeCommand(finalJarCmd);

            System.out.println("======== " + " Finished Generating " + productName + " ========");
        }
        System.out.println("Cleaning up ...");
        cleanBinaries();
        System.out.println("======== " + " Finished Generating All Services" + " ========");
    }

    private static JsonArray getModules(String splName, String featureName) throws IllegalArgumentException {
        if (!featureMappings.containsKey(splName)) {
            extractFeatureToModuleMapping(splName);
        }

        if (!featureMappings.get(splName).has(featureName)) {
            throw new IllegalArgumentException("Feature '" + featureName + "' not found in SPL '" + splName + "'");
        }

        return featureMappings.get(splName).get(featureName).getAsJsonArray();
    }


    private static void extractFeatureToModuleMapping(String splName) {
        try {
            String mappingJson = Files.readString(Path.of(RESOURCES_DIR, splName, "feature_to_module.json"));

            featureMappings.put(splName, jsonParser.parse(mappingJson).getAsJsonObject());
        } catch (IOException e) {
            throw new RuntimeException("Error reading feature-to-module mapping for " + splName + ": " + e.getMessage());
        }
    }

    private static Set<String> getModuleDependencies(String moduleName, Set<String> visited) throws IOException {
        if (visited.contains(moduleName)) {
            return new HashSet<>();
        }

        visited.add(moduleName);

        Set<String> dependencies = new LinkedHashSet<>();
        Path moduleInfoPath = Path.of(MODULES_DIR, moduleName, "module-info.java");

        if (Files.exists(moduleInfoPath)) {
            List<String> lines = Files.readAllLines(moduleInfoPath);
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("requires vmj")) {
                    break;
                } else if (line.startsWith("requires")) {
                    String dependency = line.split(" ")[1].replace(";", "");
                    if (!visited.contains(dependency)) {
                        dependencies.addAll(getModuleDependencies(dependency, visited));
                        dependencies.add(dependency);
                    }
                }
            }
        }
        return dependencies;
    }

    private static void copyWinVMJLib(Path targetDir){
        if (Files.notExists(targetDir)) {
            try {
                Files.createDirectories(targetDir);
            } catch (IOException e){
                throw new RuntimeException("Failed to create target directory: " + targetDir, e);
            }
        }

        // Use try-with-resources to make Stream<Path> automatically closed.
        try (Stream<Path> paths = Files.walk(WINVMJ_LIB_DIR)) {
            paths.filter(path -> path.toString().endsWith(".jar"))
                    .forEach(sourcePath -> {
                        Path destination = targetDir.resolve(WINVMJ_LIB_DIR.relativize(sourcePath));
                        try {
                            Files.copy(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to copy: " + sourcePath, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Error while traversing source directory: " + WINVMJ_LIB_DIR, e);
        }

    }

    private static List<String> constructCompileCommand(String module, String compiledProductDir) {
        String modulePath = Path.of(MODULES_DIR ,module).toString();

        List<String> javaFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Path.of(modulePath))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> javaFiles.add(path.toString()));
        } catch (IOException e) {
            throw new RuntimeException("Error while traversing module path: " + modulePath, e);
        }

        if (javaFiles.isEmpty()) {
            throw new RuntimeException("No Java files found in module path: " + modulePath);
        }

        Path binPath = Path.of("bin", module);
        if (Files.notExists(binPath)) {
            try {
                Files.createDirectories(binPath);
                System.out.println("Created directory: " + binPath.toAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to create output directory: " + binPath, e);
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("javac");
        cmd.add("-d");
        cmd.add(binPath.toString());
        cmd.add("--module-path");
        cmd.add(compiledProductDir);
        cmd.addAll(javaFiles);
        return cmd;
    }

    private static List<String> constructJARCommand(String modulePath, String jarName, String compiledProductDir, String mainClass) {
        Path binPath = Path.of("bin", modulePath);

        String jarPath = Path.of(compiledProductDir, jarName + ".jar").toString() ;

        List<String> cmd = new ArrayList<>();
        cmd.add("jar");
        cmd.add("--create");
        cmd.add("--file");
        cmd.add(jarPath);
        if (!mainClass.isEmpty()) {
            cmd.add("--main-class");
            cmd.add(mainClass);
        }
        cmd.add("-C");
        cmd.add(binPath.toString());
        cmd.add(".");
        return cmd;
    }

    private static void executeCommand(List<String> command) throws IOException, InterruptedException {
        System.out.println("Executing command: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode);
        }
    }

    private static void cleanBinaries() {
        try (Stream<Path> walk = Files.walk(Path.of("bin"))) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException("Error while cleaning bin directory : ", e);
        }
    }
}
