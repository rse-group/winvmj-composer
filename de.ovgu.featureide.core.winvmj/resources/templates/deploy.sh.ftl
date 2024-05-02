#!/bin/bash

# Environment variables required:
# - 'private_key_amanah': Direktori lokasi private key akses server Amanah
# - 'username_amanah': Credential username untuk akses server Amanah
# - 'local_tunnel_port': Port lokal yang dibuka untuk akses server Amanah melalui SSH tunneling server Kawung

# Print script description
echo "Eclipse Deployment Script for PRICES-IDE products"
echo

# Step 1 - Copy product components to Amanah server
product_name="${productName}"
echo "Deploying PRICES-IDE product '$product_name' to Amanah server."
echo

read -p "Enter the local directory path of the product (ensure the folder is archived as .zip with correct file structure): " product_local_directory
echo "Product directory: $product_local_directory"

echo "Copying product '$product_name' to Amanah server..."
scp -i "$private_key_amanah" -P "$local_tunnel_port" -r "$product_local_directory" "$username_amanah@localhost:/tmp"
if [ $? -eq 0 ]; then
  echo "Success!"
else
  echo "Error copying product '$product_name' to Amanah server."
  echo "Please try again."
  echo "Note: Ensure server credentials are correct and the product has the right information and structure."
  exit 1
fi

echo "Extracting product '$product_name'..."
ssh -i "$private_key_amanah" "$username_amanah@localhost" -p "$local_tunnel_port" "cd /var/www/products && sudo unzip /tmp/$product_name.zip && rm /tmp/$product_name.zip"
if [ $? -eq 0 ]; then
  echo "Success!"
  echo
  echo "Finished moving product '$product_name' to Amanah server!"
else
  echo "Error extracting product '$product_name' on Amanah server."
  echo "Please try again."
  echo "Note: Ensure server credentials are correct and the product has the right information and structure."
  exit 1
fi

echo

# Step 2 - Deploy product by executing Prices Product Deployment Script (prices_product_deployment.sh) on the server via SSH
echo "Deploying product '$product_name' on Amanah server..."
product_remote_directory="/var/www/products/$product_name"

ssh -i "$private_key_amanah" "$username_amanah@localhost" -p "$local_tunnel_port" "cd /home/prices-deployment/nix-environment && nix-shell --run 'bash prices_product_deployment.sh $product_name $product_remote_directory'"
if [ $? -eq 0 ]; then
  echo
  echo "Product '$product_name' deployed successfully!"
  echo "Deployment of PRICES-IDE product completed successfully."
else
  echo
  echo "Error deploying product '$product_name' on Amanah server."
  echo "Note: Check product components and try deploying again."
fi