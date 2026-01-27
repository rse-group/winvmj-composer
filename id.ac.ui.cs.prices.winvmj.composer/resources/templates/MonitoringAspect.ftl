package ${productPackage};

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;

@Aspect
public class MonitoringAspect {

    private static final Meter meter = GlobalOpenTelemetry.getMeter("${productPackage}");
    
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
