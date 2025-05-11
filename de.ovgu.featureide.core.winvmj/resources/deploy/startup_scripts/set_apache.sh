#! /bin/bash
apt update
apt -y install apache2
cat <<EOF > /var/www/html/index.html
<html><body><p>Lab RSE, GCP Created</p></body></html>
EOF