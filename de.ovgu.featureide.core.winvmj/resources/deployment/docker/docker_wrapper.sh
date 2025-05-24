#!/bin/bash

# Wrapper Script to Run environment.sh and deploy_micro.sh

# Get the directory of the current script and set it as current dir
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Fail on any error
set -e

# Load product name and directory from arguments
USERNAME="$1"
IP_ADDRESS="$2"
PRODUCT_NAME="$3"
CERTIFICATE_NAME="$4"
NGINX_CERTIFICATE_NAME="$5"
PRODUCT_PREFIX="$6"
PRODUCT_DIR="$7"
PRIVATE_KEY_PATH="$8"
NUM_BACKENDS=$9

DB_URL="${10}"
DB_USERNAME="${11}"
DB_PASSWORD="${12}"
RABBITMQ_HOST="${13}"
RABBITMQ_USER="${14}"
RABBITMQ_PASS="${15}"

if [ -z "$PRODUCT_NAME" ] || [ -z "$PRODUCT_DIR" ]; then
  echo "Usage: $0 <product_name> <product_directory> [other args...]"
  exit 1
fi

if [ ! -f "$PRODUCT_DIR" ]; then
  echo "Error: Product not found at $PRODUCT_DIR"
  exit 1
fi

if [ ! -f "$PRIVATE_KEY_PATH" ] || [ ! -r "$PRIVATE_KEY_PATH" ]; then
  echo "Error: Private key $PRIVATE_KEY_PATH not found or not readable!"
  exit 1
fi

chmod +x ./external_scripts/copy_and_run_script.sh
chmod +x ./external_scripts/copy_port_reserver.sh
chmod +x ./external_scripts/initial_setup.sh

# scp -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no /mnt/d/Ngampus/TA/repo/FullFeature/postman/* $USERNAME@$IP_ADDRESS:/home/rikza/postman
# Run environment setup and copy folders
echo "Running initial_setup.sh to install dependencies and copy products..."
./external_scripts/initial_setup.sh $USERNAME $IP_ADDRESS $PRODUCT_NAME $PRODUCT_DIR $PRIVATE_KEY_PATH

# Copy port reserver
echo "Running copy_port_reserver.sh to install dependencies and copy products..."
./external_scripts/copy_port_reserver.sh $USERNAME $IP_ADDRESS $PRIVATE_KEY_PATH

# Wait for user to confirm DNS setup
echo "=========================================================================="
echo "==========================IMPORTANT!!!!!=================================="
echo "Please set up the DNS A record to point $CERTIFICATE_NAME to $INSTANCE_IP."
echo "You can verify it by running: nslookup $CERTIFICATE_NAME"
echo "The process will continue in a few second"
sleep 30

# Run Script
echo "Running Installation Script..."
./external_scripts/copy_and_run_script.sh $USERNAME $IP_ADDRESS $PRODUCT_NAME $PRODUCT_PREFIX $PRIVATE_KEY_PATH $NUM_BACKENDS $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME


echo "Run deployment on server, check status on /home/{USER}/propagated_log.log"