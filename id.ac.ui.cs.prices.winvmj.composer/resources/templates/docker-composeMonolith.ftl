# WinVMJ Generated Docker Compose
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
  # OpenTelemetry Collector - verbose logging for testing
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.96.0
    container_name: ${productName?lower_case}-otel-collector
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./otel-collector-config.yaml:/etc/otel-collector-config.yaml:ro
    ports:
      - "4318:4318"
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
      AMANAH_DB_URL: ${"$"}{AMANAH_DB_URL:-jdbc:postgresql://postgres:5432/${productName?lower_case}}
      AMANAH_DB_USERNAME: ${"$"}{AMANAH_DB_USERNAME:-postgres}
      AMANAH_DB_PASSWORD: ${"$"}{AMANAH_DB_PASSWORD:-postgres123}
<#if shouldHaveMonitoring>
      # OTLP endpoint points to collector
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4318
      OTEL_SERVICE_NAME: ${productName?lower_case}
</#if>
    ports:
      - "${"$"}{HOST_PORT_BE:-7776}:7776"
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
