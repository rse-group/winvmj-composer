#!/bin/bash

LOG_FILE_LOCATION=/home/prices-deployment/seeding_log.log
ERROR_LOG=/home/prices-deployment/seeding_error_log.log

touch "$LOG_FILE" "$ERROR_LOG"

# Function to handle errors
error_seeding() {
    echo "Error occurred during seeding" >> "$LOG_FILE_LOCATION"
    echo "[ERROR] An error occurred at $(date)" | tee -a "$ERROR_LOG"
}

# Trap errors and call the error handling function
trap 'error_seeding' ERR

exec 2>>"$ERROR_LOG"


db_name=$1
service_name=$2
product_dir=$3
DB_USERNAME=$4
DB_URL=$5
DB_PASSWORD=$6

echo "Seeding database for $service_name.."  >> "$LOG_FILE_LOCATION"

for sql_file in $product_dir/$service_name/sql/*.sql; do
    echo "Seeding: $sql_file" >> $LOG_FILE_LOCATION
    PGPASSWORD="$DB_PASSWORD" psql -U $DB_USERNAME -h ${DB_URL%%:*} -p ${DB_URL##*:} -d "$db_name" -f "$sql_file" 2>> "$ERROR_LOG"  
done
