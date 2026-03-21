package id.ac.ui.cs.prices.winvmj.composer.monitoring.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared AST utilities for all monitoring injectors.
 */
public class AstUtils {

    public static void addImports(CompilationUnit cu, List<String> requiredImports) {
        requiredImports.forEach(importStr -> {
            boolean isAsterisk = importStr.endsWith(".*");
            String importName = isAsterisk ? importStr.substring(0, importStr.length() - 2) : importStr;
            ImportDeclaration importDecl = new ImportDeclaration(importName, false, isAsterisk);
            boolean exists = cu.getImports().stream().anyMatch(existing ->
                existing.getNameAsString().equals(importName) && existing.isAsterisk() == isAsterisk);
            if (!exists) cu.addImport(importDecl);
        });
    }

    /**
     * Add instance field declarations to the top of the class.
     * Idempotent — checks for sentinel field name before injecting.
     */
    public static void addInstanceFields(CompilationUnit cu, String[] declarations, String sentinelName) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
            boolean alreadyInjected = classDecl.getFields().stream()
                .anyMatch(f -> f.getVariables().stream()
                    .anyMatch(v -> v.getNameAsString().equals(sentinelName)));
            if (alreadyInjected) return;

            for (int i = declarations.length - 1; i >= 0; i--) {
                FieldDeclaration field = StaticJavaParser.parseBodyDeclaration(declarations[i])
                    .asFieldDeclaration();
                classDecl.getMembers().add(0, field);
            }
        });
    }

    /**
     * Inject statements at the end of the first constructor body.
     * If no constructor exists, creates a default no-arg constructor.
     * Idempotent — checks for sentinel statement substring before injecting.
     */
    public static void addConstructorInit(CompilationUnit cu, String[] statements, String sentinelSubstring) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
            ConstructorDeclaration ctor = classDecl.findFirst(ConstructorDeclaration.class)
                .orElseGet(() -> classDecl.addConstructor(com.github.javaparser.ast.Modifier.Keyword.PUBLIC));

            BlockStmt body = ctor.getBody();
            boolean alreadyInjected = body.getStatements().stream()
                .anyMatch(s -> s.toString().contains(sentinelSubstring));
            if (alreadyInjected) return;

            for (String stmt : statements) {
                body.addStatement(StaticJavaParser.parseStatement(stmt));
            }
        });
    }

    public static void overwriteFile(IFile file, CompilationUnit cu) {
        try (InputStream stream = new ByteArrayInputStream(cu.toString().getBytes(StandardCharsets.UTF_8))) {
            if (file.exists()) {
                file.setContents(stream, true, true, null);
            } else {
                file.create(stream, true, null);
            }
        } catch (CoreException | IOException e) {
            throw new RuntimeException("Failed to write to file: " + file.getFullPath(), e);
        }
    }

    /**
     * Add 'requires' directives to a module-info.java file via text manipulation.
     * Idempotent — skips modules already required.
     */
    public static void addModuleRequires(IFile moduleInfoFile, List<String> requiredModules) {
        try {
            String content;
            try (InputStream is = moduleInfoFile.getContents()) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            StringBuilder toInsert = new StringBuilder();
            for (String mod : requiredModules) {
                if (!content.contains("requires " + mod + ";")) {
                    toInsert.append("    requires ").append(mod).append(";\n");
                }
            }
            if (toInsert.length() == 0) return;

            // Insert before the closing brace
            int lastBrace = content.lastIndexOf('}');
            if (lastBrace == -1) return;

            String newContent = content.substring(0, lastBrace)
                + toInsert
                + content.substring(lastBrace);

            try (InputStream stream = new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8))) {
                moduleInfoFile.setContents(stream, true, true, null);
            }
        } catch (CoreException | IOException e) {
            throw new RuntimeException("Failed to update module-info: " + moduleInfoFile.getFullPath(), e);
        }
    }

    // ========== File Discovery Utilities ==========

    /**
     * Recursively find all files in a module directory matching any of the given suffixes.
     */
    public static List<IFile> findFiles(IFolder folder, String... suffixes) throws CoreException {
        List<IFile> result = new ArrayList<>();
        scanFiles(folder, result, suffixes);
        return result;
    }

    private static void scanFiles(IFolder folder, List<IFile> result, String... suffixes) throws CoreException {
        for (IResource resource : folder.members()) {
            if (resource instanceof IFile file) {
                for (String suffix : suffixes) {
                    if (file.getName().endsWith(suffix)) {
                        result.add(file);
                        break;
                    }
                }
            } else if (resource instanceof IFolder subFolder) {
                scanFiles(subFolder, result, suffixes);
            }
        }
    }

    /**
     * Find all ServiceImpl and ResourceImpl files in a module directory.
     */
    public static List<IFile> findImplFiles(IFolder moduleDir) throws CoreException {
        return findFiles(moduleDir, "ServiceImpl.java", "ResourceImpl.java");
    }

    /**
     * Find all ResourceImpl files in a module directory.
     */
    public static List<IFile> findResourceImplFiles(IFolder moduleDir) throws CoreException {
        return findFiles(moduleDir, "ResourceImpl.java");
    }

    /**
     * Find all RepositoryImpl files (generated proxy) in a module directory.
     */
    public static List<IFile> findRepositoryImplFiles(IFolder moduleDir) throws CoreException {
        return findFiles(moduleDir, "RepositoryImpl.java");
    }

    /**
     * Find the folder containing ServiceImpl or ResourceImpl.
     * Prefers service/ over resource/ if both exist.
     */
    public static IFolder findImplFolder(IFolder folder) throws CoreException {
        IFolder resourceFolder = null;
        for (IResource resource : folder.members()) {
            if (resource instanceof IFile file) {
                if (file.getName().endsWith("ServiceImpl.java")) {
                    return (IFolder) file.getParent();
                } else if (file.getName().endsWith("ResourceImpl.java")) {
                    resourceFolder = (IFolder) file.getParent();
                }
            } else if (resource instanceof IFolder subFolder) {
                IFolder found = findImplFolder(subFolder);
                if (found != null) {
                    if (found.getFullPath().toString().contains("/service/")) return found;
                    if (resourceFolder == null) resourceFolder = found;
                }
            }
        }
        return resourceFolder;
    }
}
