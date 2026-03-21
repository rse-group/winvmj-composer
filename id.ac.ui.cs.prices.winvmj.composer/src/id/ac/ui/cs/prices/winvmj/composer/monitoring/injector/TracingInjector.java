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
import java.util.Map;
import java.util.Optional;

/**
 * Injects distributed tracing spans into ServiceImpl, ResourceImpl, and RepositoryImpl files.
 * SpanKind: SERVER for Resource, INTERNAL for Service, CLIENT for Repository (DB).
 * Parent-child automatic via OTel SDK: Span.current() as parent, span.makeCurrent() via Scope.
 */
public class TracingInjector {

    private static final List<String> IMPORTS = List.of(
        "io.opentelemetry.api.GlobalOpenTelemetry",
        "io.opentelemetry.api.trace.Tracer",
        "io.opentelemetry.api.trace.Span",
        "io.opentelemetry.api.trace.SpanKind",
        "io.opentelemetry.api.trace.StatusCode",
        "io.opentelemetry.context.Scope"
    );

    private static final String[] FIELD_DECLARATIONS = {
        "private Tracer _tracer;"
    };

    private static final String[] CONSTRUCTOR_INIT = {
        "_tracer = GlobalOpenTelemetry.get().getTracer(\"monitoring\");"
    };

    /**
     * Map RepositoryImpl method name → DB operation for span naming.
     */
    private static final Map<String, String> METHOD_TO_OPERATION = Map.of(
        "saveObject", "INSERT",
        "updateObject", "UPDATE",
        "getObject", "SELECT",
        "getAllObject", "SELECT",
        "deleteObject", "DELETE",
        "executeQuery", "EXECUTE"
    );

    public static void inject(IFolder moduleDir, String featureName) {
        try {
            IFile moduleInfo = moduleDir.getFile("module-info.java");
            if (moduleInfo.exists()) {
                AstUtils.addModuleRequires(moduleInfo, List.of(
                    "io.opentelemetry.api", "io.opentelemetry.context"));
            }

            // Wrap ServiceImpl + ResourceImpl methods
            for (IFile file : AstUtils.findImplFiles(moduleDir)) {
                processImplFile(file, featureName);
            }

            // Wrap RepositoryImpl methods (SpanKind.CLIENT for DB)
            for (IFile file : AstUtils.findRepositoryImplFiles(moduleDir)) {
                processRepositoryFile(file, featureName);
            }
        } catch (CoreException e) {
            WinVMJConsole.println("[TracingInjector] Error scanning " + moduleDir.getName() + ": " + e.getMessage());
        }
    }

    // ========== ServiceImpl / ResourceImpl ==========

    private static void processImplFile(IFile file, String featureName) {
        WinVMJConsole.println("[TracingInjector] Processing " + file.getFullPath());

        CompilationUnit cu = JavaParserUtil.parse(file);

        AstUtils.addImports(cu, IMPORTS);
        AstUtils.addInstanceFields(cu, FIELD_DECLARATIONS, "_tracer");
        AstUtils.addConstructorInit(cu, CONSTRUCTOR_INIT, "_tracer = GlobalOpenTelemetry");

        String className = cu.findFirst(ClassOrInterfaceDeclaration.class)
            .map(ClassOrInterfaceDeclaration::getNameAsString)
            .orElse("Unknown");
        boolean isResource = className.contains("Resource");

        int count = 0;
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (method.getBody().isEmpty()) continue;
            wrapImplMethod(method, featureName, className, isResource);
            count++;
        }

        if (count > 0) {
            AstUtils.overwriteFile(file, cu);
            WinVMJConsole.println("[TracingInjector] Wrapped " + count + " methods in " + file.getName());
        }
    }

    private static void wrapImplMethod(MethodDeclaration method, String featureName,
            String className, boolean isResource) {
        String methodName = method.getNameAsString();
        String fullMethodName = className + "." + methodName;
        String spanKind = isResource ? "SpanKind.SERVER" : "SpanKind.INTERNAL";

        NodeList<Statement> originalStmts = new NodeList<>(method.getBody().get().getStatements());

        BlockStmt newBody = new BlockStmt();
        newBody.addStatement(StaticJavaParser.parseStatement(
            "Span _trSpan = _tracer.spanBuilder(\"" + fullMethodName + "\")"
                + ".setSpanKind(" + spanKind + ")"
                + ".setAttribute(\"feature\", \"" + featureName + "\")"
                + ".setAttribute(\"class\", \"" + className + "\")"
                + ".setAttribute(\"method\", \"" + methodName + "\")"
                + ".startSpan();"));
        newBody.addStatement(StaticJavaParser.parseStatement(
            "Scope _trScope = _trSpan.makeCurrent();"));

        BlockStmt tryBlock = new BlockStmt();
        originalStmts.forEach(tryBlock::addStatement);

        BlockStmt catchBlock = new BlockStmt();
        catchBlock.addStatement(StaticJavaParser.parseStatement(
            "_trSpan.setStatus(StatusCode.ERROR, _trT.getMessage());"));
        catchBlock.addStatement(StaticJavaParser.parseStatement(
            "_trSpan.recordException(_trT);"));
        catchBlock.addStatement(StaticJavaParser.parseStatement("throw _trT;"));

        CatchClause catchClause = new CatchClause(
            new Parameter(StaticJavaParser.parseType("Throwable"), "_trT"), catchBlock);

        BlockStmt finallyBlock = new BlockStmt();
        finallyBlock.addStatement(StaticJavaParser.parseStatement("_trScope.close();"));
        finallyBlock.addStatement(StaticJavaParser.parseStatement("_trSpan.end();"));

        newBody.addStatement(new TryStmt(tryBlock, new NodeList<>(catchClause), finallyBlock));
        method.setBody(newBody);
    }

    // ========== RepositoryImpl (DB layer, SpanKind.CLIENT) ==========

    private static void processRepositoryFile(IFile file, String featureName) {
        WinVMJConsole.println("[TracingInjector] Processing Repository " + file.getFullPath());

        CompilationUnit cu = JavaParserUtil.parse(file);

        AstUtils.addImports(cu, IMPORTS);
        AstUtils.addInstanceFields(cu, FIELD_DECLARATIONS, "_tracer");
        AstUtils.addConstructorInit(cu, CONSTRUCTOR_INIT, "_tracer = GlobalOpenTelemetry");

        int count = 0;
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (method.getBody().isEmpty()) continue;

            String operation = METHOD_TO_OPERATION.get(method.getNameAsString());
            if (operation == null) continue;

            wrapRepositoryMethod(method, featureName, operation);
            count++;
        }

        if (count > 0) {
            AstUtils.overwriteFile(file, cu);
            WinVMJConsole.println("[TracingInjector] Wrapped " + count + " Repository methods in " + file.getName());
        }
    }

    private static void wrapRepositoryMethod(MethodDeclaration method, String featureName, String operation) {
        NodeList<Statement> originalStmts = new NodeList<>(method.getBody().get().getStatements());

        BlockStmt newBody = new BlockStmt();
        newBody.addStatement(StaticJavaParser.parseStatement(
            "Span _trSpan = _tracer.spanBuilder(\"" + operation + " \" + TABLE_NAME)"
                + ".setSpanKind(SpanKind.CLIENT)"
                + ".setAttribute(\"feature\", FEATURE_NAME)"
                + ".setAttribute(\"db.operation\", \"" + operation + "\")"
                + ".setAttribute(\"db.table\", TABLE_NAME)"
                + ".startSpan();"));
        newBody.addStatement(StaticJavaParser.parseStatement(
            "Scope _trScope = _trSpan.makeCurrent();"));

        BlockStmt tryBlock = new BlockStmt();
        originalStmts.forEach(tryBlock::addStatement);

        BlockStmt catchBlock = new BlockStmt();
        catchBlock.addStatement(StaticJavaParser.parseStatement(
            "_trSpan.setStatus(StatusCode.ERROR, _trT.getMessage());"));
        catchBlock.addStatement(StaticJavaParser.parseStatement(
            "_trSpan.recordException(_trT);"));
        catchBlock.addStatement(StaticJavaParser.parseStatement("throw _trT;"));

        CatchClause catchClause = new CatchClause(
            new Parameter(StaticJavaParser.parseType("Throwable"), "_trT"), catchBlock);

        BlockStmt finallyBlock = new BlockStmt();
        finallyBlock.addStatement(StaticJavaParser.parseStatement("_trScope.close();"));
        finallyBlock.addStatement(StaticJavaParser.parseStatement("_trSpan.end();"));

        newBody.addStatement(new TryStmt(tryBlock, new NodeList<>(catchClause), finallyBlock));
        method.setBody(newBody);
    }
}
