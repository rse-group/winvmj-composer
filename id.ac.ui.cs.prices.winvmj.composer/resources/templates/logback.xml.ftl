<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

<#if anyLoggingEnabled>
    <appender name="OTEL" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
        <captureExperimentalAttributes>true</captureExperimentalAttributes>
        <captureCodeAttributes>true</captureCodeAttributes>
    </appender>
</#if>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
<#if anyLoggingEnabled>
        <appender-ref ref="OTEL" />
</#if>
    </root>
</configuration>
