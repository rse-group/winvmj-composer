#!/bin/bash

# Variables
USERNAME=$1
MACHINE_TYPE=$2
ZONE=$3
CREDENTIALS=$4
PROVIDER=$5
INSTANCE_NAME=$6
PUBLIC_KEY=$7

# Save current directory
current_dir=$(pwd)

echo "Selected provider is $PROVIDER"

# Change to the correct Terraform deployment directory
cd "$(dirname "$0")/../terraform/$PROVIDER" || { echo "Error: Terraform directory not found!"; exit 1; }

# Check if .tf files exist
if ! ls *.tf >/dev/null 2>&1; then
  echo "Error: No Terraform configuration files found in $(pwd)!"
  exit 1
fi

# Error handling func
resource_provisioning_error() {
  echo "Error: Terraform apply failed! Checking for existing resources..."

  # Check if the error is due to existing resources
  if echo "$ERR_OUTPUT" | grep -qE "already exists|A resource with ID| alreadyExists"; then
    echo "Error: Detected existing resources that are not in Terraform state!"
  else
    echo "Unknown Terraform error occurred."
    echo "$ERR_OUTPUT"
  fi

  exit 1
}

# Initialize Terraform
terraform init

trap 'resource_provisioning_error' ERR

echo "Creating resource ..."

# Apply Terraform configuration based on selected provider
if [ "$PROVIDER" == "gcp" ]; then
  PROJECT_NAME=$(awk -F\" '/"project_id"/ {print $4}' "$CREDENTIALS")

  ERR_OUTPUT=$(terraform apply \
  -var="ssh_user=$(whoami)" \
  -var="gcp_credentials=$CREDENTIALS" \
  -var="instance_type=$MACHINE_TYPE" \
  -var="instance_name=$INSTANCE_NAME" \
  -var="zone=$ZONE" \
  -var="project_name=$PROJECT_NAME" \
  -var="ssh_public_key_path=$PUBLIC_KEY" \
  -auto-approve 2>&1)

elif [ "$PROVIDER" == "aws" ]; then
  AWS_ACCESS_KEY_ID=$(awk -F\" '/"access_key_id"/ {print $4}' "$CREDENTIALS")
  AWS_SECRET_ACCESS_KEY=$(awk -F\" '/"secret_access_key"/ {print $4}' "$CREDENTIALS")

  ERR_OUTPUT=$(terraform apply \
  -var="aws_access_key=$AWS_ACCESS_KEY_ID" \
  -var="aws_secret_key=$AWS_SECRET_ACCESS_KEY" \
  -var="instance_type=$MACHINE_TYPE" \
  -var="instance_name=$INSTANCE_NAME" \
  -var="region=$ZONE" \
  -var="ssh_public_key_path=$PUBLIC_KEY" \
  -auto-approve 2>&1) 

else
  echo "Error: Unsupported provider!"
  exit 1
fi

# Get instance IP
INSTANCE_IP=$(terraform output -raw instance_ip)
echo "Instance created with IP: $INSTANCE_IP"

# Go back to original directory
cd "$current_dir"
echo "$INSTANCE_IP" > instance_ip.txt