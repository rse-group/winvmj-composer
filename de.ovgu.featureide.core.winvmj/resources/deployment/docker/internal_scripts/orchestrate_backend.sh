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

# Rabbit MQ variable
RABBITMQ_USER="$8"
RABBITMQ_USER="${RABBITMQ_USER:-guest}"

RABBITMQ_PASS="$9"
RABBITMQ_PASS="${RABBITMQ_PASS:-guest}"

RABBITMQ_HOST="${10}"
RABBITMQ_HOST="${RABBITMQ_HOST:-rabbitmq}"

# DB Variable
DB_URL="${11}"
DB_NAME="${12}"
POSTGRES_USER="${13}"
POSTGRES_PASSWORD="${14}"

IS_REDEPLOYMENT="${15}"

echo "Starting ${SERVICE_FULL_NAME} container"
echo "Main Class is ${SERVICE_NAME}"
echo "Product directory : ${PRODUCT_DIR}"

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
        MAIN_CLASS: $SERVICE_NAME
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
BACKEND_MAIN_CLASS=$SERVICE_NAME
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
