#!/bin/bash

set -e 
ERROR_LOG="/home/prices-deployment/orchestration_error.log" 

touch "$ERROR_LOG"

exec 2>>"$ERROR_LOG"

PRODUCT_DIR="$1"
NETWORK_NAME="$2"
PRODUCT_NAME="$3"

NUM_BACKENDS=$4
NUM_BACKENDS="${NUM_BACKENDS:-1}"

# DB variable 
POSTGRES_USER="$5"
POSTGRES_USER="${POSTGRES_USER:-postgres}"

POSTGRES_PASSWORD="$6"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

POSTGRES_DB="$7"
POSTGRES_DB="${POSTGRES_DB:-postgres}"

DB_PORT=$8
DB_PORT="${DB_PORT:-5432}"

# Rabbit MQ variable
RABBITMQ_USER="$9"
RABBITMQ_USER="${RABBITMQ_USER:-guest}"

RABBITMQ_PASS="${10}"
RABBITMQ_PASS="${RABBITMQ_PASS:-guest}"

RABBITMQ_PORT=${11}
RABBITMQ_PORT="${RABBITMQ_PORT:-5672}"

RABBITMQ_MANAGEMENT_PORT=${12}
RABBITMQ_MANAGEMENT_PORT="${RABBITMQ_MANAGEMENT_PORT:-15672}"


WITH_DB=false


# Parse arguments
for arg in "$@"; do
  if [[ "$arg" == "--with-db" ]]; then
    WITH_DB=true
  fi
done

set -e

wait_for_healthy() {
  SERVICE_NAME=$1
  echo "Waiting for $SERVICE_NAME to become healthy..."
  for i in {1..30}; do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$SERVICE_NAME" 2>/dev/null || echo "not_found")
    echo "Current status: $STATUS"

    if [[ "$STATUS" == "healthy" ]]; then
      echo "$SERVICE_NAME is healthy."
      return
    elif [[ "$STATUS" == "unhealthy" ]]; then
      echo "$SERVICE_NAME is unhealthy."
      exit 1
    elif [[ "$STATUS" == "not_found" ]]; then
      echo "$SERVICE_NAME container not found."
      exit 1
    fi
    sleep 2
  done
  echo "Timed out waiting for $SERVICE_NAME to become healthy."
  exit 1
}


echo "Create DB container = ${WITH_DB}"

# Start optional DB
if [[ "$WITH_DB" == true ]]; then
  echo "Starting PostgreSQL..."
  DB_COMPOSE="docker-compose.db.yml"
  DB_CONTAINER_NAME="${PRODUCT_NAME}-db-container"


  TEMP_ENV_FILE=$(mktemp)
cat <<EOF > "$TEMP_ENV_FILE"
NET=$NETWORK_NAME
POSTGRES_USER=$POSTGRES_USER
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
POSTGRES_DB=$POSTGRES_DB
DB_PORT=$DB_PORT
DB_CONTAINER_NAME=$DB_CONTAINER_NAME
EOF

  docker compose -f "${PRODUCT_DIR}/$DB_COMPOSE" --env-file "$TEMP_ENV_FILE" up -d --build 2>>"$ERROR_LOG"

  rm -f "$TEMP_ENV_FILE"

  wait_for_healthy "$DB_CONTAINER_NAME"
  echo "CREATING DB FINISHED..."
fi


if [[ "$NUM_BACKENDS" -gt 1 ]]; then
  echo "Starting RabbitMQ..."

  RABBITMQ_CONTAINER_NAME="${PRODUCT_NAME}-rabbitmq-container"
  RABBITMQ_SERVICE_NAME="${PRODUCT_NAME}_rabbitmq"
  RABBIT_COMPOSE_FILE="$PRODUCT_DIR/docker-compose.rabbitmq.yml"
  RABBITMQ_ERLANG_COOKIE=$(echo -n "$RABBITMQ_SERVICE_NAME" | sha256sum | cut -c1-32)

cat <<EOF > "$RABBIT_COMPOSE_FILE"
services:
  $RABBITMQ_SERVICE_NAME:
    image: rabbitmq:4-management
    container_name: $RABBITMQ_CONTAINER_NAME
    ports:
      - "${RABBITMQ_PORT}:5672"
      - "${RABBITMQ_MANAGEMENT_PORT}:15672"
    environment:
      RABBITMQ_DEFAULT_USER: $RABBITMQ_USER
      RABBITMQ_DEFAULT_PASS: $RABBITMQ_PASS
      RABBITMQ_ERLANG_COOKIE: $RABBITMQ_ERLANG_COOKIE
    volumes:
      - "${RABBITMQ_SERVICE_NAME}_data:/var/lib/rabbitmq"
    networks:
      - app_network
    healthcheck:
      test: ["CMD-SHELL", "rabbitmq-diagnostics check_running -q"]
      interval: 5s
      timeout: 5s
      retries: 5
    restart: always

volumes:
  ${RABBITMQ_SERVICE_NAME}_data:

networks:
  app_network:
    external: true
    name: $NETWORK_NAME
EOF

  RABBIT_ENV_FILE=$(mktemp)
  cat <<EOF > "$RABBIT_ENV_FILE"
NET=$NETWORK_NAME
RABBITMQ_USER=$RABBITMQ_USER
RABBITMQ_PASS=$RABBITMQ_PASS
RABBITMQ_PORT=$RABBITMQ_PORT
RABBITMQ_MANAGEMENT_PORT=$RABBITMQ_MANAGEMENT_PORT
RABBITMQ_CONTAINER_NAME=$RABBITMQ_CONTAINER_NAME
EOF

  docker compose -f "${PRODUCT_DIR}/docker-compose.rabbitmq.yml" --env-file "$RABBIT_ENV_FILE" up -d --build --force-recreate 2>>"$ERROR_LOG"
  rm -f "$RABBIT_ENV_FILE"

  wait_for_healthy $RABBITMQ_CONTAINER_NAME
  echo "CREATING RABBITMQ FINISHED..."
fi