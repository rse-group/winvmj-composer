#!/bin/bash

# Variables
CERTIFICATE_NAME=$1
NGINX_CERTIFICATE_NAME=$2
LOG_FILE_LOCATION=$VM_ROOT_FILES/propagated_log.log

error_deployment() {
    echo "Certificate issue" >> "$LOG_FILE_LOCATION"
}

trap 'error_deployment' ERR

sudo rm /etc/nginx/sites-enabled/$NGINX_CERTIFICATE_NAME
sudo nginx -t
sudo systemctl restart nginx

# Check if certificate name is an IP address (HTTP mode)
if [[ "$CERTIFICATE_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "HTTP-only mode detected (IP: $CERTIFICATE_NAME). Skipping SSL certificate generation." >> "$LOG_FILE_LOCATION"
else
    echo "HTTPS mode detected. Generating SSL certificate for domain: $CERTIFICATE_NAME" >> "$LOG_FILE_LOCATION"
    sudo certbot certonly --nginx --non-interactive -d $CERTIFICATE_NAME --agree-tos --register-unsafely-without-email
fi
