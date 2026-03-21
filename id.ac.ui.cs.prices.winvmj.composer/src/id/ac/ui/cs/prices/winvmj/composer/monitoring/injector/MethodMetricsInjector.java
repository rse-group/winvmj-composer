package id.ac.ui.cs.prices.winvmj.composer.monitoring.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
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
import java.util.Optional;

/**
 * Injects method-level metrics into ResourceImpl and ServiceImpl files.
 * Wraps all non-constructor methods with timing + OTel counter/histogram recording.
 */
public class MethodMetricsInjector {

    private static final List<String> IMPORTS = List.of(
        "io.opentelemetry.api.GlobalOpenTelemetry",
        "io.opentelemetry.api.metrics.Meter",
        "io.opentelemetry.api.metrics.LongCounter",
        "io.opentelemetry.api.metrics.LongHistogram",
        "io.opentelemetry.api.common.Attributes",
        "io.opentelemetry.api.common.AttributeKey"
    );

    private static final String[] FIELD_DECLARATIONS = {
        "private Meter _mmMeter;",
        "private LongCounter _mmCallCounter;",
        "private LongCounter _mmErrorCounter;",
        "private LongHistogram _mmDurationHistogram;"
    };

    private static final String[] CONSTRUCTOR_INIT = {
        "_mmMeter = GlobalOpenTelemetry.get().getMeter(\"monitoring\");",
        "_mmCallCounter = _mmMeter.counterBuilder(\"method_calls_total\").setDescription(\"Total number of method calls\").build();",
        "_mmErrorCounter = _mmMeter.counterBuilder(\"method_errors_total\").setDescription(\"Total number of method errors\").build();",
        "_mmDurationHistogram = _mmMeter.histogramBuilder(\"method_duration_ms\").setDescription(\"Method execution duration in milliseconds\").ofLongs().build();"
    };

    public static void inject(IFolder moduleDir, String featureName) {
        try {
            IFile moduleInfo = moduleDir.getFile("module-info.java");
            if (moduleInfo.exists()) {
                AstUtils.addModuleRequires(moduleInfo, List.of("io.opentelemetry.api"));
            }
            for (IFile file : AstUtils.findServiceImplFiles(moduleDir)) {
                processFile(file, featureName);
            }
            for (IFile file : AstUtils.findResourceImplFiles(moduleDir)) {
                processFile(file, featureName);
            }
            for (IFile file : AstUtils.findRepositoryImplFiles(moduleDir)) {
                processFile(file, featureName);
            }
        } catch (CoreException e) {
            WinVMJConsole.println("[MethodMetricsInjector] Error scanning " + moduleDir.getName() + ": " + e.getMessage());
        }
    }

    private static void processFile(IFile file, String featureName) {
        WinVMJConsole.println("[MethodMetricsInjector] Processing " + file.getFullPath());

        CompilationUnit cu = JavaParserUtil.parse(file);

        AstUtils.addImports(cu, IMPORTS);
        AstUtils.addInstanceFields(cu, FIELD_DECLARATIONS, "_mmMeter");
        AstUtils.addConstructorInit(cu, CONSTRUCTOR_INIT, "_mmMeter = GlobalOpenTelemetry");

        int count = 0;
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            Optional<BlockStmt> bodyOpt = method.getBody();
            if (bodyOpt.isEmpty()) continue;

            wrapMethod(method, featureName);
            count++;
        }

        if (count > 0) {
            AstUtils.overwriteFile(file, cu);
            WinVMJConsole.println("[MethodMetricsInjector] Wrapped " + count + " methods in " + file.getName());
        }
    }

    private static void wrapMethod(MethodDeclaration method, String featureName) {
        Optional<BlockStmt> bodyOpt = method.getBody();
        if (bodyOpt.isEmpty()) return;

        String className = method.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
            .map(c -> c.getNameAsString())
            .orElse("Unknown");
        String methodName = method.getNameAsString();

        NodeList<Statement> originalStmts = new NodeList<>(bodyOpt.get().getStatements());

        String attrsExpr = "Attributes.of("
            + "AttributeKey.stringKey(\"feature\"), \"" + featureName + "\", "
            + "AttributeKey.stringKey(\"class\"), \"" + className + "\", "
            + "AttributeKey.stringKey(\"method\"), \"" + methodName + "\")";

        BlockStmt newBody = new BlockStmt();
        newBody.addStatement(StaticJavaParser.parseStatement(
            "long _mmStartTime = System.currentTimeMillis();"));

        // try { originalBody } — success metrics recorded in finally with _mmSuccess flag
        BlockStmt tryBlock = new BlockStmt();
        originalStmts.forEach(tryBlock::addStatement);

        // catch (Throwable _mmT) { mark failure, record error counter, rethrow }
        BlockStmt catchBlock = new BlockStmt();
        catchBlock.addStatement(StaticJavaParser.parseStatement("_mmSuccess = false;"));
        catchBlock.addStatement(StaticJavaParser.parseStatement(
            "_mmErrorCounter.add(1, Attributes.of("
                + "AttributeKey.stringKey(\"feature\"), \"" + featureName + "\", "
                + "AttributeKey.stringKey(\"class\"), \"" + className + "\", "
                + "AttributeKey.stringKey(\"method\"), \"" + methodName + "\", "
                + "AttributeKey.stringKey(\"exception\"), _mmT.getClass().getSimpleName()));"));
        catchBlock.addStatement(StaticJavaParser.parseStatement("throw _mmT;"));

        CatchClause catchClause = new CatchClause(
            new Parameter(StaticJavaParser.parseType("Throwable"), "_mmT"), catchBlock);

        // finally { always record call count + duration }
        BlockStmt finallyBlock = new BlockStmt();
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "long _mmDuration = System.currentTimeMillis() - _mmStartTime;"));
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "_mmCallCounter.add(1, " + attrsExpr + ");"));
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "_mmDurationHistogram.record(_mmDuration, " + attrsExpr + ");"));

        // Add _mmSuccess flag before try
        newBody.addStatement(StaticJavaParser.parseStatement("boolean _mmSuccess = true;"));
        newBody.addStatement(new TryStmt(tryBlock, new NodeList<>(catchClause), finallyBlock));
        method.setBody(newBody);
    }
}
