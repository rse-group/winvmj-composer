#!/bin/bash

# Variables
USERNAME=$1
CERTIFICATE_NAME=$2
NGINX_CERTIFICATE_NAME=$3

# Set VM_ROOT_FILES based on the username
VM_ROOT_FILES=/home/$USERNAME
LOG_FILE_LOCATION=$VM_ROOT_FILES/propagated_log.log

error_deployment() {
    echo "Certificate issue" >> "$LOG_FILE_LOCATION"
}

trap 'error_deployment' ERR

# Remove nginx config file if it exists
if [ -f "/etc/nginx/sites-enabled/$NGINX_CERTIFICATE_NAME" ]; then
    sudo rm /etc/nginx/sites-enabled/$NGINX_CERTIFICATE_NAME
    echo "Removed existing nginx config: $NGINX_CERTIFICATE_NAME" >> "$LOG_FILE_LOCATION"
else
    echo "No existing nginx config to remove: $NGINX_CERTIFICATE_NAME" >> "$LOG_FILE_LOCATION"
fi

sudo nginx -t
sudo systemctl restart nginx

# Check if CERTIFICATE_NAME is an IP address
if [[ $CERTIFICATE_NAME =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Using IP address ($CERTIFICATE_NAME). Skipping SSL certificate generation." >> "$LOG_FILE_LOCATION"
    echo "HTTP-only deployment mode." >> "$LOG_FILE_LOCATION"
else
    echo "Using domain name ($CERTIFICATE_NAME). Generating SSL certificate..." >> "$LOG_FILE_LOCATION"
    sudo certbot certonly --nginx --non-interactive -d $CERTIFICATE_NAME --agree-tos --register-unsafely-without-email
fi
