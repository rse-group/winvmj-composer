#!/bin/bash

# Variables
USERNAME=$1
INSTANCE_IP=$2  # Changed from INSTANCE_NAME to INSTANCE_IP
FILES_DIRECTORIES=./startup_scripts
VM_ROOT_FILES=/home/$USERNAME
PRODUCT_NAME=$3
CERTIFICATE_NAME=$4
NGINX_CERTIFICATE_NAME=$5
PRODUCT_PREFIX=$6

# Ensure SSH key authentication is set up beforehand

# Copy scripts to run
scp -o StrictHostKeyChecking=no \
    $FILES_DIRECTORIES/setup_before_propagate.sh \
    $FILES_DIRECTORIES/setup_after_propagate.sh \
    $FILES_DIRECTORIES/check_if_propagated.sh \
    $FILES_DIRECTORIES/deploy_443.sh \
    $FILES_DIRECTORIES/seeding.sh \
    $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Change permissions for check_if_propagated.sh
ssh -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo chmod 777 $VM_ROOT_FILES/check_if_propagated.sh"

# Run setup_before_propagate.sh on the remote VM
ssh -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo bash $VM_ROOT_FILES/setup_before_propagate.sh $USERNAME $PRODUCT_NAME $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME $PRODUCT_PREFIX --y"
