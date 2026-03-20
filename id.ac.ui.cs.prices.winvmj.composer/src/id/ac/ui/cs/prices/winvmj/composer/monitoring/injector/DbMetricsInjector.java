package id.ac.ui.cs.prices.winvmj.composer.monitoring.injector;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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
 * Injects DB metrics via generated RepositoryImpl proxy.
 * 1. Scans model/ for @Table(name=...) to resolve table name
 * 2. Generates {Entity}RepositoryImpl.java in the module's repository/ folder
 * 3. Modifies ServiceImpl/ResourceImpl constructor to replace Repository with instrumented proxy
 */
public class DbMetricsInjector {

    public static void inject(IFolder moduleDir, String featureName) {
        try {
            IFile moduleInfo = moduleDir.getFile("module-info.java");
            if (moduleInfo.exists()) {
                AstUtils.addModuleRequires(moduleInfo, List.of("io.opentelemetry.api"));
            }

            String tableName = resolveTableName(moduleDir);
            if (tableName == null) {
                WinVMJConsole.println("[DbMetricsInjector] No @Table found in " + moduleDir.getName() + ", skipping");
                return;
            }
            WinVMJConsole.println("[DbMetricsInjector] Resolved table: " + tableName + " for feature: " + featureName);

            String[] repoInfo = resolveRepositoryInfo(moduleDir);
            if (repoInfo == null) {
                WinVMJConsole.println("[DbMetricsInjector] Could not resolve Repository info for " + moduleDir.getName());
                return;
            }
            String componentClassFQN = repoInfo[0];
            String repoFieldName = repoInfo[1];
            WinVMJConsole.println("[DbMetricsInjector] Resolved componentClass: " + componentClassFQN + ", field: " + repoFieldName);

            String packageName = resolveImplPackageName(moduleDir);
            if (packageName == null) {
                WinVMJConsole.println("[DbMetricsInjector] Could not resolve package for " + moduleDir.getName());
                return;
            }

            // Resolve entity name from Component class: "accountpl.account.core.AccountComponent" → "Account"
            String simpleClassName = componentClassFQN.contains(".")
                ? componentClassFQN.substring(componentClassFQN.lastIndexOf('.') + 1)
                : componentClassFQN;
            String entityName = simpleClassName.replace("Component", "");

            generateRepositoryImpl(moduleDir, packageName, featureName, tableName, entityName);
            injectConstructorReplacement(moduleDir, componentClassFQN, repoFieldName, entityName);
        } catch (CoreException e) {
            WinVMJConsole.println("[DbMetricsInjector] Error: " + e.getMessage());
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
            WinVMJConsole.println("[DbMetricsInjector] Error parsing " + file.getName() + ": " + e.getMessage());
        }
        return null;
    }

    // ========== Repository Info Resolution (componentClass + field name) ==========

    /**
     * Resolve componentClass and Repository field name from the Component class.
     * Scans sibling modules for *ServiceComponent.java or *ResourceComponent.java
     * containing: this.X = new RepositoryUtil<...>(Y.class)
     * @return [componentClassFQN, fieldName] or null
     */
    private static String[] resolveRepositoryInfo(IFolder moduleDir) throws CoreException {
        IFolder buildFolder = (IFolder) moduleDir.getParent();
        for (IResource sibling : buildFolder.members()) {
            if (sibling instanceof IFolder siblingDir) {
                String[] result = scanForRepositoryInfo(siblingDir);
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

    /**
     * Extract from assignment: this.SomeField = new RepositoryUtil<X>(Y.class);
     * @return [componentClassFQN, fieldName] e.g. ["accountpl.account.core.AccountComponent", "Repository"]
     */
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
                        return new String[] { arg.substring(0, arg.length() - 6), fieldName };
                    }
                }
            }
        } catch (Exception e) {
            WinVMJConsole.println("[DbMetricsInjector] Error parsing " + file.getName() + ": " + e.getMessage());
        }
        return null;
    }

    // ========== Package Name Resolution ==========

    /**
     * Resolve the package name from ServiceImpl/ResourceImpl.
     * All files in a module share the same package (folder structure ≠ package).
     */
    private static String resolveImplPackageName(IFolder moduleDir) throws CoreException {
        for (IFile file : AstUtils.findImplFiles(moduleDir)) {
            try {
                CompilationUnit cu = JavaParserUtil.parse(file);
                if (cu.getPackageDeclaration().isPresent()) {
                    return cu.getPackageDeclaration().get().getNameAsString();
                }
            } catch (Exception e) { /* continue */ }
        }
        return null;
    }

    // ========== RepositoryImpl Generation ==========

    private static void generateRepositoryImpl(IFolder moduleDir, String packageName,
            String featureName, String tableName, String entityName) throws CoreException {
        // Find the feature-level folder (e.g. .../accountpl/account/overdraft/)
        // by going to parent of any impl folder (service/ or resource/)
        IFolder implFolder = AstUtils.findImplFolder(moduleDir);
        if (implFolder == null) {
            WinVMJConsole.println("[DbMetricsInjector] Could not find impl folder for " + packageName);
            return;
        }
        IFolder featureFolder = (IFolder) implFolder.getParent();

        // Place in repository/ subfolder
        IFolder targetFolder = featureFolder.getFolder("repository");
        if (!targetFolder.exists()) {
            targetFolder.create(true, true, null);
        }

        String fileName = entityName + "RepositoryImpl.java";
        IFile targetFile = targetFolder.getFile(fileName);
        if (targetFile.exists()) {
            WinVMJConsole.println("[DbMetricsInjector] " + fileName + " already exists, skipping");
            return;
        }

        String source = generateRepoImplSource(packageName, featureName, tableName, entityName);
        try (InputStream stream = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))) {
            targetFile.create(stream, true, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create " + fileName, e);
        }
        WinVMJConsole.println("[DbMetricsInjector] Generated " + fileName + " in " + targetFolder.getFullPath());
    }

    private static String generateRepoImplSource(String packageName, String featureName, String tableName, String entityName) {
        String className = entityName + "RepositoryImpl";
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import id.ac.ui.cs.prices.winvmj.hibernate.RepositoryUtil;\n");
        sb.append("import io.opentelemetry.api.GlobalOpenTelemetry;\n");
        sb.append("import io.opentelemetry.api.metrics.Meter;\n");
        sb.append("import io.opentelemetry.api.metrics.LongCounter;\n");
        sb.append("import io.opentelemetry.api.metrics.LongHistogram;\n");
        sb.append("import io.opentelemetry.api.common.Attributes;\n");
        sb.append("import io.opentelemetry.api.common.AttributeKey;\n\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.UUID;\n");
        sb.append("import java.util.function.Consumer;\n");
        sb.append("import javax.persistence.PersistenceException;\n");
        sb.append("import org.hibernate.Session;\n\n");
        sb.append("public class ").append(className).append("<Y> extends RepositoryUtil<Y> {\n\n");
        sb.append("    private static final String FEATURE_NAME = \"").append(featureName).append("\";\n");
        sb.append("    private static final String TABLE_NAME = \"").append(tableName).append("\";\n");
        sb.append("    private final LongCounter dbQueryCounter;\n");
        sb.append("    private final LongCounter dbQueryErrorCounter;\n");
        sb.append("    private final LongHistogram dbQueryDurationHistogram;\n\n");
        sb.append("    public ").append(className).append("(Class<? extends Y> componentClass) {\n");
        sb.append("        super(componentClass);\n");
        sb.append("        Meter meter = GlobalOpenTelemetry.get().getMeter(\"monitoring\");\n");
        sb.append("        this.dbQueryCounter = meter.counterBuilder(\"db_queries_total\")\n");
        sb.append("            .setDescription(\"Total number of database queries\").build();\n");
        sb.append("        this.dbQueryErrorCounter = meter.counterBuilder(\"db_query_errors_total\")\n");
        sb.append("            .setDescription(\"Total number of failed database queries\").build();\n");
        sb.append("        this.dbQueryDurationHistogram = meter.histogramBuilder(\"db_query_duration_ms\")\n");
        sb.append("            .setDescription(\"Database query duration in milliseconds\").ofLongs().build();\n");
        sb.append("    }\n\n");
        sb.append("    private Attributes attrs(String operation) {\n");
        sb.append("        return Attributes.of(\n");
        sb.append("            AttributeKey.stringKey(\"feature\"), FEATURE_NAME,\n");
        sb.append("            AttributeKey.stringKey(\"db.operation\"), operation,\n");
        sb.append("            AttributeKey.stringKey(\"db.table\"), TABLE_NAME);\n");
        sb.append("    }\n\n");
        sb.append(voidOverride("saveObject", "Y object", "object", "INSERT", "PersistenceException"));
        sb.append(voidOverride("updateObject", "Y object", "object", "UPDATE", null));
        sb.append(returnOverride("getObject", "int id", "id", "Y", "SELECT"));
        sb.append(returnOverride("getObject", "UUID id", "id", "Y", "SELECT"));
        sb.append(returnOverride("getAllObject", "String tableName", "tableName", "List<Y>", "SELECT"));
        sb.append(returnOverride("getAllObject", "String tableName, String objectName", "tableName, objectName", "List<Y>", "SELECT"));
        sb.append(voidOverride("deleteObject", "int id", "id", "DELETE", null));
        sb.append(voidOverride("deleteObject", "UUID id", "id", "DELETE", null));
        sb.append(voidOverride("executeQuery", "Consumer<Session> action", "action", "EXECUTE", "PersistenceException"));
        sb.append("}\n");
        return sb.toString();
    }

    private static String voidOverride(String method, String params, String args, String op, String throwsClause) {
        StringBuilder sb = new StringBuilder();
        sb.append("    @Override\n");
        sb.append("    public void ").append(method).append("(").append(params).append(")");
        if (throwsClause != null) sb.append(" throws ").append(throwsClause);
        sb.append(" {\n");
        sb.append("        long start = System.currentTimeMillis();\n");
        sb.append("        try {\n");
        sb.append("            super.").append(method).append("(").append(args).append(");\n");
        sb.append("            dbQueryCounter.add(1, attrs(\"").append(op).append("\"));\n");
        sb.append("        } catch (Throwable t) {\n");
        sb.append("            dbQueryErrorCounter.add(1, attrs(\"").append(op).append("\"));\n");
        sb.append("            throw t;\n");
        sb.append("        } finally {\n");
        sb.append("            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs(\"").append(op).append("\"));\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        return sb.toString();
    }

    private static String returnOverride(String method, String params, String args, String retType, String op) {
        StringBuilder sb = new StringBuilder();
        sb.append("    @Override\n");
        sb.append("    public ").append(retType).append(" ").append(method).append("(").append(params).append(") {\n");
        sb.append("        long start = System.currentTimeMillis();\n");
        sb.append("        try {\n");
        sb.append("            ").append(retType).append(" result = super.").append(method).append("(").append(args).append(");\n");
        sb.append("            dbQueryCounter.add(1, attrs(\"").append(op).append("\"));\n");
        sb.append("            return result;\n");
        sb.append("        } catch (Throwable t) {\n");
        sb.append("            dbQueryErrorCounter.add(1, attrs(\"").append(op).append("\"));\n");
        sb.append("            throw t;\n");
        sb.append("        } finally {\n");
        sb.append("            dbQueryDurationHistogram.record(System.currentTimeMillis() - start, attrs(\"").append(op).append("\"));\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        return sb.toString();
    }

    // ========== Constructor Injection ==========

    /**
     * Inject RepositoryImpl replacement into ServiceImpl/ResourceImpl constructors.
     * Uses AstUtils.addConstructorInit for find-or-create + idempotency.
     */
    private static void injectConstructorReplacement(IFolder moduleDir,
            String componentClassFQN, String repoFieldName, String entityName) throws CoreException {
        for (IFile file : AstUtils.findImplFiles(moduleDir)) {
            injectIntoImpl(file, componentClassFQN, repoFieldName, entityName);
        }
    }

    private static void injectIntoImpl(IFile file,
            String componentClassFQN, String repoFieldName, String entityName) {
        WinVMJConsole.println("[DbMetricsInjector] Processing " + file.getFullPath());

        CompilationUnit cu = JavaParserUtil.parse(file);

        String className = entityName + "RepositoryImpl";
        String stmt = "this." + repoFieldName + " = new " + className + "<>("
            + componentClassFQN + ".class);";

        AstUtils.addConstructorInit(cu, new String[] { stmt }, className);
        AstUtils.overwriteFile(file, cu);

        WinVMJConsole.println("[DbMetricsInjector] Injected " + className + " in " + file.getName());
    }
}
