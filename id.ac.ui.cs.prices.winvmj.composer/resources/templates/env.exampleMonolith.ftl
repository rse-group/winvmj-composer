# WinVMJ Environment Configuration
# Product: ${productName}

# ============================================
# Backend Server Configuration
# ============================================
AMANAH_HOST_BE=0.0.0.0
AMANAH_PORT_BE=7776

# ============================================
# Database Configuration
# ============================================
AMANAH_DB_URL=jdbc:postgresql://postgres:5432/${productName?lower_case}
AMANAH_DB_USERNAME=postgres
AMANAH_DB_PASSWORD=postgres123

# ============================================
# Docker Port Mappings
# ============================================
HOST_PORT_BE=7776
HOST_PORT_POSTGRES=5432

<#if shouldHaveMonitoring>
# ============================================
# Monitoring Configuration (OpenTelemetry)
# ============================================
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
OTEL_SERVICE_NAME=${productName}
AMANAH_LOGGING_LEVEL=VERBOSE
</#if>
