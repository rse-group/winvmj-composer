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

sudo rm /etc/nginx/sites-enabled/$NGINX_CERTIFICATE_NAME
sudo nginx -t
sudo systemctl restart nginx
sudo certbot certonly --nginx --non-interactive -d $CERTIFICATE_NAME --agree-tos --register-unsafely-without-email
