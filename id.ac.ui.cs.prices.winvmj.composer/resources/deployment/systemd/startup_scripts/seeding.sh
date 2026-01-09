#!/bin/bash

PRODUCT_NAME=$1
PRODUCT_PREFIX=$2
DB_URL="$3"
DB_URL="${DB_URL:-localhost:5432}"

DB_USERNAME="$4"
DB_USERNAME="${DB_USERNAME:-postgres}"

DB_PASSWORD="$5"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

sleep 60

PRODUCT_FOLDER=/var/www/products/$PRODUCT_NAME/backend/sql
cd $PRODUCT_FOLDER

for sql_file in "$PRODUCT_FOLDER"/*.sql; do
  echo "Processing file: ${sql_file}"
  sudo psql "postgresql://$DB_USERNAME:$DB_PASSWORD@$DB_URL/${PRODUCT_PREFIX}_product_$PRODUCT_NAME" -f "$sql_file"
done