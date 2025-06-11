#!/bin/bash

# Variables
USERNAME=$1
INSTANCE_IP=$2  # Changed from INSTANCE_NAME to INSTANCE_IP
FILES_DIRECTORIES=./internal_scripts
DOCKERFILES_DIRECTORY=./docker_config
PRODUCT_NAME=$3
PRODUCT_PATH=/var/www/products
LIBRARY_PATH=/home/prices-deployment/nix-environment/
VM_ROOT_FILES=/home/$USERNAME
PRODUCT_DIRECTORY=$4
PRIVATE_KEY_PATH=$5

# Ensure SSH key authentication is set up beforehand

# Copy the setup script to the VM
scp -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no -r $FILES_DIRECTORIES/setup_environment.sh $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Run the setup script on the remote VM
ssh -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo bash $VM_ROOT_FILES/setup_environment.sh --y"

# Upload product files
scp -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no $PRODUCT_DIRECTORY \
    $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Unzip product files on the remote VM
ssh -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo unzip -o $VM_ROOT_FILES/$PRODUCT_NAME.zip -d $PRODUCT_PATH"

# Copy and move docker configs
scp -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no \
    $DOCKERFILES_DIRECTORY/docker-compose.base.yml \
    $DOCKERFILES_DIRECTORY/docker-compose.db.yml \
    $DOCKERFILES_DIRECTORY/Dockerfile.backend \
    $DOCKERFILES_DIRECTORY/Dockerfile.frontend \
    $DOCKERFILES_DIRECTORY/Dockerfile.apigateway \
    $USERNAME@$INSTANCE_IP:/tmp/

ssh -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "
    sudo mv /tmp/docker-compose.* $PRODUCT_PATH/$PRODUCT_NAME &&
    sudo mv /tmp/Dockerfile.* $PRODUCT_PATH/$PRODUCT_NAME 
"