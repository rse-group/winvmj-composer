#!/bin/bash

PRODUCT_NAME=$1
PRODUCT_PREFIX=$2

sleep 60

PRODUCT_FOLDER=/var/www/products/$PRODUCT_NAME/backend/sql
cd $PRODUCT_FOLDER

for sql_file in "$PRODUCT_FOLDER"/*.sql; do
  echo "Processing file: ${sql_file}"
  sudo psql "postgresql://postgres:postgres@localhost:5432/${PRODUCT_PREFIX}_product_$PRODUCT_NAME" -f "$sql_file"
done