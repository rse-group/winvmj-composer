#!/bin/bash

# Variables
USERNAME=$1
INSTANCE_IP=$2  # Changed from INSTANCE_NAME to INSTANCE_IP
FILES_DIRECTORIES=./internal_scripts
VM_ROOT_FILES=/home/$USERNAME
PRODUCT_NAME=$3
PRODUCT_PREFIX=$4
PRIVATE_KEY_PATH=$5
NUM_BACKENDS=$6
PRODUCT_DIR=/var/www/products/$PRODUCT_NAME
CERTIFICATE_NAME=$7
NGINX_CERTIFICATE_NAME=$8

# Ensure SSH key authentication is set up beforehand

# Copy scripts to run
scp -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no \
    $FILES_DIRECTORIES/deploy_micro.sh \
    $FILES_DIRECTORIES/orchestrate_backend.sh \
    $FILES_DIRECTORIES/orchestrate_container.sh \
    $FILES_DIRECTORIES/orchestrate_rabbitmq_db.sh \
    $FILES_DIRECTORIES/check_if_propagated.sh \
    $FILES_DIRECTORIES/setup_after_propagate.sh \
    $FILES_DIRECTORIES/setup_before_propagate.sh \
    $FILES_DIRECTORIES/seeding.sh \
    $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Change permissions for check_if_propagated.sh
ssh -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo chown $USERNAME:$USERNAME $VM_ROOT_FILES/*.sh && sudo chmod 700 $VM_ROOT_FILES/*.sh"

# # Run setup_before_propagate.sh on the remote VM
ssh  -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo bash $VM_ROOT_FILES/setup_before_propagate.sh $USERNAME $PRODUCT_NAME $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME $PRODUCT_PREFIX $NUM_BACKENDS --y"

# ssh  -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo bash $VM_ROOT_FILES/deploy_micro.sh $PRODUCT_NAME $PRODUCT_DIR $NUM_BACKENDS $PRODUCT_PREFIX"    