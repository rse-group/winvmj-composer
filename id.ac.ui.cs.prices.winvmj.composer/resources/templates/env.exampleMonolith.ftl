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
OTEL_SERVICE_NAME=${productName?lower_case}

# ============================================
# Observability Stack Ports
# ============================================
HOST_PORT_PROMETHEUS=9090
HOST_PORT_GRAFANA=3000
HOST_PORT_LOKI=3100
HOST_PORT_TEMPO=3200

# ============================================
# Grafana Credentials
# ============================================
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin123
</#if>
