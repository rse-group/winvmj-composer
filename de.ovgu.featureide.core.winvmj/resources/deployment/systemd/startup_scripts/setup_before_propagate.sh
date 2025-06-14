#!/bin/bash

USERNAME=$1
VM_ROOT_FILES=/home/$USERNAME
PRODUCT_NAME=$2
CERTIFICATE_NAME=$3
NGINX_CERTIFICATE_NAME=$4
PRODUCT_PREFIX=$5

# Check if already propagated
sudo crontab -u $USERNAME -l 2>/dev/null | { cat; echo "* * * * * $VM_ROOT_FILES/check_if_propagated.sh $USERNAME $PRODUCT_NAME $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME $PRODUCT_PREFIX"; } | sudo crontab -u $USERNAME -