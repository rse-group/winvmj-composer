package ${productPackage};

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.metrics.export.MetricReader;
<#if enableJvmMetrics>
import io.opentelemetry.instrumentation.runtimemetrics.java8.Classes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.java8.GarbageCollector;
import io.opentelemetry.instrumentation.runtimemetrics.java8.MemoryPools;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Threads;
</#if>

@Aspect
public class MonitoringAspect {

    private static final OpenTelemetry openTelemetry;
    private static final Meter meter;
    
    static {
        // Initialize OpenTelemetry with Prometheus exporter
        int port = getMonitoringPort();
        System.out.println("== MONITORING ASPECT: Initializing OpenTelemetry on port " + port + " ==");
        
        try {
            MetricReader prometheusReader = PrometheusHttpServer.builder()
                .setPort(port)
                .build();
            
            SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(prometheusReader)
                .build();
            
            openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .build();
            
            meter = openTelemetry.getMeter("${productPackage}");
            
<#if enableJvmMetrics>
            // Register JVM metrics (memory, GC, threads, CPU, classes)
            Classes.registerObservers(openTelemetry);
            Cpu.registerObservers(openTelemetry);
            GarbageCollector.registerObservers(openTelemetry);
            MemoryPools.registerObservers(openTelemetry);
            Threads.registerObservers(openTelemetry);
            System.out.println("== MONITORING ASPECT: JVM metrics registered (memory, GC, threads, CPU, classes) ==");
</#if>
            System.out.println("== MONITORING ASPECT: OpenTelemetry initialized - Prometheus metrics at :" + port + "/metrics ==");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OpenTelemetry monitoring", e);
        }
    }
    
    private static int getMonitoringPort() {
        String portStr = System.getenv("AMANAH_MONITORING_PORT");
        return portStr != null ? Integer.parseInt(portStr) : 9464;
    }
    
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

<#list monitoredModules as module>
    @Pointcut("execution(* ${module}..*.*(..))")
    public void ${module?replace(".", "_")}_methods() {}

</#list>
    @Around("<#list monitoredModules as module>${module?replace(".", "_")}_methods()<#if module_has_next> || </#if></#list>")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        Attributes attributes = Attributes.of(
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
}
