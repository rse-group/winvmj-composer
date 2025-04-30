#!/bin/bash

# Variables
USERNAME=$1
INSTANCE_IP=$2  # Changed from INSTANCE_NAME to INSTANCE_IP
FILES_DIRECTORIES=./startup_scripts
ADDITIONAL_SCRIPTS_DIRECTORY=./additional_scripts
VM_ROOT_FILES=/home/$USERNAME

# Ensure SSH key authentication is set up beforehand

# Copy the port_reserver.py script to the VM
scp -o StrictHostKeyChecking=no $ADDITIONAL_SCRIPTS_DIRECTORY/port_reserver.py $USERNAME@$INSTANCE_IP:$VM_ROOT_FILES

# Move the script to the desired location on the remote VM
ssh -o StrictHostKeyChecking=no $USERNAME@$INSTANCE_IP "sudo mv $VM_ROOT_FILES/port_reserver.py /home/prices-deployment/port_reserver/"
