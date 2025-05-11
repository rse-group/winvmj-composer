#!/bin/bash

# Variables
NGINX_CERTIFICATE_NAME=$1

#
# MAIN
#

sudo apt update -y
sudo apt upgrade -y
sudo apt-get update -y
sudo apt-get -y upgrade -y

# Java
sudo apt install openjdk-17-jdk -y

# Postgres
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" | sudo tee /etc/apt/sources.list.d/postgresql-pgdg.list > /dev/null
sudo apt-get update
sudo apt install postgresql-11 -y
# Start pgcluster
sudo pg_ctlcluster 11 main start
# Change postgres user
sudo -u postgres psql << EOF
ALTER USER postgres WITH PASSWORD 'postgres';
\q
EOF

# Node
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys ABF5BD827BD9BF62
curl -s https://deb.nodesource.com/setup_16.x | sudo bash
sudo apt-get install nodejs -y

# Python
cd /usr/bin
sudo ln -s python3 python

# Certbot
sudo apt install certbot python3-certbot-nginx -y

# Json-server
sudo npm i -g json-server 

# Unzip
sudo apt install unzip

# Mkdir
sudo mkdir /var/www/products
sudo mkdir /home/prices-deployment
sudo mkdir /home/prices-deployment/nix-environment
sudo mkdir /home/prices-deployment/port_reserver
sudo mkdir /etc/default/products/
sudo mkdir /home/prices-deployment/nix-environment

# Prometheus
sudo wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.18.0/jmx_prometheus_javaagent-0.18.0.jar
sudo mv jmx_prometheus_javaagent-0.18.0.jar /usr/local/bin/
sudo mkdir /etc/jmx
# Define the content to write to the file
jmxContent="rules:
- pattern: \".*\""
# Write the content to the file
echo "$jmxContent" | sudo tee /etc/jmx/config.yml > /dev/null