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

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ${productName?lower_case}-backend
    environment:
      # Internal ports are fixed (7776, 9464) - configured in Dockerfile
      AMANAH_DB_URL: ${"$"}{AMANAH_DB_URL:-jdbc:postgresql://postgres:5432/${productName?lower_case}}
      AMANAH_DB_USERNAME: ${"$"}{AMANAH_DB_USERNAME:-postgres}
      AMANAH_DB_PASSWORD: ${"$"}{AMANAH_DB_PASSWORD:-postgres123}
    ports:
      # HOST_PORT:CONTAINER_PORT (container port fixed at 7776)
      - "${"$"}{HOST_PORT_BE:-7776}:7776"
<#if shouldHaveMonitoring>
      - "${"$"}{HOST_PORT_MONITORING:-9464}:9464"
</#if>
    networks:
      - ${productName?lower_case}-network
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

networks:
  ${productName?lower_case}-network:
    driver: bridge

volumes:
  postgres_data:
