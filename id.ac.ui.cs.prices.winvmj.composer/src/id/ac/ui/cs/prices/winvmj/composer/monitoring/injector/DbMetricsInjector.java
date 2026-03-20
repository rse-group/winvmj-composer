package id.ac.ui.cs.prices.winvmj.composer.monitoring.injector;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import id.ac.ui.cs.prices.winvmj.composer.microservicepreprocessor.JavaParserUtil;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        IFolder implFolder = AstUtils.findImplFolder(moduleDir);
        if (implFolder == null) {
            WinVMJConsole.println("[DbMetricsInjector] Could not find impl folder for " + packageName);
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
            WinVMJConsole.println("[DbMetricsInjector] " + fileName + " already exists, skipping");
            return;
        }

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", packageName);
        dataModel.put("entityName", entityName);
        dataModel.put("featureName", featureName);
        dataModel.put("tableName", tableName);

        try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setClassForTemplateLoading(DbMetricsInjector.class, "/templates");
            Template template = cfg.getTemplate("RepositoryImpl.ftl");

            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);

            try (InputStream stream = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8))) {
                targetFile.create(stream, true, null);
            }
        } catch (IOException | TemplateException e) {
            throw new RuntimeException("Failed to create " + fileName, e);
        }
        WinVMJConsole.println("[DbMetricsInjector] Generated " + fileName + " in " + targetFolder.getFullPath());
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

        // Only inject if this class (or its parent chain) actually uses the Repository field.
        // Check: does the file contain "this.<repoFieldName>" or "new RepositoryUtil" usage?
        String source = cu.toString();
        if (!source.contains(repoFieldName)) {
            WinVMJConsole.println("[DbMetricsInjector] " + file.getName() + " does not use field '" + repoFieldName + "', skipping");
            return;
        }

        String className = entityName + "RepositoryImpl";
        String stmt = "this." + repoFieldName + " = new " + className + "<>("
            + componentClassFQN + ".class);";

        AstUtils.addConstructorInit(cu, new String[] { stmt }, className);
        AstUtils.overwriteFile(file, cu);

        WinVMJConsole.println("[DbMetricsInjector] Injected " + className + " in " + file.getName());
    }
}
