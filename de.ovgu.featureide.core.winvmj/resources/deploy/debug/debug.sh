# List all system services
systemctl list-units --type=service
systemctl list-units --type=service | grep be.service
journalctl -n 20 -u test-be.service > /home/rzknurfaizi/journal_output.txt

# Nix product path
cd /home/prices-deployment/nix-environment

# Product path
cd /var/www/products

# Abort
cd /home/prices-deployment/nix-environment
sudo rm ../deployed_prices_ports.csv
sudo bash prices_abort.sh test /var/www/products/test
sudo rm /etc/nginx/sites-enabled/procom.procom-rzk
sudo nginx -t
sudo systemctl restart nginx

# Remove from deployed ports
sudo sed -i '4d;5d;6d' ../deployed_prices_ports.csv

# Unzip product
sudo unzip test.zip -d /var/www/products

# Create db
psql -h localhost -U postgres
CREATE DATABASE aisco_product_Test;
psql postgresql://postgres:postgres@localhost:5432/aisco_product_test

# Seed db
psql postgresql://postgres:postgres@localhost:5432/aisco_product_test -f ./auth_seed.sql
psql postgresql://postgres:postgres@localhost:5432/aisco_product_test -f ./coa_seed.sql


# Test nginx config
sudo nginx -t
sudo systemctl restart nginx

# BE
curl http://localhost:30002
<h1>404 Not Found</h1>No context found for request

# Deployed ports
cat /home/prices-deployment/deployed_prices_ports.csv

# Certbot
sudo certbot certonly --nginx -d procom.procom-rzk.cfd
sudo certbot certonly --nginx -d realprocom.procom-rzk.cfd

# Stop service
sudo systemctl stop aisco_curl_checker.timer
sudo systemctl disable aisco_curl_checker.timer
sudo rm /etc/systemd/system/aisco_curl_checker.timer
sudo systemctl daemon-reload
sudo systemctl reset-failed
sudo systemctl stop aisco_curl_checker.service
sudo systemctl disable aisco_curl_checker.service
sudo rm /etc/systemd/system/aisco_curl_checker.service
sudo systemctl daemon-reload
sudo systemctl reset-failed

journalctl -n 20 -u aisco_curl_checker.service




# TESTING
cat <<EOF | sudo tee /etc/systemd/system/aisco_curl_checker.service >/dev/null
[Unit]
Description=Check PRICES Service

[Service]
Type=oneshot
ExecStart=/bin/bash -c "$VM_ROOT_FILES/check_if_propagated.sh $USERNAME $PRODUCT_NAME $CERTIFICATE_NAME $NGINX_CERTIFICATE_NAME"
User=root
Group=root

[Install]
WantedBy=multi-user.target
EOF

# Run it per minute
cat <<EOF | sudo tee /etc/systemd/system/aisco_curl_checker.timer >/dev/null
[Unit]
Description=Run aisco_curl_checker.service every minute

[Timer]
OnUnitActiveSec=1min
Unit=aisco_curl_checker.service

[Install]
WantedBy=timers.target
EOF

sudo systemctl daemon-reload
sudo systemctl start aisco_curl_checker.timer
sudo systemctl enable aisco_curl_checker.timer





cat <<EOF | sudo tee /etc/systemd/system/aisco_curl_checker.service >/dev/null
[Unit]
Description=Check PRICES Service

[Service]
Type=simple
ExecStart=/bin/bash -c "/home/rzknurfaizi/hello_script.sh"
User=root

[Install]
WantedBy=multi-user.target
EOF

cat <<EOF | sudo tee /etc/systemd/system/aisco_curl_checker.timer >/dev/null
[Unit]
Description=Run aisco_curl_checker.service every minute

[Timer]
OnUnitActiveSec=1min
Unit=aisco_curl_checker.service

[Install]
WantedBy=timers.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable aisco_curl_checker.timer
sudo systemctl start aisco_curl_checker.timer

journalctl -n 20 -u aisco_curl_checker.service
systemctl list-units --type=timer

# Check if jmx exporter running
sudo ps -ax | grep jmx

# Deploy example
./wrapper.sh rzknurfaizi amanah-test SMALL US test automate-zatesto.procom-rzk.cfd automate-zatesto.procom-rzk
./wrapper.sh rzknurfaizi amanah-test2 SMALL US test procom.procom-rzk.cfd procom.procom-rzk
