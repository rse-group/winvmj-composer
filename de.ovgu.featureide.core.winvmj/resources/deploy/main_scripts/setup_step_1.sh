#!/bin/bash

# Variables
USERNAME=$1
INSTANCE_IP=$2  # Change from INSTANCE_NAME to INSTANCE_IP
FILES_DIRECTORIES=./startup_scripts
VM_ROOT_FILES=/home/$USERNAME
PRODUCT_NAME=$3
CERTIFICATE_NAME=$4
NGINX_CERTIFICATE_NAME=$5

# Ensure SSH key authentication is set up beforehand

# Copy scripts to the VM
scp -o StrictHostKeyChecking=no -r $FILES_DIRECTORIES/setup_before_ready.sh $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Execute the script on the remote VM via SSH
ssh -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo bash $VM_ROOT_FILES/setup_before_ready.sh $USERNAME $PRODUCT_NAME $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME --y"