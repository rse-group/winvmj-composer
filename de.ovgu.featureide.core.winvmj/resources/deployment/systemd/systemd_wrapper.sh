#!/bin/bash

file "$0" | grep CRLF && echo "Warning: This script has CRLF line endings!" && exit 1

# Get the directory of the current script and set it as current dir
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Set Variables
USERNAME=$1
INSTANCE_IP=$2
PRODUCT_NAME=$3
CERTIFICATE_NAME=$4
NGINX_CERTIFICATE_NAME=$5
PRODUCT_PREFIX=$6
PRODUCT_DIR=$7
PRIVATE_KEY_PATH=$8


if [ ! -f "$PRODUCT_DIR" ]; then
  echo "Error: Product not found at $PRODUCT_DIR"
  exit 1
fi

if [ ! -f "$PRIVATE_KEY_PATH" ] || [ ! -r "$PRIVATE_KEY_PATH" ]; then
  echo "Error: Private key $PRIVATE_KEY_PATH not found or not readable!"
  exit 1
fi

echo "Using username: $USERNAME"

#
# Divided into 2 steps
# 1: Before user register the IP to the domain register
# 2: After that

# 1

set -e # Exit immediately if any command fails

# set permission
chmod +x ./main_scripts/setup_step_1.sh
chmod +x ./main_scripts/initial_setup.sh
chmod +x ./main_scripts/initial_setup_2.sh
chmod +x ./main_scripts/before_propagate.sh

# Initial setup before deployment ready
# e.g. ./main_scripts/setup_step_1.sh ubuntu 35.21.12.240 hightide hightide.rikza.net hightide.rikza
./main_scripts/setup_step_1.sh $USERNAME $INSTANCE_IP $PRODUCT_NAME $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME $PRIVATE_KEY_PATH

# 2

# Wait for user to confirm DNS setup
echo "=========================================================================="
echo "==========================IMPORTANT!!!!!=================================="
echo "Please set up the DNS A record to point $CERTIFICATE_NAME to $INSTANCE_IP."
echo "You can verify it by running: nslookup $CERTIFICATE_NAME"
echo "The process will continue in a few second"
sleep 30

# Initial setup in VM
# e.g. ./main_scripts/initial_setup.sh ubuntu 35.21.12.240 hightide hightide.rikza.net
./main_scripts/initial_setup.sh $USERNAME $INSTANCE_IP $PRODUCT_NAME $CERTIFICATE_NAME $PRODUCT_DIR $PRIVATE_KEY_PATH
# Additional necessary setup in VM
# e.g. ./main_scripts/initial_setup_2.sh ubuntu 35.21.12.240 
./main_scripts/initial_setup_2.sh $USERNAME $INSTANCE_IP $PRIVATE_KEY_PATH

# Before propagate
# ./main_scripts/before_propagate.sh ubuntu 35.21.12.240 hightide hightide.rikza.net hightide.rikza
./main_scripts/before_propagate.sh $USERNAME $INSTANCE_IP $PRODUCT_NAME $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME $PRODUCT_PREFIX $PRIVATE_KEY_PATH


echo "Finished! It may takes few minutes for the application to run"
echo "Check directory /home/{USER} and look for propagated_log.log file to check deployment status"
echo "Check propagated_log_error.log file to check error message"
