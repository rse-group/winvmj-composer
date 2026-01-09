#!/bin/bash

set -e 
ERROR_LOG="/home/prices-deployment/orchestration_error.log" 

touch "$ERROR_LOG"

exec 2>>"$ERROR_LOG"


PRODUCT_DIR="$1"
NETWORK_NAME="$2"

PRODUCT_NAME="$3"
PRODUCT_LINE="$4"
SERVICE_NAME="$5"

SERVICE_FULL_NAME="$6"
BE_PORT="$7"

# Determine the correct main class name for Docker build
# For monolith deployments, use SERVICE_FULL_NAME which contains the full directory path
# For microservice deployments, use SERVICE_NAME
if [ "$SERVICE_NAME" = "$PRODUCT_NAME" ]; then
  # Monolith deployment: use the full service name for directory path
  MAIN_CLASS_NAME="$SERVICE_FULL_NAME"
  echo "Monolith deployment detected. Using SERVICE_FULL_NAME: $MAIN_CLASS_NAME"
else
  # Microservice deployment: use the individual service name
  MAIN_CLASS_NAME="$SERVICE_NAME"
  echo "Microservice deployment detected. Using SERVICE_NAME: $MAIN_CLASS_NAME"
fi

# Rabbit MQ variable
RABBITMQ_USER="$8"
RABBITMQ_USER="${RABBITMQ_USER:-guest}"

RABBITMQ_PASS="$9"
RABBITMQ_PASS="${RABBITMQ_PASS:-guest}"

RABBITMQ_HOST="${10}"
RABBITMQ_HOST="${RABBITMQ_HOST:-${PRODUCT_NAME}_rabbitmq}"

# DB Variable
DB_URL="${11}"
DB_NAME="${12}"
POSTGRES_USER="${13}"
POSTGRES_PASSWORD="${14}"

IS_REDEPLOYMENT="${15}"

echo "Starting ${SERVICE_FULL_NAME} container"
echo "Main Class is ${SERVICE_NAME}"
echo "Main Class Name (corrected) is ${MAIN_CLASS_NAME}"
echo "Product Line is ${PRODUCT_LINE}"
echo "Product Name is ${PRODUCT_NAME}"
echo "Product directory : ${PRODUCT_DIR}"
echo "SERVICE_FULL_NAME is ${SERVICE_FULL_NAME}"
echo "Docker build args will be: PRODUCTLINE=${PRODUCT_LINE}, MAIN_CLASS=${MAIN_CLASS_NAME}"

# Clean up existing backend containers to avoid conflicts
echo "Cleaning up existing backend containers..."
BACKEND_CONTAINER_NAME="${SERVICE_FULL_NAME}-container"
docker stop "$BACKEND_CONTAINER_NAME" 2>/dev/null || true
docker rm "$BACKEND_CONTAINER_NAME" 2>/dev/null || true

# Also clean up any containers with similar names
docker ps -a --filter "name=${PRODUCT_NAME}.*backend" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true
docker ps -a --filter "name=${SERVICE_FULL_NAME}" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true

echo "Backend container cleanup completed"

SERVICE_FULL_NAME_LOWER=$(echo "$SERVICE_FULL_NAME" | tr 'A-Z' 'a-z')
COMPOSE_FILE="$PRODUCT_DIR/docker-compose.backend.yml"

PORT_FORWARDING="$BE_PORT:$BE_PORT"
echo "Ports ${PORT_FORWARDING}"


cat <<EOF > "$COMPOSE_FILE"
services:
  $SERVICE_FULL_NAME_LOWER:
    build:
      context: .
      dockerfile: Dockerfile.backend
      args:
        PRODUCTLINE: $PRODUCT_LINE
        MAIN_CLASS: $MAIN_CLASS_NAME
    container_name: $SERVICE_FULL_NAME
    restart: always
    environment:
      AMANAH_PORT_BE: $BE_PORT
      AMANAH_DB_URL: jdbc:postgresql://$DB_URL/$DB_NAME
      AMANAH_DB_USERNAME: $POSTGRES_USER
      AMANAH_DB_PASSWORD: $POSTGRES_PASSWORD
      APP_ID: $SERVICE_FULL_NAME
      RABBITMQ_HOST: $RABBITMQ_HOST
      RABBITMQ_USER: $RABBITMQ_USER
      RABBITMQ_PASS: $RABBITMQ_PASS
    ports:
      - ${PORT_FORWARDING}
    networks:
      - app_network

networks:
  app_network:
    external: true
    name: $NETWORK_NAME
EOF

TEMP_ENV_FILE=$(mktemp)
cat <<EOF > "$TEMP_ENV_FILE"
NET=$NETWORK_NAME
PRODUCTLINE=$PRODUCT_LINE
BACKEND_MAIN_CLASS=$MAIN_CLASS_NAME
BACKEND_CONTAINER_NAME=$SERVICE_FULL_NAME
AMANAH_PORT_BE=$BE_PORT
AMANAH_DB_URL=jdbc:postgresql://$DB_URL/$DB_NAME
POSTGRES_USER=$POSTGRES_USER
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
APP_ID=$SERVICE_FULL_NAME
RABBITMQ_HOST=$RABBITMQ_HOST
RABBITMQ_USER=$RABBITMQ_USER
RABBITMQ_PASS=$RABBITMQ_PASS
EOF

docker compose -f "$COMPOSE_FILE" --env-file "$TEMP_ENV_FILE" up -d --build --force-recreate 2>>"$ERROR_LOG"

rm -f "$TEMP_ENV_FILE"

echo "Finished creating ${SERVICE_FULL_NAME} container"
