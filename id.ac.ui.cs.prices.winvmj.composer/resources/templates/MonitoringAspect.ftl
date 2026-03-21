package ${monitoringPackage};

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.ac.ui.cs.prices.winvmj.core.Route;
import id.ac.ui.cs.prices.winvmj.core.VMJExchange;
import id.ac.ui.cs.prices.winvmj.core.exceptions.VMJException;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.resources.Resource;

<#assign anyTracingEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableTracing>
<#assign anyTracingEnabled = true>
<#break>
</#if>
</#list>

<#assign anyLoggingEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableLogging>
<#assign anyLoggingEnabled = true>
<#break>
</#if>
</#list>

<#assign anyMethodMetricsEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableMethodMetrics>
<#assign anyMethodMetricsEnabled = true>
<#break>
</#if>
</#list>

<#assign anyHttpMetricsEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableHttpMetrics>
<#assign anyHttpMetricsEnabled = true>
<#break>
</#if>
</#list>

<#if anyTracingEnabled>
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
</#if>

<#if anyLoggingEnabled>
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
</#if>

<#if enableJvmMetrics>
import io.opentelemetry.instrumentation.runtimemetrics.java8.Classes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.java8.GarbageCollector;
import io.opentelemetry.instrumentation.runtimemetrics.java8.MemoryPools;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Threads;
</#if>

<#-- Check if any feature has DB metrics enabled -->
<#assign anyDbMetricsEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableDbMetrics>
<#assign anyDbMetricsEnabled = true>
<#break>
</#if>
</#list>

<#-- Check if any feature needs DB-level interception (metrics OR tracing) -->
<#assign anyDbInterceptEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableDbMetrics || config.enableTracing>
<#assign anyDbInterceptEnabled = true>
<#break>
</#if>
</#list>

<#if anyDbInterceptEnabled>
import java.util.regex.Matcher;
import java.util.regex.Pattern;
</#if>

@Aspect
public class MonitoringAspect {

    private static final Logger logger;
    private static final OpenTelemetry openTelemetry;
    private static final Meter meter;
    <#if anyTracingEnabled>
    private static final Tracer tracer;
    </#if>
    
    static {
        logger = LoggerFactory.getLogger(MonitoringAspect.class);
        
        String otlpEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (otlpEndpoint == null) {
            otlpEndpoint = "http://localhost:4318";
        }
        
        System.out.println("== MONITORING ASPECT: Initializing OpenTelemetry with OTLP HTTP ==");
        System.out.println("== MONITORING ASPECT: OTLP endpoint: " + otlpEndpoint + " ==");
        
        try {
            String serviceName = System.getenv("OTEL_SERVICE_NAME");
            if (serviceName == null) {
                serviceName = "${productName}";
            }
            
            Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                    AttributeKey.stringKey("service.name"), serviceName
                )));
            
            OtlpHttpMetricExporter metricExporter = OtlpHttpMetricExporter.builder()
                .setEndpoint(otlpEndpoint + "/v1/metrics")
                .build();
            
            SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                    .setInterval(java.time.Duration.ofSeconds(30))
                    .build())
                .build();
            
<#if anyTracingEnabled>
            OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(otlpEndpoint + "/v1/traces")
                .build();
            
            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();
</#if>
            
<#if anyLoggingEnabled>
            OtlpHttpLogRecordExporter logExporter = OtlpHttpLogRecordExporter.builder()
                .setEndpoint(otlpEndpoint + "/v1/logs")
                .build();
            
            SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
                .setResource(resource)
                .build();
</#if>
            
            openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
<#if anyTracingEnabled>
                .setTracerProvider(tracerProvider)
</#if>
<#if anyLoggingEnabled>
                .setLoggerProvider(loggerProvider)
</#if>
                .build();
            
<#if anyTracingEnabled>
            tracer = openTelemetry.getTracer("${monitoringPackage}");
            System.out.println("== MONITORING ASPECT: Tracing enabled ==");
</#if>
<#if anyLoggingEnabled>
            OpenTelemetryAppender.install(openTelemetry);
            System.out.println("== MONITORING ASPECT: Log export to OTel Collector enabled ==");
</#if>
            
            meter = openTelemetry.getMeter("${monitoringPackage}");
            
<#if enableJvmMetrics>
            Classes.registerObservers(openTelemetry);
            Cpu.registerObservers(openTelemetry);
            GarbageCollector.registerObservers(openTelemetry);
            MemoryPools.registerObservers(openTelemetry);
            Threads.registerObservers(openTelemetry);
            System.out.println("== MONITORING ASPECT: JVM metrics registered ==");
</#if>
            System.out.println("== MONITORING ASPECT: OpenTelemetry initialized - metrics push every 30s ==");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OpenTelemetry monitoring", e);
        }
    }
    
<#if anyMethodMetricsEnabled>
    private static final LongCounter methodCallCounter = meter
        .counterBuilder("method_calls_total")
        .setDescription("Total number of method calls")
        .build();
    
    private static final LongCounter methodErrorCounter = meter
        .counterBuilder("method_errors_total")
        .setDescription("Total number of method errors")
        .build();
    
    private static final LongHistogram methodDurationHistogram = meter
        .histogramBuilder("method_duration_ms")
        .setDescription("Method execution duration in milliseconds")
        .ofLongs()
        .build();
</#if>

<#if anyHttpMetricsEnabled>
    private static final LongCounter httpRequestCounter = meter
        .counterBuilder("http_requests_total")
        .setDescription("Total number of HTTP requests")
        .build();
    
    private static final LongHistogram httpRequestDurationHistogram = meter
        .histogramBuilder("http_request_duration_ms")
        .setDescription("HTTP request duration in milliseconds")
        .ofLongs()
        .build();
</#if>

<#if anyDbInterceptEnabled>
<#if anyDbMetricsEnabled>
    private static final LongCounter dbQueryCounter = meter
        .counterBuilder("db_queries_total")
        .setDescription("Total number of database queries")
        .build();
    
    private static final LongCounter dbQueryErrorCounter = meter
        .counterBuilder("db_query_errors_total")
        .setDescription("Total number of failed database queries")
        .build();
    
    private static final LongHistogram dbQueryDurationHistogram = meter
        .histogramBuilder("db_query_duration_ms")
        .setDescription("Database query duration in milliseconds")
        .ofLongs()
        .build();
</#if>
    
    /**
     * Compile-time resolved map: @Table(name) -> featureName.
     */
    private static final java.util.Map<String, String> TABLE_TO_FEATURE = new java.util.HashMap<>();
    static {
    <#list tableToFeatureMap?keys as tableName>
        TABLE_TO_FEATURE.put("${tableName}", "${tableToFeatureMap[tableName]}");
    </#list>
    }

    private static final java.util.Map<String, Boolean> FEATURE_LOGGING_ENABLED = new java.util.HashMap<>();
    static {
    <#list featureMonitoringConfigs as config>
    <#if config.enableDbMetrics || config.enableTracing>
        FEATURE_LOGGING_ENABLED.put("${config.featureName}", ${config.enableLogging?string("true", "false")});
    </#if>
    </#list>
    }

<#if anyTracingEnabled>
    private static final java.util.Map<String, Boolean> FEATURE_TRACING_ENABLED = new java.util.HashMap<>();
    static {
    <#list featureMonitoringConfigs as config>
    <#if config.enableDbMetrics || config.enableTracing>
        FEATURE_TRACING_ENABLED.put("${config.featureName}", ${config.enableTracing?string("true", "false")});
    </#if>
    </#list>
    }
</#if>

    private static final Pattern INSERT_PATTERN = Pattern.compile("(?i)insert\\s+into\\s+(\\S+)");
    private static final Pattern UPDATE_PATTERN = Pattern.compile("(?i)update\\s+(\\S+)");
    private static final Pattern DELETE_PATTERN = Pattern.compile("(?i)delete\\s+from\\s+(\\S+)");
    private static final Pattern FROM_JOIN_PATTERN = Pattern.compile("(?i)(?:from|join)\\s+(\\S+)");

    private static java.util.Set<String> extractAllTableNames(String sql) {
        java.util.Set<String> tables = new java.util.LinkedHashSet<>();
        if (sql == null) return tables;
        Matcher m;
        m = INSERT_PATTERN.matcher(sql);
        if (m.find()) tables.add(m.group(1).toLowerCase());
        m = UPDATE_PATTERN.matcher(sql);
        if (m.find()) tables.add(m.group(1).toLowerCase());
        m = DELETE_PATTERN.matcher(sql);
        if (m.find()) tables.add(m.group(1).toLowerCase());
        m = FROM_JOIN_PATTERN.matcher(sql);
        while (m.find()) tables.add(m.group(1).toLowerCase());
        return tables;
    }

    private static String detectOperation(String sql) {
        if (sql == null) return "UNKNOWN";
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("INSERT")) return "INSERT";
        if (trimmed.startsWith("UPDATE")) return "UPDATE";
        if (trimmed.startsWith("DELETE")) return "DELETE";
        if (trimmed.startsWith("SELECT")) return "SELECT";
        return "OTHER";
    }

    private static String extractSqlFromStatement(Object[] args) {
        // Statement variants: SQL is in args[1]
        // PreparedStatement variants: fallback to statement.toString()
        if (args.length >= 2 && args[1] instanceof String) {
            return (String) args[1];
        }
        if (args.length >= 1 && args[0] != null) {
            return args[0].toString();
        }
        return null;
    }

    @Pointcut("execution(* org.hibernate.engine.jdbc.internal.ResultSetReturnImpl.extract(..)) || " +
              "execution(* org.hibernate.engine.jdbc.internal.ResultSetReturnImpl.execute(..)) || " +
              "execution(* org.hibernate.engine.jdbc.internal.ResultSetReturnImpl.executeUpdate(..))")
    public void hibernateJdbcExecution() {}

    @Around("hibernateJdbcExecution()")
    public Object monitorDbExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String sql = extractSqlFromStatement(args);
        java.util.Set<String> tableNames = extractAllTableNames(sql);
        String operation = detectOperation(sql);

        java.util.Set<String> matchedFeatures = new java.util.LinkedHashSet<>();
        java.util.Map<String, String> featureToTable = new java.util.LinkedHashMap<>();
        for (String table : tableNames) {
            String feature = TABLE_TO_FEATURE.get(table);
            if (feature != null && matchedFeatures.add(feature)) {
                featureToTable.put(feature, table);
            }
        }

        long startTime = System.nanoTime();
<#if anyTracingEnabled>
        // Create DB span if any matched feature has tracing enabled
        Span dbSpan = null;
        Scope dbScope = null;
        for (String feature : matchedFeatures) {
            Boolean tracingEnabled = FEATURE_TRACING_ENABLED.get(feature);
            if (Boolean.TRUE.equals(tracingEnabled)) {
                String spanName = operation + " " + String.join(",", tableNames);
                dbSpan = tracer.spanBuilder(spanName)
                    .setSpanKind(SpanKind.CLIENT)
                    .setAttribute("feature", feature)
                    .setAttribute("db.operation", operation)
                    .setAttribute("db.table", featureToTable.getOrDefault(feature, "unknown"))
                    .setAttribute("db.statement", sql != null ? sql : "")
                    .startSpan();
                dbScope = dbSpan.makeCurrent();
                break; // one span per DB execution is enough
            }
        }
</#if>
        try {
            Object result = joinPoint.proceed();
            long duration = (System.nanoTime() - startTime) / 1_000_000;

<#if anyDbMetricsEnabled>
            for (String feature : matchedFeatures) {
                Attributes dbAttributes = Attributes.of(
                    AttributeKey.stringKey("feature"), feature,
                    AttributeKey.stringKey("db.operation"), operation,
                    AttributeKey.stringKey("db.table"), featureToTable.get(feature)
                );
                dbQueryCounter.add(1, dbAttributes);
                dbQueryDurationHistogram.record(duration, dbAttributes);
            }
</#if>

            for (String feature : matchedFeatures) {
                Boolean loggingEnabled = FEATURE_LOGGING_ENABLED.get(feature);
                if (Boolean.TRUE.equals(loggingEnabled)) {
                    logger.info("[{}][DB] {} {} {}ms", feature, operation, featureToTable.get(feature), duration);
                }
            }
            return result;
        } catch (Throwable t) {
            long duration = (System.nanoTime() - startTime) / 1_000_000;

<#if anyTracingEnabled>
            if (dbSpan != null) {
                dbSpan.setStatus(StatusCode.ERROR, t.getMessage());
                dbSpan.recordException(t);
            }
</#if>

<#if anyDbMetricsEnabled>
            for (String feature : matchedFeatures) {
                Attributes errorAttributes = Attributes.of(
                    AttributeKey.stringKey("feature"), feature,
                    AttributeKey.stringKey("db.operation"), operation,
                    AttributeKey.stringKey("db.table"), featureToTable.get(feature)
                );
                dbQueryErrorCounter.add(1, errorAttributes);
                dbQueryDurationHistogram.record(duration, errorAttributes);
            }
</#if>

            for (String feature : matchedFeatures) {
                Boolean loggingEnabled = FEATURE_LOGGING_ENABLED.get(feature);
                if (Boolean.TRUE.equals(loggingEnabled)) {
                    logger.error("[{}][DB][ERROR] {} {} {}ms - {}", feature, operation, featureToTable.get(feature), duration, t.getMessage());
                }
            }
            throw t;
        } finally {
<#if anyTracingEnabled>
            if (dbSpan != null) {
                dbSpan.setAttribute("duration_ms", (System.nanoTime() - startTime) / 1_000_000);
                dbSpan.end();
            }
            if (dbScope != null) {
                dbScope.close();
            }
</#if>
        }
    }
</#if>

<#list featureMonitoringConfigs as config>

    // ==================== FEATURE: ${config.featureName} ====================

<#if config.enableMethodMetrics || config.enableTracing || config.enableLogging>
    @Pointcut("<#list config.modulePackages as mp>execution(* ${mp}..*ServiceImpl.*(..)) || execution(* ${mp}..*ResourceImpl.*(..))<#if mp?has_next> || </#if></#list>")
    public void ${config.featureNameLower}AllMethods() {}
</#if>

    <#-- ===== 1. Tracing advice (outermost — declared first) ===== -->
<#if config.enableTracing>
    @Around("${config.featureNameLower}AllMethods()")
    public Object trace${config.featureName}(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        SpanKind spanKind = className.contains("Resource") ? SpanKind.SERVER : SpanKind.INTERNAL;
        Span span = tracer.spanBuilder(fullMethodName)
            .setSpanKind(spanKind)
            .setAttribute("feature", "${config.featureName}")
            .setAttribute("class", className)
            .setAttribute("method", methodName)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR, t.getMessage());
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }
</#if>

    <#-- ===== 2. Logging advice ===== -->
<#if config.enableLogging>
    @Around("${config.featureNameLower}AllMethods()")
    public Object log${config.featureName}(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        String featureName = "${config.featureName}";

        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            logger.info("[{}][{}] arg[{}]: {}", featureName, fullMethodName, i, args[i]);
        }

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[{}] {} completed in {}ms", featureName, fullMethodName, duration);
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[{}][ERROR] {} threw {} after {}ms: {}", featureName, fullMethodName,
                t.getClass().getSimpleName(), duration, t.getMessage());
            throw t;
        }
    }
</#if>

    <#-- ===== 3. MethodMetrics advice ===== -->
<#if config.enableMethodMetrics>
    @Around("${config.featureNameLower}AllMethods()")
    public Object methodMetrics${config.featureName}(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String featureName = "${config.featureName}";

        Attributes attributes = Attributes.of(
            AttributeKey.stringKey("feature"), featureName,
            AttributeKey.stringKey("class"), className,
            AttributeKey.stringKey("method"), methodName
        );

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            methodCallCounter.add(1, attributes);
            methodDurationHistogram.record(duration, attributes);
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            Attributes errorAttributes = Attributes.of(
                AttributeKey.stringKey("feature"), featureName,
                AttributeKey.stringKey("class"), className,
                AttributeKey.stringKey("method"), methodName,
                AttributeKey.stringKey("exception"), t.getClass().getSimpleName()
            );
            methodCallCounter.add(1, attributes);
            methodErrorCounter.add(1, errorAttributes);
            methodDurationHistogram.record(duration, attributes);
            throw t;
        }
    }
</#if>

    <#-- ===== 4. HttpMetrics advice ===== -->
<#if config.enableHttpMetrics>
    @Pointcut("(<#list config.modulePackages as mp>execution(* ${mp}.*Resource*.*(..))<#if mp?has_next> || </#if></#list>) && @annotation(id.ac.ui.cs.prices.winvmj.core.Route)")
    public void ${config.featureNameLower}HttpEndpoints() {}

    @Around("${config.featureNameLower}HttpEndpoints()")
    public Object httpMetrics${config.featureName}(ProceedingJoinPoint joinPoint) throws Throwable {
        String featureName = "${config.featureName}";

        String httpMethod = "UNKNOWN";
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof VMJExchange) {
                httpMethod = ((VMJExchange) arg).getHttpMethod();
                break;
            }
        }

        String httpRoute = "UNKNOWN";
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Route routeAnnotation = signature.getMethod().getAnnotation(Route.class);
            if (routeAnnotation != null) {
                httpRoute = routeAnnotation.url();
            }
        } catch (Exception e) {
        }

        long startTime = System.currentTimeMillis();
        int statusCode = 200;

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            statusCode = (t instanceof VMJException) ? ((VMJException) t).getHttpStatusCode() : (t.getMessage() != null ? 400 : 500);
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            Attributes httpAttributes = Attributes.of(
                AttributeKey.stringKey("feature"), featureName,
                AttributeKey.stringKey("http.method"), httpMethod,
                AttributeKey.stringKey("http.route"), "/" + httpRoute,
                AttributeKey.longKey("http.status_code"), (long) statusCode
            );

            httpRequestCounter.add(1, httpAttributes);
            httpRequestDurationHistogram.record(duration, httpAttributes);
        }
    }
</#if>

</#list>
}
