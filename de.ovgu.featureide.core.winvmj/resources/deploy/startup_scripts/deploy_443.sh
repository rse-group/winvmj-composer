#!/bin/bash

CERTIFICATE_NAME=$3
NGINX_CERTIFICATE_NAME_OUT=$4

PRODUCT_PREFIX="$5"
PRODUCT_PREFIX="${PRODUCT_PREFIX:-aisco}"

DB_URL="$6"
DB_URL="${DB_URL:-localhost:5432}"

DB_USERNAME="$7"
DB_USERNAME="${DB_USERNAME:-postgres}"

DB_PASSWORD="$8"
DB_PASSWORD="${DB_PASSWORD:-postgres}"


init_deployment() {
  product_name=$1
  product_dir=$2
  product_name_full=${PRODUCT_PREFIX}.product.$1
  deployed_ports_file=/home/prices-deployment/deployed_prices_ports.csv

  echo "Product Name: $product_name";
  echo "Product Directory: $product_dir";

  sudo touch $deployed_ports_file
  is_product_exist=$(grep -cw "$product_name" $deployed_ports_file)

  if [ $is_product_exist = 0 ]; then
    is_new_deployment=true
    echo "New product '$product_name' detected, initiating deployment"
    new_deployment
  else
    is_new_deployment=false
    echo "Existing product: '$product_name' detected, initiating redeployment"
    redeployment
  fi
}

init_port() {
  trap 'error_deployment' ERR
  echo "Initiating port reservation for every product's components"
  product_static_port=$(port_reserver $product_name-static-service $deployed_ports_file)
  product_be_port=$(port_reserver $product_name-backend-service $deployed_ports_file $(( product_static_port + 1 )))
  product_jmx_exporter_port=$(port_reserver $product_name-jmx-exporter $deployed_ports_file 50000 60000)
}

# stored used port information for product redeployment
load_ports_from_file() {
  trap 'error_deployment' ERR
  echo "Loading reserved ports for $product_name from $deployed_ports_file"

  product_static_port=$(grep "^${product_name}-static-service," "$deployed_ports_file" | cut -d',' -f2)
  product_be_port=$(grep "^${product_name}-backend-service," "$deployed_ports_file" | cut -d',' -f2)
  product_jmx_exporter_port=$(grep "^${product_name}-jmx-exporter," "$deployed_ports_file" | cut -d',' -f2)

  if [[ -z "$product_static_port" || -z "$product_be_port" || -z "$product_jmx_exporter_port" ]]; then
    echo "Error: Could not find all required ports for $product_name in $deployed_ports_file"
    exit 1
  fi

  echo "Loaded ports:"
  echo "Static port: $product_static_port"
  echo "Backend port: $product_be_port"
  echo "JMX exporter port: $product_jmx_exporter_port"
}


be_env_config_setup() {
  trap 'error_deployment' ERR
  echo "Generating backend environment variable configuration for $product_name product"
  be_env_file=/etc/default/products/$product_name
  sudo touch $be_env_file
  sudo tee $be_env_file > /dev/null <<EOL
AMANAH_DB_URL="jdbc:postgresql://$DB_URL/`echo $product_name_full | tr . _`"
AMANAH_DB_USERNAME="$DB_USERNAME"
AMANAH_DB_PASSWORD="$DB_PASSWORD"
AMANAH_HOST_BE="localhost"
AMANAH_PORT_BE=${product_be_port}
_JAVA_OPTIONS="-javaagent:/usr/local/bin/jmx_prometheus_javaagent-0.18.0.jar=${product_jmx_exporter_port}:/etc/jmx/config.yml"
EOL
}

generate_be_systemd_config() {
  trap 'error_deployment' ERR
  echo "Generating backend systemd configuration for $product_name product in $product_name-be.service"
  PRODUCT=$product_name
  PRODUCT_DIR=$product_dir
  PRODUCT_FULL=$product_name_full
  USER=root
  JAVA_PATH=$(which java)
  OUT=$product_name-be.service
  cat <<EOF | sudo tee $OUT >/dev/null
[Unit]
Description=${PRODUCT} Backend Service
Documentation=https://example.com
After=network.target

[Service]
WorkingDirectory=${PRODUCT_DIR}/backend
Type=simple
User=${USER}
EnvironmentFile=/etc/default/products/${PRODUCT}
ExecStart=${JAVA_PATH} -cp ${PRODUCT_FULL}:/home/prices-deployment/nix-environment/prices_product_libraries --module-path ${PRODUCT_FULL}:/home/prices-deployment/nix-environment/prices_product_libraries -m ${PRODUCT_FULL}
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
}

generate_static_systemd_config() {
  trap 'error_deployment' ERR
  echo "Generating static systemd configuration for $product_name product in $product_name-static.service"
  PRODUCT=$product_name
  PRODUCT_DIR=$product_dir
  USER=root
  JSON_SERVER_PATH=$(which json-server)
  STATIC_PORT=$product_static_port
  OUT=$product_name-static.service
  cat <<EOF | sudo tee $OUT >/dev/null
[Unit]
Description=${PRODUCT} Static Service
Documentation=https://example.com
After=network.target

[Service]
WorkingDirectory=${PRODUCT_DIR}/frontend
Type=simple
User=${USER}
ExecStart=${JSON_SERVER_PATH} --watch static-page-db.json --port=${STATIC_PORT}

[Install]
WantedBy=multi-user.target
EOF
}

generate_nginx_config() {
  trap 'error_deployment' ERR
  echo "Generating nginx configuration for $product_name product in $product_name.conf"
  PRODUCT=$product_name
  PRODUCT_DIR=$product_dir
  STATIC_PORT=$product_static_port
  BE_PORT=$product_be_port
  OUT=$NGINX_CERTIFICATE_NAME_OUT

  cat <<EOF | sudo tee $OUT >/dev/null
server {
  listen 443 ssl;
  server_name ${CERTIFICATE_NAME};

  ssl_certificate /etc/letsencrypt/live/${CERTIFICATE_NAME}/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/${CERTIFICATE_NAME}/privkey.pem;

  location / {
    root ${PRODUCT_DIR}/frontend/build;
    index index.html;
    try_files \$uri \$uri/ /index.html /index.htm =404;
  }

  location @admin_endpoint {
    proxy_pass             http://localhost:${STATIC_PORT};
    proxy_redirect         off;
    proxy_http_version     1.1;
    proxy_set_header       Upgrade \$http_upgrade;
    proxy_set_header       Connection "upgrade";
    proxy_set_header       Last-Modified \$date_gmt;
    proxy_set_header       Cache-Control 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0';
    proxy_no_cache         1;
    proxy_cache_bypass     1;
    add_header             Last-Modified \$date_gmt;
    add_header             Cache-Control 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0';
    if_modified_since      off;
    expires                off;
    etag                   off;
  }

  location /apiadmin {
    try_files \$uri @admin_endpoint;
  }

  location /apiimage {
    try_files \$uri @admin_endpoint;
  }

  location /call {
    proxy_pass http://localhost:${BE_PORT};
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header Authorization \$http_authorization;
  }

  location /auth {
    proxy_pass http://localhost:${BE_PORT};
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header Authorization \$http_authorization;
  }

  location /static-data{
    try_files \$uri @admin_endpoint;
  }

  location /appearance{
    try_files \$uri @admin_endpoint;
  }
}

server {
  listen 80;
  server_name ${CERTIFICATE_NAME};

  # Redirect HTTP to HTTPS
  return 301 https://\$host\$request_uri;
}
EOF
}

be_systemd_setup() {
  trap 'error_deployment' ERR
  echo "Setting up systemd backend service..."
  sudo mv $product_name-be.service /lib/systemd/system
  sudo systemctl daemon-reload
  sudo systemctl start $product_name-be.service
  sudo systemctl enable $product_name-be.service
}

static_systemd_setup() {
  trap 'error_deployment' ERR
  echo "Setting up systemd static service..."
  sudo mv $product_name-static.service /lib/systemd/system
  sudo systemctl daemon-reload
  sudo systemctl start $product_name-static.service
  sudo systemctl enable $product_name-static.service
}

nginx_setup() {
  trap 'error_deployment' ERR
  echo "Setting up nginx web server..."
  sudo mkdir -p $product_dir/logs
  sudo touch $product_dir/logs/nginx_proxy_access.log $product_dir/logs/nginx_proxy_error.log
  sudo mv $NGINX_CERTIFICATE_NAME_OUT /etc/nginx/sites-enabled
  sudo nginx -t
  sudo systemctl restart nginx
}

wait_for_db() {
  trap 'error_deployment' ERR
  if [ -z "$product_name_full" ]; then
    echo "Error: product_name_full is not set!"
    return 1
  fi
  db_name="${product_name_full//./_}"

  echo "Waiting for database to be ready..."

  export PGPASSWORD="$DB_PASSWORD"

  # Timeout maksimal dalam detik (5 menit = 300 detik)
  MAX_WAIT_TIME=300
  WAIT_INTERVAL=5
  elapsed=0

  until psql -U postgres -h localhost -d "$db_name" -c '\l' >/dev/null 2>&1; do
    echo "Database $db_name not ready, retrying in $WAIT_INTERVAL seconds..."
    sleep $WAIT_INTERVAL
    elapsed=$((elapsed + WAIT_INTERVAL))

    if [ $elapsed -ge $MAX_WAIT_TIME ]; then
      echo "Warning: Database $db_name for product $product_name_full not ready after $((MAX_WAIT_TIME / 60)) minutes."
      unset PGPASSWORD
      return 1
    fi
  done

  echo "Database $product_name_full is ready!"

  # Hapus PGPASSWORD setelah selesai
  unset PGPASSWORD
}

database_setup() {
  trap 'error_deployment' ERR
  echo "Setting up database..."
  echo "SELECT 'CREATE DATABASE `echo $product_name_full | tr . _`' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '`echo $product_name_full | tr . _`') \gexec" | psql "postgresql://$DB_USERNAME:$DB_PASSWORD@localhost"
}

database_seeding() {
  trap 'error_deployment' ERR
  echo "Filling up product's database..."
  for sql_file in $product_dir/backend/sql/*.sql;
  do
    DB_SEED_NAME=echo $product_name_full | tr . _
    SEED_FILE=$sql_file
    psql postgresql://postgres:postgres@$DB_URL/$DB_SEED_NAME -f $SEED_FILE
  done
}

port_reserver() {
  trap 'error_deployment' ERR
  product_component=$1
  ports_file=$2
  lower_port=${3:-30000}
  upper_port=${4:-40000}
  available_port=$(sudo python /home/prices-deployment/port_reserver/port_reserver.py $product_component $lower_port $upper_port $ports_file)
  if [ $available_port = 'None' ]; then
    error_deployment 1 "No port available to be used";
  else
    echo "$available_port";
  fi
}

error_deployment() {
  deploy_error_code=${1:-$?}
  deploy_error_msg=${2:-$($BASH_COMMAND 2>&1)}
  echo -e "There has been error on the product's deployment.\nERROR: $deploy_error_msg";
  if [ $is_new_deployment=true ]; then
    abort_deployment
  fi
  exit $deploy_error_code;
}

abort_deployment() {
  echo "Aborting deployment...";
  sudo systemctl stop $product_name-static
  sudo systemctl stop $product_name-be
  sudo systemctl disable $product_name-static
  sudo systemctl disable $product_name-be
  sudo rm -f /lib/systemd/system/$product_name-static.service
  sudo rm -f /lib/systemd/system/$product_name-be.service
  sudo rm -f /var/www/products/nginx/$product_name.conf
  sudo rm -f /etc/default/products/$product_name
  sudo rm -f $product_dir/logs/nginx_proxy_access.log $product_dir/logs/nginx_proxy_error.log
  sudo systemctl daemon-reload
  sudo systemctl restart nginx
  echo "DROP DATABASE IF EXISTS ${PRODUCT_PREFIX}_product_$product_name;" | psql "postgresql://$DB_USERNAME:$DB_PASSWORD@localhost"
  sudo grep -v "$product_name-static" $deployed_ports_file > /tmp/tmp_port_file && sudo mv /tmp/tmp_port_file $deployed_ports_file
  sudo grep -v "$product_name-backend" $deployed_ports_file > /tmp/tmp_port_file && sudo mv /tmp/tmp_port_file $deployed_ports_file
  sudo grep -v "$product_name-jmx-exporter" $deployed_ports_file > /tmp/tmp_port_file && sudo mv /tmp/tmp_port_file $deployed_ports_file
  echo "The deployment has been aborted."
}

reload_processes() {
  trap 'error_deployment' ERR
  sudo systemctl stop $product_name-static
  sudo systemctl stop $product_name-be
  sudo systemctl daemon-reload
  sudo systemctl start $product_name-static
  sudo systemctl start $product_name-be
}

new_deployment() {
  init_port
  be_env_config_setup
  generate_be_systemd_config
  generate_static_systemd_config
  database_setup
  wait_for_db
  be_systemd_setup
  static_systemd_setup
  generate_nginx_config
  nginx_setup
  # database_seeding
  echo "Deployment finished!"
}

redeployment() {
  reload_processes
  load_ports_from_file
  generate_nginx_config
  nginx_setup
  database_seeding
  echo "Redeployment finished!"
}

init_deployment $1 $2