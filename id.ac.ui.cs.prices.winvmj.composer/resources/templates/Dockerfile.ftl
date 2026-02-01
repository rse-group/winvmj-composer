# WinVMJ Generated Dockerfile
# Product: ${productName}
# Context: src-gen/${productName}/ (same level as docker-compose.yml)

# Stage 1: Download Gradle dependencies (needs full project context)
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app
COPY . .

# Compile sources and download dependencies
RUN gradle compileJava copyDependencies --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 winvmj && \
    adduser -u 1001 -G winvmj -s /bin/sh -D winvmj

# Copy everything from project folder
COPY . .

# Copy Gradle-downloaded dependencies into jars folder
COPY --from=builder /app/deps/ ./jars/

# Copy compiled classes (includes MonitoringAspect)
COPY --from=builder /app/build/classes/java/main/ ./classes/

# Move product JARs to jars folder for unified classpath
RUN mv ${productPackage}/*.jar ./jars/ 2>/dev/null || true

# Fix Windows CRLF line endings and set permissions
RUN sed -i 's/\r$//' /app/entrypoint.sh && \
    chmod +x /app/entrypoint.sh && \
    chown -R winvmj:winvmj /app

USER winvmj

# Environment variables with defaults
ENV AMANAH_HOST_BE=0.0.0.0
ENV AMANAH_DB_URL=jdbc:postgresql://postgres:5432/${productName?lower_case}
ENV AMANAH_DB_USERNAME=postgres
ENV AMANAH_DB_PASSWORD=postgres123
<#if hasMonitoringAspect>
ENV AMANAH_MONITORING_PORT=9464
</#if>

# Expose internal ports
EXPOSE 7776
<#if hasMonitoringAspect>
EXPOSE 9464
</#if>

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:7776/health || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]
