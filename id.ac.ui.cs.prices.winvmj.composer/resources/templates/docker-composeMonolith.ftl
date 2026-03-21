# WinVMJ Generated Docker Compose (Local Dev)
# Product: ${productName}

version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ${productName?lower_case}-postgres
    environment:
      POSTGRES_DB: ${productName?lower_case}
      POSTGRES_USER: ${"$"}{AMANAH_DB_USERNAME:-postgres}
      POSTGRES_PASSWORD: ${"$"}{AMANAH_DB_PASSWORD:-postgres123}
    ports:
      - "${"$"}{HOST_PORT_POSTGRES:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - ${productName?lower_case}-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

<#if shouldHaveMonitoring>
  # ============ Observability Stack ============

  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.120.0
    container_name: ${productName?lower_case}-otel-collector
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./otel-collector-config.yaml:/etc/otel-collector-config.yaml:ro
    ports:
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
    networks:
      - ${productName?lower_case}-network
    depends_on:
      - prometheus
      - loki
      - tempo
    restart: unless-stopped

  prometheus:
    image: prom/prometheus:v3.4.0
    container_name: ${productName?lower_case}-prometheus
    user: "0"
    command:
      - "--config.file=/etc/prometheus/prometheus.yml"
      - "--storage.tsdb.path=/prometheus"
      - "--web.enable-lifecycle"
      - "--web.enable-remote-write-receiver"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    ports:
      - "${"$"}{HOST_PORT_PROMETHEUS:-9090}:9090"
    networks:
      - ${productName?lower_case}-network
    restart: unless-stopped

  grafana:
    image: grafana/grafana:11.6.0
    container_name: ${productName?lower_case}-grafana
    user: "0"
    environment:
      - GF_SECURITY_ADMIN_USER=${"$"}{GRAFANA_ADMIN_USER:-admin}
      - GF_SECURITY_ADMIN_PASSWORD=${"$"}{GRAFANA_ADMIN_PASSWORD:-admin123}
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana-datasources.yaml:/etc/grafana/provisioning/datasources/datasources.yaml:ro
      - ./grafana-dashboards.yaml:/etc/grafana/provisioning/dashboards/dashboards.yaml:ro
      - ./grafana-dashboard.json:/var/lib/grafana/dashboards/monitoring.json:ro
    ports:
      - "${"$"}{HOST_PORT_GRAFANA:-3000}:3000"
    networks:
      - ${productName?lower_case}-network
    depends_on:
      - prometheus
      - loki
      - tempo
    restart: unless-stopped

  loki:
    image: grafana/loki:3.5.0
    container_name: ${productName?lower_case}-loki
    user: "0"
    command: -config.file=/etc/loki/loki-config.yaml
    volumes:
      - ./loki-config.yaml:/etc/loki/loki-config.yaml:ro
      - loki_data:/loki
    ports:
      - "${"$"}{HOST_PORT_LOKI:-3100}:3100"
    networks:
      - ${productName?lower_case}-network
    restart: unless-stopped

  tempo:
    image: grafana/tempo:2.7.0
    container_name: ${productName?lower_case}-tempo
    user: "0"
    command: ["-config.file=/etc/tempo/tempo-config.yaml"]
    volumes:
      - ./tempo-config.yaml:/etc/tempo/tempo-config.yaml:ro
      - tempo_data:/tmp/tempo
    ports:
      - "${"$"}{HOST_PORT_TEMPO:-3200}:3200"
    networks:
      - ${productName?lower_case}-network
    restart: unless-stopped

</#if>
  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ${productName?lower_case}-backend
    environment:
      AMANAH_HOST_BE: ${"$"}{AMANAH_HOST_BE:-0.0.0.0}
      AMANAH_PORT_BE: ${"$"}{AMANAH_PORT_BE:-7776}
      AMANAH_DB_URL: ${"$"}{AMANAH_DB_URL:-jdbc:postgresql://postgres:5432/${productName?lower_case}}
      AMANAH_DB_USERNAME: ${"$"}{AMANAH_DB_USERNAME:-postgres}
      AMANAH_DB_PASSWORD: ${"$"}{AMANAH_DB_PASSWORD:-postgres123}
<#if shouldHaveMonitoring>
      # OpenTelemetry monitoring
      OTEL_EXPORTER_OTLP_ENDPOINT: ${"$"}{OTEL_EXPORTER_OTLP_ENDPOINT:-http://otel-collector:4318}
      OTEL_SERVICE_NAME: ${"$"}{OTEL_SERVICE_NAME:-${productName?lower_case}}
</#if>
    ports:
      - "${"$"}{HOST_PORT_BE:-7776}:${"$"}{AMANAH_PORT_BE:-7776}"
    networks:
      - ${productName?lower_case}-network
    depends_on:
      postgres:
        condition: service_healthy
<#if shouldHaveMonitoring>
      otel-collector:
        condition: service_started
</#if>
    restart: unless-stopped

networks:
  ${productName?lower_case}-network:
    driver: bridge

volumes:
  postgres_data:
<#if shouldHaveMonitoring>
  prometheus_data:
  grafana_data:
  loki_data:
  tempo_data:
</#if>
