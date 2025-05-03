#!/bin/bash

USERNAME=$1
VM_ROOT_FILES=/home/$USERNAME
PRODUCT_NAME=$2
PRODUCT_DIR=/var/www/products/$PRODUCT_NAME
CERTIFICATE_NAME=$3
NGINX_CERTIFICATE_NAME=$4
PRODUCT_PREFIX=$5

LOG_FILE_LOCATION=$VM_ROOT_FILES/propagated_log.log
ERROR_LOG="${LOG_FILE_LOCATION%.log}_error.log" 

touch "$LOG_FILE" "$ERROR_LOG"

# Perform the curl request and store the status code
sudo systemctl restart systemd-resolved
status_code=$(curl -s -o /dev/null -w "%{http_code}" -m 30 http://$CERTIFICATE_NAME)

# Function to handle errors
error_deployment() {
    echo "Error occurred during deployment" >> "$LOG_FILE_LOCATION"
    echo "[ERROR] An error occurred at $(date)" | tee -a "$ERROR_LOG"
}

# Trap errors and call the error handling function
trap 'error_deployment' ERR

exec 2>>"$ERROR_LOG"

# Check if the status code is 200
if [ "$status_code" == "200" ]; then
    echo "Status 200. Disabling timer and start setup..." >> "$LOG_FILE_LOCATION"
    # Remove the systemd service
    sudo crontab -u $USERNAME -r

    echo "Starting the jobs... after 60s" >> "$LOG_FILE_LOCATION"
    sleep 60
    # Script after propagated
    sudo bash "$VM_ROOT_FILES/setup_after_propagate.sh" "$CERTIFICATE_NAME" "$NGINX_CERTIFICATE_NAME" 2>>"$ERROR_LOG"
    # Deploy
    sudo bash "$VM_ROOT_FILES/deploy_443.sh" "$PRODUCT_NAME" "$PRODUCT_DIR" "$CERTIFICATE_NAME" "$NGINX_CERTIFICATE_NAME" "$PRODUCT_PREFIX" 2>>"$ERROR_LOG"
    
    echo "Finished doing all jobs..." >> "$LOG_FILE_LOCATION"
    
    # DB Seeding
    echo "Seed will be executed after 60 seconds..." >> "$LOG_FILE_LOCATION"
    sleep 60
    sudo bash "$VM_ROOT_FILES/seeding.sh" "$PRODUCT_NAME" "$PRODUCT_PREFIX" 2>>"$ERROR_LOG"
    echo "Seeding done" >> "$LOG_FILE_LOCATION"

    # Add NGINX Configuration to make sure all deployed products is served
    sudo grep -q 'include /etc/nginx/sites-enabled/\*;' /etc/nginx/nginx.conf || sudo sed -i '/http {/a\    include /etc/nginx/sites-enabled/*;' /etc/nginx/nginx.conf

    sudo systemctl restart nginx
else
    echo "Status code is not 200" >> "$LOG_FILE_LOCATION"
fi
