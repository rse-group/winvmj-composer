#!/bin/bash

# Variables
USERNAME=$1
INSTANCE_IP=$2  # Changed from INSTANCE_NAME to INSTANCE_IP
FILES_DIRECTORIES=./startup_scripts
PRODUCT_NAME=$3
PRODUCT_DIR=/var/www/products/$PRODUCT_NAME
VM_ROOT_FILES=/home/$USERNAME
CERTIFICATE_NAME=$4
NGINX_CERTIFICATE_NAME=$5
PRODUCT_PREFIX=$6

# Ensure SSH key authentication is set up beforehand

# Upload deploy script to the remote VM
scp -o StrictHostKeyChecking=no $FILES_DIRECTORIES/deploy_443.sh $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Run the deploy script on the remote VM
ssh -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo bash $VM_ROOT_FILES/deploy_443.sh $PRODUCT_NAME $PRODUCT_DIR $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME $PRODUCT_PREFIX"