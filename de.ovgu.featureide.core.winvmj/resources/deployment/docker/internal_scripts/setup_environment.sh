# Python
cd /usr/bin
sudo ln -s python3 python

# Unzip
sudo apt install unzip -y

# Mkdir
sudo mkdir /var/www/products
sudo mkdir /home/prices-deployment
sudo mkdir /home/prices-deployment/nix-environment
sudo mkdir /home/prices-deployment/port_reserver
sudo mkdir /etc/default/products/
sudo mkdir /home/prices-deployment/nix-environment

# Certbot
sudo add-apt-repository universe
sudo apt update
sudo apt install certbot python3-certbot-nginx -y

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

# PG Client
sudo apt update
sudo apt install -y postgresql-client

# Docker
# Add Docker's official GPG key:
sudo apt-get update
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to Apt sources:
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update

sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo service docker start
