package id.ac.ui.cs.prices.winvmj.composer.monitoring.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

import id.ac.ui.cs.prices.winvmj.composer.microservicepreprocessor.JavaParserUtil;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import java.util.List;

/**
 * Injects SLF4J logging into ServiceImpl, ResourceImpl, and RepositoryImpl files.
 * Logs method entry (args), exit (duration), and error (exception + duration).
 * For RepositoryImpl, logs DB operation + table name.
 */
public class LoggingInjector {

    private static final List<String> IMPORTS = List.of(
        "org.slf4j.Logger",
        "org.slf4j.LoggerFactory"
    );

    private static final String FIELD_SENTINEL = "_logLogger";

    public static void inject(IFolder moduleDir, String featureName) {
        try {
            IFile moduleInfo = moduleDir.getFile("module-info.java");
            if (moduleInfo.exists()) {
                AstUtils.addModuleRequires(moduleInfo, List.of("org.slf4j"));
            }

            for (IFile file : AstUtils.findServiceImplFiles(moduleDir)) {
                processImplFile(file, featureName);
            }

            for (IFile file : AstUtils.findResourceImplFiles(moduleDir)) {
                processImplFile(file, featureName);
            }

            for (IFile file : AstUtils.findRepositoryImplFiles(moduleDir)) {
                processImplFile(file, featureName);
            }
        } catch (CoreException e) {
            WinVMJConsole.println("[LoggingInjector] Error scanning " + moduleDir.getName() + ": " + e.getMessage());
        }
    }

    // ========== ServiceImpl / ResourceImpl ==========

    private static void processImplFile(IFile file, String featureName) {
        WinVMJConsole.println("[LoggingInjector] Processing " + file.getFullPath());

        CompilationUnit cu = JavaParserUtil.parse(file);

        AstUtils.addImports(cu, IMPORTS);

        String className = cu.findFirst(ClassOrInterfaceDeclaration.class)
            .map(ClassOrInterfaceDeclaration::getNameAsString)
            .orElse("Unknown");

        addLoggerField(cu, className);

        int count = 0;
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (method.getBody().isEmpty()) continue;
            wrapImplMethod(method, featureName, className);
            count++;
        }

        if (count > 0) {
            AstUtils.overwriteFile(file, cu);
            WinVMJConsole.println("[LoggingInjector] Wrapped " + count + " methods in " + file.getName());
        }
    }

    private static void wrapImplMethod(MethodDeclaration method, String featureName, String className) {
        String methodName = method.getNameAsString();
        String fullMethodName = className + "." + methodName;

        NodeList<Statement> originalStmts = new NodeList<>(method.getBody().get().getStatements());

        BlockStmt newBody = new BlockStmt();

        // Log entry: each parameter
        List<Parameter> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            newBody.addStatement(StaticJavaParser.parseStatement(
                "_logLogger.info(\"[" + featureName + "][" + fullMethodName + "] arg[" + i + "]: {}\", "
                    + params.get(i).getNameAsString() + ");"));
        }

        newBody.addStatement(StaticJavaParser.parseStatement(
            "long _logStart = System.currentTimeMillis();"));

        BlockStmt tryBlock = new BlockStmt();
        originalStmts.forEach(tryBlock::addStatement);

        BlockStmt catchBlock = new BlockStmt();
        catchBlock.addStatement(StaticJavaParser.parseStatement(
            "long _logDuration = System.currentTimeMillis() - _logStart;"));
        catchBlock.addStatement(StaticJavaParser.parseStatement(
            "_logLogger.error(\"[" + featureName + "][ERROR] " + fullMethodName
                + " threw {} after {}ms: {}\", _logT.getClass().getSimpleName(), _logDuration, _logT.getMessage());"));
        catchBlock.addStatement(StaticJavaParser.parseStatement("throw _logT;"));

        CatchClause catchClause = new CatchClause(
            new Parameter(StaticJavaParser.parseType("Throwable"), "_logT"), catchBlock);

        BlockStmt finallyBlock = new BlockStmt();
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "long _logDur = System.currentTimeMillis() - _logStart;"));
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "_logLogger.info(\"[" + featureName + "] " + fullMethodName + " completed in {}ms\", _logDur);"));

        newBody.addStatement(new TryStmt(tryBlock, new NodeList<>(catchClause), finallyBlock));
        method.setBody(newBody);
    }

    // ========== Logger Field ==========

    /**
     * Add Logger field to class. Uses class-specific LoggerFactory.
     * Idempotent — checks for _logLogger sentinel.
     */
    private static void addLoggerField(CompilationUnit cu, String className) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
            boolean alreadyInjected = classDecl.getFields().stream()
                .anyMatch(f -> f.getVariables().stream()
                    .anyMatch(v -> v.getNameAsString().equals(FIELD_SENTINEL)));
            if (alreadyInjected) return;

            classDecl.getMembers().add(0,
                StaticJavaParser.parseBodyDeclaration(
                    "private static final Logger " + FIELD_SENTINEL
                        + " = LoggerFactory.getLogger(" + className + ".class);")
                    .asFieldDeclaration());
        });
    }
}
