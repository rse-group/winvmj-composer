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

<#-- Check if any feature has tracing enabled -->
<#assign anyTracingEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableTracing>
<#assign anyTracingEnabled = true>
<#break>
</#if>
</#list>

<#-- Check if any feature has method metrics enabled -->
<#assign anyMethodMetricsEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableMethodMetrics>
<#assign anyMethodMetricsEnabled = true>
<#break>
</#if>
</#list>

<#-- Check if any feature has HTTP metrics enabled -->
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

<#if enableJvmMetrics>
import io.opentelemetry.instrumentation.runtimemetrics.java8.Classes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.java8.GarbageCollector;
import io.opentelemetry.instrumentation.runtimemetrics.java8.MemoryPools;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Threads;
</#if>

<#-- Check if any feature has DB metrics enabled (for imports) -->
<#assign anyDbMetricsEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableDbMetrics>
<#assign anyDbMetricsEnabled = true>
<#break>
</#if>
</#list>

<#if anyDbMetricsEnabled>
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostLoadEvent;
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
        
        // Get OTLP endpoint from env (default: http://localhost:4318)
        String otlpEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (otlpEndpoint == null) {
            otlpEndpoint = "http://localhost:4318";
        }
        
        System.out.println("== MONITORING ASPECT: Initializing OpenTelemetry with OTLP HTTP ==");
        System.out.println("== MONITORING ASPECT: OTLP endpoint: " + otlpEndpoint + " ==");
        
        try {
            // Setup resource with service name (can be overridden via OTEL_SERVICE_NAME env var)
            String serviceName = System.getenv("OTEL_SERVICE_NAME");
            if (serviceName == null) {
                serviceName = "${productName}";
            }
            
            Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                    AttributeKey.stringKey("service.name"), serviceName
                )));
            
            // Setup OTLP HTTP metrics exporter (push-based)
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
            // Setup OTLP HTTP trace exporter (push-based)
            OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(otlpEndpoint + "/v1/traces")
                .build();
            
            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();
            
            openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .setTracerProvider(tracerProvider)
                .build();
            
            tracer = openTelemetry.getTracer("${monitoringPackage}");
            System.out.println("== MONITORING ASPECT: Tracing enabled ==");
<#else>
            openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .build();
</#if>
            
            meter = openTelemetry.getMeter("${monitoringPackage}");
            
<#if enableJvmMetrics>
            // Register JVM metrics (memory, GC, threads, CPU, classes)
            Classes.registerObservers(openTelemetry);
            Cpu.registerObservers(openTelemetry);
            GarbageCollector.registerObservers(openTelemetry);
            MemoryPools.registerObservers(openTelemetry);
            Threads.registerObservers(openTelemetry);
            System.out.println("== MONITORING ASPECT: JVM metrics registered (memory, GC, threads, CPU, classes) ==");
</#if>
            System.out.println("== MONITORING ASPECT: OpenTelemetry initialized - metrics push every 30s ==");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OpenTelemetry monitoring", e);
        }
    }
    
<#if anyMethodMetricsEnabled>
    // ==================== METHOD METRICS (shared counters) ====================
    
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
    // ==================== HTTP METRICS (shared counters) ====================
    
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

<#-- Check if any feature has DB metrics enabled -->
<#assign anyDbMetricsEnabled = false>
<#list featureMonitoringConfigs as config>
<#if config.enableDbMetrics>
<#assign anyDbMetricsEnabled = true>
<#break>
</#if>
</#list>

<#if anyDbMetricsEnabled>
    // ==================== DB METRICS (Hibernate Event Listener) ====================
    
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
    
    /**
     * Hibernate Event Listener for DB Metrics.
     * Listens to INSERT, UPDATE, DELETE, and SELECT (load) events.
     * Extracts feature name from entity package.
     */
    public static class DbMetricsEventListener implements 
            PreInsertEventListener,
            PreUpdateEventListener,
            PreDeleteEventListener,
            PostLoadEventListener {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * Compile-time resolved map: @Table(name) -> featureName
         * Built by scanning model source files during composition.
         *
         * Example:
         *   "account_overdraft" -> "Overdraft"
         *   "account_dailylimit" -> "DailyLimit"
         *   "account_impl" -> "BankAccount"
         */
        private static final java.util.Map<String, String> TABLE_TO_FEATURE = new java.util.HashMap<>();
        static {
        <#list tableToFeatureMap?keys as tableName>
            TABLE_TO_FEATURE.put("${tableName}", "${tableToFeatureMap[tableName]}");
        </#list>
        }
        
        @Override
        public boolean onPreInsert(PreInsertEvent event) {
            recordDbMetrics("INSERT", event.getPersister().getTableName());
            return false; // Don't veto the insert
        }
        
        @Override
        public boolean onPreUpdate(PreUpdateEvent event) {
            recordDbMetrics("UPDATE", event.getPersister().getTableName());
            return false; // Don't veto the update
        }
        
        @Override
        public boolean onPreDelete(PreDeleteEvent event) {
            recordDbMetrics("DELETE", event.getPersister().getTableName());
            return false; // Don't veto the delete
        }
        
        @Override
        public void onPostLoad(PostLoadEvent event) {
            recordDbMetrics("SELECT", event.getPersister().getTableName());
        }
        
        private void recordDbMetrics(String operation, String tableName) {
            String feature = TABLE_TO_FEATURE.get(tableName);
            if (feature == null) {
                return;
            }
            
            Attributes dbAttributes = Attributes.of(
                AttributeKey.stringKey("feature"), feature,
                AttributeKey.stringKey("db.operation"), operation,
                AttributeKey.stringKey("db.table"), tableName
            );
            
            dbQueryCounter.add(1, dbAttributes);
        }
    }
    
    // Static instance of the event listener
    private static final DbMetricsEventListener dbMetricsEventListener = new DbMetricsEventListener();
    
    /**
     * Returns the Hibernate Event Listener for DB Metrics.
     * This should be registered with Hibernate SessionFactory during application startup.
     * 
     * Example registration in Hibernate configuration:
     *   SessionFactory sessionFactory = ...;
     *   EventListenerRegistry registry = ((SessionFactoryImpl) sessionFactory)
     *       .getServiceRegistry()
     *       .getService(EventListenerRegistry.class);
     *   registry.appendListeners(EventType.PRE_INSERT, MonitoringAspect.getDbMetricsEventListener());
     *   registry.appendListeners(EventType.PRE_UPDATE, MonitoringAspect.getDbMetricsEventListener());
     *   registry.appendListeners(EventType.PRE_DELETE, MonitoringAspect.getDbMetricsEventListener());
     *   registry.appendListeners(EventType.POST_LOAD, MonitoringAspect.getDbMetricsEventListener());
     */
    public static DbMetricsEventListener getDbMetricsEventListener() {
        return dbMetricsEventListener;
    }
</#if>

<#-- ==================== PER-FEATURE POINTCUTS AND ADVICES ==================== -->
<#list featureMonitoringConfigs as config>

    // ==================== FEATURE: ${config.featureName} ====================
    // Logging Level: ${config.loggingLevel}
    // HTTP Metrics: ${config.enableHttpMetrics?string("enabled", "disabled")}
    // Method Metrics: ${config.enableMethodMetrics?string("enabled", "disabled")}
    // Tracing: ${config.enableTracing?string("enabled", "disabled")}
    // DB Metrics: ${config.enableDbMetrics?string("enabled", "disabled")} (TODO: pending discussion)
    
<#if config.enableMethodMetrics>
    // Pointcut for ${config.featureName} all methods (method metrics)
    // Matches all classes in module packages: <#list config.modulePackages as mp>${mp}<#if mp?has_next>, </#if></#list>
    @Pointcut("<#list config.modulePackages as mp>execution(* ${mp}..*.*(..))<#if mp?has_next> || </#if></#list>")
    public void ${config.featureNameLower}AllMethods() {}
    
    @Around("${config.featureNameLower}AllMethods()")
    public Object monitor${config.featureName}Methods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        String featureName = "${config.featureName}";
        String loggingLevel = "${config.loggingLevel}";
        
        // DEBUG logging - method arguments (only when VERBOSE)
        if ("VERBOSE".equals(loggingLevel)) {
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof java.util.Map) {
                    logger.debug("[{}][{}] arg[{}] (Map): {}", featureName, fullMethodName, i, args[i]);
                } else {
                    logger.debug("[{}][{}] arg[{}]: {}", featureName, fullMethodName, i, args[i]);
                }
            }
        }
        
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
            
            if ("INFO".equals(loggingLevel) || "VERBOSE".equals(loggingLevel)) {
                logger.info("[{}] {} completed in {}ms", featureName, fullMethodName, duration);
            }
            
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            
            if ("VERBOSE".equals(loggingLevel)) {
                logger.error("[{}][ERROR] {} threw {} after {}ms", featureName, fullMethodName, t.getClass().getSimpleName(), duration);
            }
            
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

<#if config.enableHttpMetrics || config.enableTracing>
    // Pointcut for ${config.featureName} resource/controller methods (HTTP endpoints)
    // Matches *Resource* classes in module packages: <#list config.modulePackages as mp>${mp}<#if mp?has_next>, </#if></#list>
    @Pointcut("<#list config.modulePackages as mp>execution(* ${mp}.*Resource*.*(..))<#if mp?has_next> || </#if></#list>")
    public void ${config.featureNameLower}HttpEndpoints() {}
    
    @Around("${config.featureNameLower}HttpEndpoints()")
    public Object monitor${config.featureName}Http(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String featureName = "${config.featureName}";
        String loggingLevel = "${config.loggingLevel}";
        
        // Extract HTTP method from VMJExchange parameter
        String httpMethod = "UNKNOWN";
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof VMJExchange) {
                VMJExchange exchange = (VMJExchange) arg;
                httpMethod = exchange.getHttpMethod();
                break;
            }
        }
        
        // Extract route URL from @Route annotation
        String httpRoute = "UNKNOWN";
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Route routeAnnotation = signature.getMethod().getAnnotation(Route.class);
            if (routeAnnotation != null) {
                httpRoute = routeAnnotation.url();
            }
        } catch (Exception e) {
            // Ignore annotation extraction errors
        }
        
        long startTime = System.currentTimeMillis();
        boolean success = true;
        int httpStatusCode = 200;
        
<#if config.enableTracing>
        // Create root span for HTTP request (distributed tracing)
        Span span = tracer.spanBuilder(httpMethod + " /" + httpRoute)
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("feature", featureName)
            .setAttribute("http.method", httpMethod)
            .setAttribute("http.route", "/" + httpRoute)
            .setAttribute("class", className)
            .setAttribute("method", methodName)
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
<#else>
        try {
</#if>
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            
            // Extract HTTP status code from VMJException if available
            if (t instanceof VMJException) {
                httpStatusCode = ((VMJException) t).getHttpStatusCode();
            } else if (t instanceof IllegalArgumentException) {
                httpStatusCode = 400;
            } else {
                httpStatusCode = 500;
            }
            
<#if config.enableTracing>
            span.setStatus(StatusCode.ERROR, t.getMessage());
            span.recordException(t);
</#if>
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
<#if config.enableTracing>
            span.setAttribute("http.status_code", httpStatusCode);
            span.setAttribute("duration_ms", duration);
            span.end();
</#if>
            
<#if config.enableHttpMetrics>
            Attributes httpAttributes = Attributes.of(
                AttributeKey.stringKey("feature"), featureName,
                AttributeKey.stringKey("http.method"), httpMethod,
                AttributeKey.stringKey("http.route"), "/" + httpRoute,
                AttributeKey.booleanKey("success"), success
            );
            
            httpRequestCounter.add(1, httpAttributes);
            httpRequestDurationHistogram.record(duration, httpAttributes);
</#if>
            
            if ("VERBOSE".equals(loggingLevel)) {
                logger.debug("[{}][HTTP] {} /{} -> {} ({}ms)", featureName, httpMethod, httpRoute, success ? "OK" : "ERROR", duration);
            } else if ("INFO".equals(loggingLevel)) {
                logger.info("[{}][HTTP] {} /{} completed in {}ms", featureName, httpMethod, httpRoute, duration);
            }
        }
    }
</#if>

<#-- DB Metrics now handled by Hibernate Event Listener (DbMetricsEventListener inner class) -->

</#list>
}
