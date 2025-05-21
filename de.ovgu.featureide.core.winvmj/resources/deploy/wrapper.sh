#!/bin/bash

file "$0" | grep CRLF && echo "Warning: This script has CRLF line endings!" && exit 1

# Variables Mapping
declare -A GCP_MACHINE_TYPE_MAP=(
    [SMALL]=e2-small
    [MEDIUM]=e2-medium
    [LARGE]=e2-standard-2
)
declare -A GCP_ZONE_MAP=(
    [US]=us-central1-a
    [SINGAPORE]=asia-southeast1-b
    [JAKARTA]=asia-southeast2-a
)
declare -A AWS_MACHINE_TYPE_MAP=(
    [SMALL]=t2.small
    [MEDIUM]=t2.medium
    [LARGE]=t2.large
)
declare -A AWS_ZONE_MAP=(
    [US]=us-east-1
    [SINGAPORE]=ap-southeast-1
    [EUROPE]=eu-central-1
)

# Set Variables
PRODUCT_NAME=$4
CERTIFICATE_NAME=$5
NGINX_CERTIFICATE_NAME=$6
CREDENTIALS=$7
PROVIDER=$8
INSTANCE_NAME=$9
PRODUCT_PREFIX=${10}
PRODUCT_DIR=${11}
PUBLIC_KEY=${12}
PRIVATE_KEY_PATH=${13}

# Set Other Variables based on Provider selected
if [ "$PROVIDER" == "aws" ]; then
    USERNAME="ubuntu"
    MACHINE_TYPE=${AWS_MACHINE_TYPE_MAP[$2]}
    ZONE=${AWS_ZONE_MAP[$3]}
elif [ "$PROVIDER" == "gcp" ]; then
    USERNAME=$1
    MACHINE_TYPE=${GCP_MACHINE_TYPE_MAP[$2]}
    ZONE=${GCP_ZONE_MAP[$3]}
else
  echo "Error: Unsupported provider!"
  exit 1
fi

# Validasi file credentials
if [ ! -f "$CREDENTIALS" ]; then
  echo "Error: Credential file not found at $CREDENTIALS"
  exit 1
fi

# Validasi file produk
if [ ! -f "$PRODUCT_DIR" ]; then
  echo "Error: Product not found at $PRODUCT_DIR"
  exit 1
fi

# Validasi file public key
if [ ! -f "$PUBLIC_KEY" ]; then
  echo "Error: Product not found at $PUBLIC_KEY"
  exit 1
fi

if [ ! -f "$PRIVATE_KEY_PATH" ] || [ ! -r "$PRIVATE_KEY_PATH" ]; then
  echo "Error: Private key $PRIVATE_KEY_PATH not found or not readable!"
  exit 1
fi

echo "Using username: $USERNAME for provider: $PROVIDER"

#
# Divided into 2 steps
# 1: Before user register the IP to the domain register
# 2: After that

# 1

set -e # Exit immediately if any command fails

# Get the directory of the current script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# set permission
chmod +x terraform/create_vm.sh
chmod +x main_scripts/setup_step_1.sh
chmod +x main_scripts/initial_setup.sh
chmod +x main_scripts/initial_setup_2.sh
chmod +x main_scripts/before_propagate.sh

# Create VM
# e.g. ./main_scripts/create_vm.sh ubuntu t2.medium ap-southeast-1 aws amanah-instance-aws
./terraform/create_vm.sh $USERNAME $MACHINE_TYPE $ZONE $CREDENTIALS $PROVIDER $INSTANCE_NAME $PUBLIC_KEY

echo "Please wait for a minute to make sure your instance is ready..."
sleep 60

INSTANCE_IP=$(cat instance_ip.txt)

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
read -p "You can exit now..."

# Deploy product
# ./wrapper.sh rikza MEDIUM JAKARTA hightide hightide.rikza.net hightide.rikza terraform-sa.json gcp amanah-instance-gcp
# OR
# ./wrapper.sh ubuntu MEDIUM SINGAPORE procom procom.rikza.net procom.rikza aws-key.json aws amanah-instance-aws
