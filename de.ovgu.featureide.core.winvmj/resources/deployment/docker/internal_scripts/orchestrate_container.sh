#!/bin/bash
set -e 
ERROR_LOG="/home/prices-deployment/orchestration_error.log" 

touch "$ERROR_LOG"

exec 2>>"$ERROR_LOG"

PRODUCT_DIR="$1"
ENV_FILE_PATH="$2"
WITH_API_GATEWAY="$3"

# Start Frontend & API Gateway (if needed)

if [[ "$WITH_API_GATEWAY" == true ]]; then
    echo "Starting Frontend and API Gateway..." >> "$ERROR_LOG"
    docker compose -f ${PRODUCT_DIR}/docker-compose.base.yml --env-file "$ENV_FILE_PATH" up -d --build --force-recreate  2>>"$ERROR_LOG"
else
    echo "Starting Frontend..." >> "$ERROR_LOG"
    docker compose -f ${PRODUCT_DIR}/docker-compose.base.yml --env-file "$ENV_FILE_PATH" up -d --build --force-recreate frontend  2>>"$ERROR_LOG"
fi


echo "All services are up and running."
