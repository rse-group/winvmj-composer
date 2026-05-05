package id.ac.ui.cs.prices.winvmj.composer.monitoring.injector;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import id.ac.ui.cs.prices.winvmj.composer.microservicepreprocessor.JavaParserUtil;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Generates bare {Entity}RepositoryImpl proxy and injects constructor replacement.
 * The proxy extends RepositoryUtil and overrides all methods with pass-through super calls.
 * Other injectors (DbMetricsInjector, TracingInjector) modify this file via AST afterwards.
 */
public class RepositoryInjector {

    public static void inject(IFolder moduleDir, String featureName) {
        try {
            String tableName = resolveTableName(moduleDir);
            if (tableName == null) {
                WinVMJConsole.println("[RepositoryInjector] No @Table found in " + moduleDir.getName() + ", skipping");
                return;
            }

            String[] repoInfo = resolveRepositoryInfo(moduleDir);
            if (repoInfo == null) {
                WinVMJConsole.println("[RepositoryInjector] Could not resolve Repository info for " + moduleDir.getName());
                return;
            }
            String componentClassFQN = repoInfo[0];
            String repoFieldName = repoInfo[1];
            String genericType = repoInfo[2];

            String packageName = resolveImplPackageName(moduleDir);
            if (packageName == null) {
                WinVMJConsole.println("[RepositoryInjector] Could not resolve package for " + moduleDir.getName());
                return;
            }

            String simpleClassName = componentClassFQN.contains(".")
                ? componentClassFQN.substring(componentClassFQN.lastIndexOf('.') + 1)
                : componentClassFQN;
            String entityName = simpleClassName.replace("Component", "");

            WinVMJConsole.println("[RepositoryInjector] table=" + tableName + ", entity=" + entityName + ", field=" + repoFieldName);

            generateRepositoryImpl(moduleDir, packageName, featureName, tableName, entityName);
            injectConstructorReplacement(moduleDir, componentClassFQN, repoFieldName, entityName, genericType);
        } catch (CoreException e) {
            WinVMJConsole.println("[RepositoryInjector] Error: " + e.getMessage());
        }
    }

    // ========== Table Name Resolution ==========

    private static String resolveTableName(IFolder moduleDir) throws CoreException {
        return scanForTable(moduleDir);
    }

    private static String scanForTable(IFolder folder) throws CoreException {
        for (IResource resource : folder.members()) {
            if (resource instanceof IFile file && file.getName().endsWith("Impl.java")
                    && file.getFullPath().toString().contains("/model/")) {
                String table = extractTableName(file);
                if (table != null) return table;
            } else if (resource instanceof IFolder subFolder) {
                String table = scanForTable(subFolder);
                if (table != null) return table;
            }
        }
        return null;
    }

    private static String extractTableName(IFile file) {
        try {
            CompilationUnit cu = JavaParserUtil.parse(file);
            for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                Optional<AnnotationExpr> tableAnnotation = classDecl.getAnnotationByName("Table");
                if (tableAnnotation.isPresent() && tableAnnotation.get().isNormalAnnotationExpr()) {
                    NormalAnnotationExpr normal = tableAnnotation.get().asNormalAnnotationExpr();
                    for (MemberValuePair pair : normal.getPairs()) {
                        if (pair.getNameAsString().equals("name")) {
                            return pair.getValue().toString().replaceAll("^\"|\"$", "");
                        }
                    }
                }
            }
        } catch (Exception e) {
            WinVMJConsole.println("[RepositoryInjector] Error parsing " + file.getName() + ": " + e.getMessage());
        }
        return null;
    }

    // ========== Repository Info Resolution ==========

    private static String[] resolveRepositoryInfo(IFolder moduleDir) throws CoreException {
        // First: scan the module itself
        String[] result = scanForRepositoryInfo(moduleDir);
        if (result != null) return result;

        // Second: scan sibling modules that share the same SPL prefix
        // e.g. koperasi.simpanan.pokok → look in koperasi.simpanan.* siblings only
        String moduleName = moduleDir.getName();
        String[] parts = moduleName.split("\\.");
        String splPrefix = parts.length >= 2 ? parts[0] + "." + parts[1] : parts[0];

        IFolder buildFolder = (IFolder) moduleDir.getParent();
        for (IResource sibling : buildFolder.members()) {
            if (sibling instanceof IFolder siblingDir
                    && !siblingDir.getName().equals(moduleName)
                    && siblingDir.getName().startsWith(splPrefix + ".")) {
                result = scanForRepositoryInfo(siblingDir);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static String[] scanForRepositoryInfo(IFolder folder) throws CoreException {
        for (IResource resource : folder.members()) {
            if (resource instanceof IFile file && file.getName().endsWith("Component.java")
                    && (file.getName().contains("Service") || file.getName().contains("Resource"))) {
                String[] result = extractRepositoryInfo(file);
                if (result != null) return result;
            } else if (resource instanceof IFolder subFolder) {
                String[] result = scanForRepositoryInfo(subFolder);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static String[] extractRepositoryInfo(IFile file) {
        try {
            CompilationUnit cu = JavaParserUtil.parse(file);
            for (com.github.javaparser.ast.expr.AssignExpr assign : cu.findAll(com.github.javaparser.ast.expr.AssignExpr.class)) {
                if (assign.getValue() instanceof ObjectCreationExpr newExpr
                        && newExpr.getTypeAsString().startsWith("RepositoryUtil")
                        && !newExpr.getArguments().isEmpty()) {
                    String target = assign.getTarget().toString();
                    String fieldName = target.startsWith("this.") ? target.substring(5) : target;
                    String arg = newExpr.getArgument(0).toString();
                    if (arg.endsWith(".class")) {
                        // Extract generic type argument e.g. RepositoryUtil<Simpanan> → "Simpanan"
                        String genericType = "";
                        if (newExpr.getType().getTypeArguments().isPresent()
                                && !newExpr.getType().getTypeArguments().get().isEmpty()) {
                            genericType = newExpr.getType().getTypeArguments().get().get(0).toString();
                        }
                        return new String[] { arg.substring(0, arg.length() - 6), fieldName, genericType };
                    }
                }
            }
        } catch (Exception e) {
            WinVMJConsole.println("[RepositoryInjector] Error parsing " + file.getName() + ": " + e.getMessage());
        }
        return null;
    }

    // ========== Package Name Resolution ==========

    private static String resolveImplPackageName(IFolder moduleDir) throws CoreException {
        List<IFile> allFiles = new java.util.ArrayList<>();
        allFiles.addAll(AstUtils.findServiceImplFiles(moduleDir));
        allFiles.addAll(AstUtils.findResourceImplFiles(moduleDir));
        for (IFile file : allFiles) {
            try {
                CompilationUnit cu = JavaParserUtil.parse(file);
                if (cu.getPackageDeclaration().isPresent()) {
                    return cu.getPackageDeclaration().get().getNameAsString();
                }
            } catch (Exception e) { /* continue */ }
        }
        return null;
    }

    // ========== RepositoryImpl Generation (AST-based) ==========

    private static void generateRepositoryImpl(IFolder moduleDir, String packageName,
            String featureName, String tableName, String entityName) throws CoreException {
        IFolder implFolder = AstUtils.findImplFolder(moduleDir);
        if (implFolder == null) {
            WinVMJConsole.println("[RepositoryInjector] Could not find impl folder for " + packageName);
            return;
        }
        IFolder featureFolder = (IFolder) implFolder.getParent();

        IFolder targetFolder = featureFolder.getFolder("repository");
        if (!targetFolder.exists()) {
            targetFolder.create(true, true, null);
        }

        String fileName = entityName + "RepositoryImpl.java";
        IFile targetFile = targetFolder.getFile(fileName);
        if (targetFile.exists()) {
            WinVMJConsole.println("[RepositoryInjector] " + fileName + " already exists, skipping");
            return;
        }

        // Locate RepositoryUtil.java in the build folder to read method signatures dynamically
        IFile repoUtilFile = findRepositoryUtilSource((IFolder) moduleDir.getParent());
        String source;

        if (repoUtilFile != null) {
            source = buildFromSource(repoUtilFile, packageName, featureName, tableName, entityName);
            WinVMJConsole.println("[RepositoryInjector] Built " + fileName + " from RepositoryUtil source");
        } else {
            WinVMJConsole.println("[RepositoryInjector] RepositoryUtil.java not found, building minimal proxy");
            source = buildMinimalProxy(packageName, featureName, tableName, entityName);
        }

        try (InputStream stream = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))) {
            targetFile.create(stream, true, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create " + fileName, e);
        }
        WinVMJConsole.println("[RepositoryInjector] Generated " + fileName + " in " + targetFolder.getFullPath());
    }

    /**
     * Recursively search the build folder for RepositoryUtil.java.
     * vmj-libraries are unpacked into the build folder at compose time.
     */
    private static IFile findRepositoryUtilSource(IFolder buildFolder) {
        try {
            return scanForFile(buildFolder, "RepositoryUtil.java");
        } catch (CoreException e) {
            WinVMJConsole.println("[RepositoryInjector] Error scanning for RepositoryUtil: " + e.getMessage());
            return null;
        }
    }

    private static IFile scanForFile(IFolder folder, String fileName) throws CoreException {
        for (IResource resource : folder.members()) {
            if (resource instanceof IFile file && file.getName().equals(fileName)) {
                return file;
            } else if (resource instanceof IFolder subFolder) {
                IFile found = scanForFile(subFolder, fileName);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Parse RepositoryUtil.java, read all public method signatures, and generate
     * a proxy class source string. If RepositoryUtil gains or loses methods,
     * the generated proxy automatically follows.
     */
    private static String buildFromSource(IFile repoUtilFile,
            String packageName, String featureName, String tableName, String entityName) {
        CompilationUnit repoUtilCu = JavaParserUtil.parse(repoUtilFile);
        ClassOrInterfaceDeclaration repoUtilClass = repoUtilCu
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new RuntimeException("No class found in RepositoryUtil.java"));

        String className = entityName + "RepositoryImpl";

        // Collect imports from RepositoryUtil (types used in method signatures)
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import id.ac.ui.cs.prices.winvmj.hibernate.RepositoryUtil;\n");
        repoUtilCu.getImports().forEach(imp -> {
            String importName = imp.getNameAsString();
            if (!importName.startsWith("id.ac.ui.cs.prices.winvmj.hibernate")) {
                sb.append(imp).append("\n");
            }
        });
        sb.append("\n");

        // Class declaration
        sb.append("/**\n");
        sb.append(" * Bare Repository proxy — extends RepositoryUtil, overrides all public methods\n");
        sb.append(" * with pass-through to super. Monitoring injectors (DbMetrics, Tracing, etc.)\n");
        sb.append(" * will modify this file via AST to wrap method bodies with their concerns.\n");
        sb.append(" *\n");
        sb.append(" * AUTO-GENERATED from RepositoryUtil source — do not edit manually.\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append("<Y> extends RepositoryUtil<Y> {\n\n");

        // Static fields
        sb.append("    private static final String FEATURE_NAME = \"").append(featureName).append("\";\n\n");
        sb.append("    private static final String TABLE_NAME = \"").append(tableName).append("\";\n\n");

        // Constructor
        sb.append("    public ").append(className).append("(Class<? extends Y> componentClass) {\n");
        sb.append("        super(componentClass);\n");
        sb.append("    }\n");

        // Override every public method from RepositoryUtil
        for (MethodDeclaration srcMethod : repoUtilClass.getMethods()) {
            if (!srcMethod.isPublic()) continue;

            sb.append("\n    @Override\n");
            sb.append("    public ");

            // Type parameters (e.g. <Z>, <T>)
            if (!srcMethod.getTypeParameters().isEmpty()) {
                sb.append("<");
                for (int i = 0; i < srcMethod.getTypeParameters().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(srcMethod.getTypeParameters().get(i));
                }
                sb.append("> ");
            }

            // Return type + method name
            sb.append(srcMethod.getType()).append(" ").append(srcMethod.getNameAsString()).append("(");

            // Parameters
            for (int i = 0; i < srcMethod.getParameters().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(srcMethod.getParameter(i).getType()).append(" ")
                  .append(srcMethod.getParameter(i).getNameAsString());
            }
            sb.append(")");

            // Throws
            if (!srcMethod.getThrownExceptions().isEmpty()) {
                sb.append(" throws ");
                for (int i = 0; i < srcMethod.getThrownExceptions().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(srcMethod.getThrownExceptions().get(i));
                }
            }

            sb.append(" {\n");

            // Body: super call
            String args = srcMethod.getParameters().stream()
                .map(p -> p.getNameAsString())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            String superCall = "super." + srcMethod.getNameAsString() + "(" + args + ")";

            if (srcMethod.getType().isVoidType()) {
                sb.append("        ").append(superCall).append(";\n");
            } else {
                sb.append("        return ").append(superCall).append(";\n");
            }

            sb.append("    }\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Fallback: generate a minimal proxy with just the constructor.
     * Other injectors can still modify this file, but no method overrides are pre-generated.
     */
    private static String buildMinimalProxy(
            String packageName, String featureName, String tableName, String entityName) {
        String className = entityName + "RepositoryImpl";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import id.ac.ui.cs.prices.winvmj.hibernate.RepositoryUtil;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.UUID;\n");
        sb.append("import java.util.function.Consumer;\n");
        sb.append("import javax.persistence.PersistenceException;\n");
        sb.append("import org.hibernate.Session;\n\n");
        sb.append("public class ").append(className).append("<Y> extends RepositoryUtil<Y> {\n\n");
        sb.append("    private static final String FEATURE_NAME = \"").append(featureName).append("\";\n\n");
        sb.append("    private static final String TABLE_NAME = \"").append(tableName).append("\";\n\n");
        sb.append("    public ").append(className).append("(Class<? extends Y> componentClass) {\n");
        sb.append("        super(componentClass);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void saveObject(Y object) throws PersistenceException {\n");
        sb.append("        super.saveObject(object);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void updateObject(Y object) {\n");
        sb.append("        super.updateObject(object);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public Y getObject(int id) {\n");
        sb.append("        return super.getObject(id);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public Y getObject(UUID id) {\n");
        sb.append("        return super.getObject(id);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public List<Y> getAllObject(String tableName) {\n");
        sb.append("        return super.getAllObject(tableName);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public List<Y> getAllObject(String tableName, String objectName) {\n");
        sb.append("        return super.getAllObject(tableName, objectName);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void deleteObject(int id) {\n");
        sb.append("        super.deleteObject(id);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void deleteObject(UUID id) {\n");
        sb.append("        super.deleteObject(id);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public void executeQuery(Consumer<Session> action) throws PersistenceException {\n");
        sb.append("        super.executeQuery(action);\n");
        sb.append("    }\n");

        sb.append("}\n");
        return sb.toString();
    }

    // ========== Constructor Injection ==========

    private static void injectConstructorReplacement(IFolder moduleDir,
            String componentClassFQN, String repoFieldName, String entityName, String genericType) throws CoreException {
        for (IFile file : AstUtils.findServiceImplFiles(moduleDir)) {
            injectIntoImpl(file, componentClassFQN, repoFieldName, entityName, genericType);
        }
        for (IFile file : AstUtils.findResourceImplFiles(moduleDir)) {
            injectIntoImpl(file, componentClassFQN, repoFieldName, entityName, genericType);
        }
    }

    private static void injectIntoImpl(IFile file,
            String componentClassFQN, String repoFieldName, String entityName, String genericType) {
        CompilationUnit cu = JavaParserUtil.parse(file);

        String source = cu.toString();
        if (!source.contains(repoFieldName)) {
            WinVMJConsole.println("[RepositoryInjector] " + file.getName() + " does not use field '" + repoFieldName + "', skipping");
            return;
        }

        String className = entityName + "RepositoryImpl";
        String typeArg = (genericType != null && !genericType.isEmpty()) ? "<" + genericType + ">" : "<>";
        String stmt = "this." + repoFieldName + " = new " + className + typeArg + "("
            + componentClassFQN + ".class);";

        // Add import for the generic type (e.g. Simpanan) if it's not fully qualified
        if (genericType != null && !genericType.isEmpty() && !genericType.contains(".")) {
            String componentPackage = componentClassFQN.substring(0, componentClassFQN.lastIndexOf('.'));
            String genericTypeFQN = componentPackage + "." + genericType;
            AstUtils.addImports(cu, List.of(genericTypeFQN));
        }

        AstUtils.addConstructorInit(cu, new String[] { stmt }, className);
        AstUtils.overwriteFile(file, cu);

        WinVMJConsole.println("[RepositoryInjector] Injected " + className + " in " + file.getName());
    }
}
