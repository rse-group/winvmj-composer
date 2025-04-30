#!/bin/bash

# Variables
USERNAME=$1
INSTANCE_IP=$2  # Changed from INSTANCE_NAME to INSTANCE_IP
FILES_DIRECTORIES=./startup_scripts
PRODUCT_NAME=$3
PRODUCT_PATH=/var/www/products
LIBRARY_PATH=/home/prices-deployment/nix-environment/
VM_ROOT_FILES=/home/$USERNAME
NGINX_CERTIFICATE_NAME=$4
LIBRARY_DIRECTORY=./products
PRODUCT_DIRECTORY=$5

# Ensure SSH key authentication is set up beforehand

# Copy the setup script to the VM
scp -o StrictHostKeyChecking=no -r $FILES_DIRECTORIES/setup_environment.sh $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Run the setup script on the remote VM
ssh -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo bash $VM_ROOT_FILES/setup_environment.sh $NGINX_CERTIFICATE_NAME --y"

# Upload product files
scp -o StrictHostKeyChecking=no $PRODUCT_DIRECTORY \
    $LIBRARY_DIRECTORY/prices_product_libraries.zip \
    $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Unzip product files on the remote VM
ssh -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo unzip -o $VM_ROOT_FILES/$PRODUCT_NAME.zip -d $PRODUCT_PATH"
ssh -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo unzip -o $VM_ROOT_FILES/prices_product_libraries.zip -d $LIBRARY_PATH"
