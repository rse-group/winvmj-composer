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

# Move product JARs and lib JARs (includes monitoring module) to jars folder
RUN mv ${productPackage}/*.jar ./jars/ 2>/dev/null || true && \
    mv lib/*.jar ./jars/ 2>/dev/null || true

# Set permissions
RUN chown -R winvmj:winvmj /app

USER winvmj

# Expose internal ports
EXPOSE 7776

# Run the application
<#if shouldHaveMonitoring && monitoringMode == "AOP">
CMD ["java", "-javaagent:jars/aspectjweaver-1.9.22.jar", "-cp", "jars/*:.", "${productPackage}.${productName}"]
<#else>
CMD ["java", "-cp", "jars/*:.", "${productPackage}.${productName}"]
</#if>
