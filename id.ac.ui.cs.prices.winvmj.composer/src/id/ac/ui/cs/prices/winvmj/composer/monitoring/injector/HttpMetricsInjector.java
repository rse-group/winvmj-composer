package id.ac.ui.cs.prices.winvmj.composer.monitoring.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

import id.ac.ui.cs.prices.winvmj.composer.microservicepreprocessor.JavaParserUtil;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.util.List;
import java.util.Optional;

/**
 * Injects HTTP metrics instrumentation into ResourceImpl files.
 * Wraps @Route methods with timing + OTel counter/histogram recording.
 */
public class HttpMetricsInjector {

    private static final List<String> IMPORTS = List.of(
        "io.opentelemetry.api.GlobalOpenTelemetry",
        "io.opentelemetry.api.metrics.Meter",
        "io.opentelemetry.api.metrics.LongCounter",
        "io.opentelemetry.api.metrics.LongHistogram",
        "io.opentelemetry.api.common.Attributes",
        "io.opentelemetry.api.common.AttributeKey"
    );

    private static final String[] FIELD_DECLARATIONS = {
        "private Meter _meter;",
        "private LongCounter _httpRequestCounter;",
        "private LongHistogram _httpRequestDurationHistogram;"
    };

    private static final String[] CONSTRUCTOR_INIT = {
        "_meter = GlobalOpenTelemetry.get().getMeter(\"monitoring\");",
        "_httpRequestCounter = _meter.counterBuilder(\"http_requests_total\").setDescription(\"Total number of HTTP requests\").build();",
        "_httpRequestDurationHistogram = _meter.histogramBuilder(\"http_request_duration_ms\").setDescription(\"HTTP request duration in milliseconds\").ofLongs().build();"
    };

    public static void inject(IFolder moduleDir, String featureName) {
        try {
            findAndProcess(moduleDir, featureName);
        } catch (CoreException e) {
            WinVMJConsole.println("[HttpMetricsInjector] Error scanning " + moduleDir.getName() + ": " + e.getMessage());
        }
    }

    private static void findAndProcess(IFolder folder, String featureName) throws CoreException {
        for (IResource resource : folder.members()) {
            if (resource instanceof IFile file && file.getName().endsWith("ResourceImpl.java")) {
                processFile(file, featureName);
            } else if (resource instanceof IFolder subFolder) {
                findAndProcess(subFolder, featureName);
            }
        }
    }

    private static void processFile(IFile file, String featureName) {
        WinVMJConsole.println("[HttpMetricsInjector] Processing " + file.getFullPath());

        CompilationUnit cu = JavaParserUtil.parse(file);

        AstUtils.addImports(cu, IMPORTS);
        AstUtils.addInstanceFields(cu, FIELD_DECLARATIONS, "_meter");
        AstUtils.addConstructorInit(cu, CONSTRUCTOR_INIT, "_meter = GlobalOpenTelemetry");

        int count = 0;
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            Optional<AnnotationExpr> routeAnnotation = method.getAnnotationByName("Route");
            if (routeAnnotation.isPresent()) {
                String routeUrl = extractRouteUrl(routeAnnotation.get());
                String vmjParam = findVMJExchangeParam(method);
                if (vmjParam != null) {
                    wrapMethod(method, featureName, routeUrl, vmjParam);
                    count++;
                }
            }
        }

        if (count > 0) {
            AstUtils.overwriteFile(file, cu);
            WinVMJConsole.println("[HttpMetricsInjector] Wrapped " + count + " @Route methods in " + file.getName());
        }
    }

    private static String extractRouteUrl(AnnotationExpr annotation) {
        if (annotation.isNormalAnnotationExpr()) {
            NormalAnnotationExpr normal = annotation.asNormalAnnotationExpr();
            for (MemberValuePair pair : normal.getPairs()) {
                if (pair.getNameAsString().equals("url")) {
                    return pair.getValue().toString().replaceAll("^\"|\"$", "");
                }
            }
        }
        return "UNKNOWN";
    }

    private static String findVMJExchangeParam(MethodDeclaration method) {
        for (Parameter param : method.getParameters()) {
            if (param.getTypeAsString().equals("VMJExchange")) {
                return param.getNameAsString();
            }
        }
        return null;
    }

    private static void wrapMethod(
            MethodDeclaration method, String featureName, String routeUrl, String vmjParam) {

        Optional<BlockStmt> bodyOpt = method.getBody();
        if (bodyOpt.isEmpty()) return;

        NodeList<Statement> originalStmts = new NodeList<>(bodyOpt.get().getStatements());

        BlockStmt newBody = new BlockStmt();
        newBody.addStatement(StaticJavaParser.parseStatement(
            "String _httpMethod = " + vmjParam + ".getHttpMethod();"));
        newBody.addStatement(StaticJavaParser.parseStatement(
            "long _startTime = System.currentTimeMillis();"));
        newBody.addStatement(StaticJavaParser.parseStatement(
            "boolean _success = true;"));

        // try { originalBody }
        BlockStmt tryBlock = new BlockStmt();
        originalStmts.forEach(tryBlock::addStatement);

        // catch (Throwable _t) { _success = false; throw _t; }
        BlockStmt catchBlock = new BlockStmt();
        catchBlock.addStatement(StaticJavaParser.parseStatement("_success = false;"));
        catchBlock.addStatement(StaticJavaParser.parseStatement("throw _t;"));
        CatchClause catchClause = new CatchClause(
            new Parameter(StaticJavaParser.parseType("Throwable"), "_t"), catchBlock);

        // finally { record metrics }
        BlockStmt finallyBlock = new BlockStmt();
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "long _duration = System.currentTimeMillis() - _startTime;"));
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "Attributes _httpAttrs = Attributes.of("
                + "AttributeKey.stringKey(\"feature\"), \"" + featureName + "\", "
                + "AttributeKey.stringKey(\"http.method\"), _httpMethod, "
                + "AttributeKey.stringKey(\"http.route\"), \"/" + routeUrl + "\", "
                + "AttributeKey.booleanKey(\"success\"), _success);"));
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "_httpRequestCounter.add(1, _httpAttrs);"));
        finallyBlock.addStatement(StaticJavaParser.parseStatement(
            "_httpRequestDurationHistogram.record(_duration, _httpAttrs);"));

        newBody.addStatement(new TryStmt(tryBlock, new NodeList<>(catchClause), finallyBlock));
        method.setBody(newBody);
    }
}
