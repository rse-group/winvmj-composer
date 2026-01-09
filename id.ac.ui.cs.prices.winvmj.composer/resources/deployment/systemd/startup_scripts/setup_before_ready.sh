#!/bin/bash

USERNAME=$1
VM_ROOT_FILES=/home/$USERNAME
PRODUCT_NAME=$2
CERTIFICATE_NAME=$3
NGINX_CERTIFICATE_NAME=$4

sudo apt update -y
sudo apt upgrade -y
sudo apt-get update -y
sudo apt-get -y upgrade -y

# Nginx
sudo apt install -y curl gnupg2 ca-certificates lsb-release
echo "deb http://nginx.org/packages/mainline/ubuntu $(lsb_release -cs) nginx" | sudo tee /etc/apt/sources.list.d/nginx.list
curl -fsSL https://nginx.org/keys/nginx_signing.key | sudo gpg --dearmor -o /usr/share/keyrings/nginx-archive-keyring.gpg
sudo apt update
sudo apt install nginx -y
sudo ufw allow 80
sudo ufw allow 443
sudo ufw allow 'Nginx Full'
sudo systemctl restart nginx

# Create the index.html file
echo "<!DOCTYPE html>
<html lang=\"en\">
<head>
    <meta charset=\"UTF-8\">
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
    <title>Lab RSE</title>
</head>
<body>
    <h1>Hello, Lab RSE!</h1>
</body>
</html>" | sudo tee /var/www/html/index.html

# Restart NGINX to apply changes
sudo systemctl restart nginx