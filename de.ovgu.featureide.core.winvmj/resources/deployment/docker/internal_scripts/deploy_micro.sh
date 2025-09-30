#!/bin/bash

CERTIFICATE_NAME=$3
NGINX_CERTIFICATE_NAME_OUT=$4

NUM_BACKENDS=$5

PRODUCT_PREFIX="$6"
PRODUCT_PREFIX="${PRODUCT_PREFIX:-aisco}"

DB_URL="$7"
DB_URL="${DB_URL:-localhost:5432}"

DB_USERNAME="$8"
DB_USERNAME="${DB_USERNAME:-postgres}"

DB_PASSWORD="$9"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

RABBITMQ_HOST="${10}"
RABBITMQ_HOST="${RABBITMQ_HOST:-${1}_rabbitmq}"

RABBITMQ_USER="${11}"
RABBITMQ_USER="${RABBITMQ_USER:-guest}"

RABBITMQ_PASS="${12}"
RABBITMQ_PASS="${RABBITMQ_PASS:-guest}"

init_deployment() {
  product_name=$1
  product_dir=$2
  product_name_full=${PRODUCT_PREFIX}.product.$1
  deployed_ports_file=/home/prices-deployment/deployed_prices_ports.csv
  NETWORK_NAME="$product_name-network"

  echo "Product Name: $product_name";
  echo "Product Directory: $product_dir";

  sudo touch $deployed_ports_file
  is_product_exist=$(grep -cw "$product_name" $deployed_ports_file)
  # Check if deployment is microservice or monolith
  if [ "$NUM_BACKENDS" -eq 1 ]; then
    if [ $is_product_exist = 0 ]; then
      echo "New product '$product_name' detected, initiating deployment (single backend)"
      new_deployment
    else
      echo "Existing product: '$product_name' detected, initiating redeployment (single backend)"
      redeployment
    fi
  else
    if [ $is_product_exist = 0 ]; then
      echo "New product '$product_name' detected, initiating deployment (multi-backend)"
      new_deployment_microservice
    else
      echo "Existing product: '$product_name' detected, initiating redeployment (multi-backend)"
      redeployment_microservice
    fi
  fi

}

init_port_microservice() {
  trap 'error_deployment' ERR
  echo "Reserving ports for FE and API gateway..."
  product_static_port=$(port_reserver $product_name-static-service $deployed_ports_file)
  product_rabbitmq_port=$(port_reserver $product_name-rabbitmq-service $deployed_ports_file)
  product_rabbitmq_manage_port=$(port_reserver $product_name-rabbitmq-manage-service $deployed_ports_file)
  product_gateway_port=$(port_reserver $product_name-gateway-service $deployed_ports_file $(( product_static_port + 1 )))
}

init_port() {
  trap 'error_deployment' ERR
  echo "Reserving ports for FE and API gateway..."
  product_static_port=$(port_reserver $product_name-static-service $deployed_ports_file)
  product_gateway_port=$(port_reserver $product_name-backend-service $deployed_ports_file $(( product_static_port + 1 )))
}

load_ports_from_file_microservice() {
  product_static_port=$(grep "^${product_name}-static-service," "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')
  product_rabbitmq_port=$(grep "^${product_name}-rabbitmq-service," "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')
  product_rabbitmq_manage_port=$(grep "^${product_name}-rabbitmq-manage-service," "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')
  product_gateway_port=$(grep "^${product_name}-gateway-service," "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')
}

load_ports_from_file() {
  product_static_port=$(grep "^${product_name}-static-service," "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')
  product_gateway_port=$(grep "^${product_name}-backend-service," "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')
}


generate_nginx_config() {
  echo "Generating nginx config for product: $product_name"
  echo "Using backend port: $product_gateway_port"
  echo "Certificate name: $CERTIFICATE_NAME"
  echo "Nginx certificate name: $NGINX_CERTIFICATE_NAME_OUT"
  
  PRODUCT_DIR=$product_dir
  STATIC_PORT=$product_static_port
  BE_PORT=$product_gateway_port
  OUT=$NGINX_CERTIFICATE_NAME_OUT
  
  # Ensure we have an absolute path for the output file
  if [[ "$OUT" != /* ]]; then
    OUT="/tmp/$OUT"
  fi
  
  echo "Creating nginx config file: $OUT"

  # Check if SSL certificates exist to determine HTTP vs HTTPS configuration
  if [ -f "/etc/letsencrypt/live/${CERTIFICATE_NAME}/fullchain.pem" ] && [ -f "/etc/letsencrypt/live/${CERTIFICATE_NAME}/privkey.pem" ]; then
    echo "SSL certificates found. Generating HTTPS configuration..."
    cat <<EOF | sudo tee $OUT >/dev/null
server {
  listen 443 ssl;
  server_name ${CERTIFICATE_NAME};
  client_max_body_size 20M;

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
  else
    echo "No SSL certificates found. Generating HTTP-only configuration..."
    cat <<EOF | sudo tee $OUT >/dev/null
server {
  listen 80;
  server_name ${CERTIFICATE_NAME};
  client_max_body_size 20M;

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
EOF
  fi
  
  # Verify the nginx config file was created
  if [ -f "$OUT" ]; then
    echo "Nginx config file created successfully: $OUT"
    echo "File size: $(stat -c%s "$OUT") bytes"
  else
    echo "ERROR: Failed to create nginx config file: $OUT"
    echo "Current working directory: $(pwd)"
    echo "Directory contents:"
    ls -la
    return 1
  fi
}

nginx_setup() {
  trap 'error_deployment' ERR
  echo "Setting up nginx web server..."
  
  # Determine the correct path for the nginx config file
  NGINX_CONFIG_PATH=$NGINX_CERTIFICATE_NAME_OUT
  if [[ "$NGINX_CONFIG_PATH" != /* ]]; then
    NGINX_CONFIG_PATH="/tmp/$NGINX_CONFIG_PATH"
  fi
  
  echo "Nginx config file: $NGINX_CONFIG_PATH"
  
  # Check if the nginx config file exists
  if [ -f "$NGINX_CONFIG_PATH" ]; then
    echo "Found nginx config file at: $NGINX_CONFIG_PATH"
    # Move the nginx configuration to sites-available and enable it
    SITE_NAME=$(basename "$NGINX_CERTIFICATE_NAME_OUT")
    echo "Using site name: $SITE_NAME"
    
    # Copy to sites-available
    sudo cp "$NGINX_CONFIG_PATH" "/etc/nginx/sites-available/$SITE_NAME"
    echo "Copied nginx config to /etc/nginx/sites-available/$SITE_NAME"
    
    # Create symlink to sites-enabled (remove existing if present)
    sudo rm -f "/etc/nginx/sites-enabled/$SITE_NAME"
    sudo ln -sf "/etc/nginx/sites-available/$SITE_NAME" "/etc/nginx/sites-enabled/$SITE_NAME"
    echo "Enabled nginx config at /etc/nginx/sites-enabled/$SITE_NAME"
    
    # Clean up temporary file
    sudo rm -f "$NGINX_CONFIG_PATH"
    echo "Cleaned up temporary file: $NGINX_CONFIG_PATH"
  else
    echo "ERROR: Nginx config file not found at: $NGINX_CONFIG_PATH"
    echo "Checking /tmp directory contents:"
    ls -la /tmp/ | grep -E "(${CERTIFICATE_NAME//\./_}|$product_name)" || echo "No matching files found"
    echo "Checking current directory contents:"
    ls -la . | grep -E "(${CERTIFICATE_NAME//\./_}|$product_name)" || echo "No matching files found"
    return 1
  fi
  
  echo "Testing nginx configuration..."
  sudo nginx -t
  echo "Restarting nginx..."
  sudo systemctl restart nginx
  echo "Nginx setup completed successfully"
}

wait_for_db() {
  trap 'error_deployment' ERR
  echo "Waiting for DB to be ready..."
  db_name=$1
  
  # Sanitize database name - replace dots and other invalid characters with underscores
  db_name=$(echo "$db_name" | sed 's/[^a-zA-Z0-9_]/_/g')
  
  export PGPASSWORD="$DB_PASSWORD"

  MAX_WAIT_TIME=300
  WAIT_INTERVAL=5
  elapsed=0

  until psql -U $DB_USERNAME -h ${DB_URL%%:*} -p ${DB_URL##*:} -d "$db_name" -c '\l' >/dev/null 2>&1; do
    echo "Waiting for DB $db_name... ($elapsed seconds)"
    sleep $WAIT_INTERVAL
    elapsed=$((elapsed + WAIT_INTERVAL))
    if [ $elapsed -ge $MAX_WAIT_TIME ]; then
      echo "Database not ready after timeout."
      unset PGPASSWORD
      return 1
    fi
  done
  echo "Database $db_name is ready."
  unset PGPASSWORD
}

database_setup() {
  echo "Creating database if not exists..."
  db_name=$1
  
  # Sanitize database name - replace dots and other invalid characters with underscores
  db_name=$(echo "$db_name" | sed 's/[^a-zA-Z0-9_]/_/g')
  
  export PGPASSWORD="$DB_PASSWORD"
  echo "Create Database with name: ${db_name}"
  echo "SELECT 'CREATE DATABASE $db_name' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db_name') \gexec" | psql -U $DB_USERNAME -h ${DB_URL%%:*} -p ${DB_URL##*:}
  unset PGPASSWORD
  echo "FINISHED SETTING UP DATABASE"
}

database_seeding() {
  echo "Seeding database for $product_name..."
  db_name=$1
  service_name=$2
  
  # Sanitize database name - replace dots and other invalid characters with underscores
  db_name=$(echo "$db_name" | sed 's/[^a-zA-Z0-9_]/_/g')
  
  for sql_file in $product_dir/$service_name/sql/*.sql; do
    echo "Seeding: $sql_file"
    PGPASSWORD="$DB_PASSWORD" psql -U $DB_USERNAME -h ${DB_URL%%:*} -p ${DB_URL##*:} -d "$db_name" -f "$sql_file"
  done
}

docker_network_setup() {
  trap 'error_deployment' ERR
  echo "Setting up docker network '$NETWORK_NAME'..."
  
  # For redeployments, clean up the existing network to avoid endpoint conflicts
  if docker network ls --format '{{.Name}}' | grep -qw "$NETWORK_NAME"; then
    echo "Network '$NETWORK_NAME' exists. Cleaning up for redeployment..."
    
    # Disconnect all containers from the network
    docker network inspect "$NETWORK_NAME" --format '{{range .Containers}}{{.Name}} {{end}}' 2>/dev/null | xargs -n1 -r docker network disconnect "$NETWORK_NAME" 2>/dev/null || true
    
    # Remove the network
    docker network rm "$NETWORK_NAME" 2>/dev/null || true
    echo "Existing network cleaned up"
  fi
  
  # Create the network
  echo "Creating network '$NETWORK_NAME'..."
  docker network create "$NETWORK_NAME"
  echo "Network '$NETWORK_NAME' created successfully"
}

check_postgres_connection() {
  PGPASSWORD="$DB_PASSWORD" psql -h "${DB_URL%%:*}" -p "${DB_URL##*:}" -U "$DB_USERNAME" -d "$POSTGRES_DB" -c '\q' >/dev/null 2>&1
}

docker_rabbit_mq_and_db() {
  trap 'error_deployment' ERR
  echo "Checking PostgreSQL availability..."

  export PGPASSWORD="$DB_PASSWORD"

  # Trying db connection
  if check_postgres_connection; then
    echo "PostgreSQL is available at $POSTGRES_HOST. Running RabbitMQ(if needed) without creating DB."
    ./orchestrate_rabbitmq_db.sh $product_dir $NETWORK_NAME $product_name $NUM_BACKENDS $DB_USERNAME $DB_PASSWORD postgres 5432 $RABBITMQ_USER $RABBITMQ_PASS $product_rabbitmq_port $product_rabbitmq_manage_port  
  else
    echo "PostgreSQL not reachable. Running RabbitMQ (if needed) and creating DB service."
    ./orchestrate_rabbitmq_db.sh $product_dir $NETWORK_NAME $product_name $NUM_BACKENDS $DB_USERNAME $DB_PASSWORD postgres 5432 $RABBITMQ_USER $RABBITMQ_PASS $product_rabbitmq_port $product_rabbitmq_manage_port $NUM_BACKENDS --with-db
  fi

  unset PGPASSWORD
}

locate_backend_service(){
  trap 'error_deployment' ERR
  BACKENDS=()

  for dir in "$product_dir"/*/; do
    service_name=$(basename "$dir")
    echo "SERVICE FOUND: ${service_name}"

    # Skip frontend dan apigateway
    if [[ "$service_name" != "frontend" && "$service_name" != "ApiGateway" && "$service_name" != "postman" && "$service_name" != "logs" ]]; then
      BACKENDS+=("$service_name")

      # Rename "/backend" directory to product name when product is monolith
      if [[ "$NUM_BACKENDS" -eq 1 && "$service_name" == "backend" ]]; then
        original_path="$dir"
        new_path="$product_dir/$product_name"
        echo "Renaming backend folder to product name: '$product_name'"
        rm -rf "$new_path"
        mv "$original_path" "$new_path"

        BACKENDS=("$product_name")
        break
      fi
    fi

  done

  # Num of backends in directory
  FOUND_BACKENDS=${#BACKENDS[@]}

  if [[ "$FOUND_BACKENDS" -ne "$NUM_BACKENDS" ]]; then
    echo "Error: Expected $NUM_BACKENDS backend(s), but found $FOUND_BACKENDS in '$PRODUCT_PATH'."
    exit 1
  fi
}

docker_backend(){
  trap 'error_deployment' ERR
  echo "Starting Backend-s..."

  echo "Checking database container"
  BACKEND_DB_URL="$DB_URL"

  POSTGRES_CONTAINER=$(sudo docker ps --format "{{.Names}} {{.Image}}" | grep postgres | head -n1 | awk '{print $1}')

  # Check if postgres is a container 
  if [ -n "$POSTGRES_CONTAINER" ]; then
    echo "Postgres container found: $POSTGRES_CONTAINER"

    # Add network to postgres container if it not connected yet
    echo "Connecting postgres container to current network..."
    docker network connect "$NETWORK_NAME" "$POSTGRES_CONTAINER" || echo "Warning: Failed to connect network, the container might already be connected"

    BACKEND_DB_URL="${POSTGRES_CONTAINER}:5432"
  else
    echo "No postgres container found. BACKEND_DB_URL : $BACKEND_DB_URL"
  fi

  # If DB_URL contains localhost or 127.0.0.1, change to IP internal so backend container can access it
  if [[ "$BACKEND_DB_URL" == *"localhost"* || "$BACKEND_DB_URL" == *"127.0.0.1"* ]]; then
    echo "BACKEND_DB_URL contains localhost, looking for internal IP..."
    
    # IP (avoid 127.0.0.1 and docker bridge)
    INTERNAL_IP=$(hostname -I | awk '{for(i=1;i<=NF;i++) if ($i !~ /^127\./ && $i !~ /^172\./) { print $i; break }}')

    if [ -n "$INTERNAL_IP" ]; then
      echo "IP found: $INTERNAL_IP"
      BACKEND_DB_URL="${INTERNAL_IP}:5432"
    else
      echo "Failed to find internal IP, BACKEND_DB_URL : $BACKEND_DB_URL"
    fi
  fi

  echo "DB URL used : $BACKEND_DB_URL"


  for ((i=0; i<NUM_BACKENDS; i++)); do
    SERVICE_NAME="${BACKENDS[$i]}"
    SERVICE_FULL_NAME="${product_name}-${SERVICE_NAME}"
    DB_NAME="${PRODUCT_PREFIX}_product_$SERVICE_NAME"
    DB_NAME="${DB_NAME,,}"
    # Sanitize database name - replace dots and other invalid characters with underscores
    DB_NAME=$(echo "$DB_NAME" | sed 's/[^a-zA-Z0-9_]/_/g')
    PRODUCT_FULL_NAME="${PRODUCT_PREFIX}.product.$SERVICE_NAME"
    PRODUCT_FULL_NAME="${PRODUCT_FULL_NAME,,}"

    echo "SERVICE NAME  ${SERVICE_NAME}"
    echo "DB NAME  ${DB_NAME}"

    # handle naming on monolith
    if [[ "$NUM_BACKENDS" -eq 1 ]]; then
      SERVICE_FULL_NAME=$SERVICE_NAME
    fi

    echo "Checking service port for redeployment"
    # Get the backend port - ensure consistency with how ports are reserved
    # For monolith: use product_name-backend-service (same as in init_port)
    # For microservice: use SERVICE_FULL_NAME-backend-service
    if [ "$NUM_BACKENDS" -eq 1 ]; then
      BE_PORT=$(grep "^${product_name}-backend-service" "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')
    else
      BE_PORT=$(grep "^${SERVICE_FULL_NAME}-backend-service" "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')
    fi

    if [ -z "$BE_PORT" ]; then
      echo "Port not found, reserving new port..."
      if [ "$NUM_BACKENDS" -eq 1 ]; then
        BE_PORT=$(port_reserver "$product_name-backend-service" "$deployed_ports_file")
      else
        BE_PORT=$(port_reserver "$SERVICE_FULL_NAME-backend-service" "$deployed_ports_file")
      fi      
    fi
    
    echo "PORT USED FOR BE: ${BE_PORT}"

    echo "Setting Up Database"
    database_setup $DB_NAME

    wait_for_db $DB_NAME

    # Making sure hibernate.properties is correct
    if [[ "$NUM_BACKENDS" -eq 1 ]]; then
      # For monolithic applications, use simpler path
      HIBERNATE_PROPERTIES_FILE="${product_dir}/${SERVICE_NAME}/hibernate.properties"
    else
      # For microservices, use full path
      HIBERNATE_PROPERTIES_FILE="${product_dir}/${SERVICE_NAME}/${PRODUCT_FULL_NAME}/hibernate.properties"
    fi
    
    echo "Looking for hibernate.properties at: $HIBERNATE_PROPERTIES_FILE"
    
    if [ -f "$HIBERNATE_PROPERTIES_FILE" ]; then
      sed -i "s/localhost:5432/${BACKEND_DB_URL}/g" "$HIBERNATE_PROPERTIES_FILE"
      echo "Updated hibernate.properties successfully"
    else
      echo "Warning: hibernate.properties not found at $HIBERNATE_PROPERTIES_FILE"
      # Try alternative paths
      ALT_HIBERNATE_PROPERTIES_FILE="${product_dir}/${SERVICE_NAME}/src/main/resources/hibernate.properties"
      if [ -f "$ALT_HIBERNATE_PROPERTIES_FILE" ]; then
        sed -i "s/localhost:5432/${BACKEND_DB_URL}/g" "$ALT_HIBERNATE_PROPERTIES_FILE"
        echo "Updated hibernate.properties at alternative location: $ALT_HIBERNATE_PROPERTIES_FILE"
      else
        echo "Warning: hibernate.properties not found in expected locations"
      fi
    fi

    echo "Starting Service full name: $SERVICE_FULL_NAME or service name: $SERVICE_NAME on port $BE_PORT"

    ./orchestrate_backend.sh $product_dir $NETWORK_NAME $product_name $PRODUCT_PREFIX $SERVICE_NAME $SERVICE_FULL_NAME $BE_PORT $RABBITMQ_USER $RABBITMQ_PASS $RABBITMQ_HOST $BACKEND_DB_URL $DB_NAME $DB_USERNAME $DB_PASSWORD

    echo "Seeding data for ${SERVICE_NAME} in backgrounds..."
    echo "Check seeding status at /home/prices-deployment/seeding_log.log"
    echo "Check seeding error log at /home/prices-deployment/seeding_error_log.log and ..."
    sleep 10
    ./seeding.sh $DB_NAME $SERVICE_NAME $product_dir $DB_USERNAME $DB_URL $DB_PASSWORD > /dev/null 2>&1 &
    

  done
}


docker_container_fe_gateway(){
  trap 'error_deployment' ERR
  echo "orchestrating gateway and frontend"

  FRONTEND_CONTAINER_NAME="${product_name}-static"
  API_GATEWAY_CONTAINER_NAME="${product_name}-apigateway"

  ENV_FILE_PATH="$product_dir/container.env"
  : > "$ENV_FILE_PATH" # Emptied file

    # Generate .env content dynamically 
  ENV_CONTENT=$(cat <<EOF
NET=$NETWORK_NAME
FRONTEND_MAIN_CLASS=$product_name
FRONTEND_CONTAINER_NAME=$FRONTEND_CONTAINER_NAME
FRONTEND_PORT=$product_static_port
MAIN_CLASS=$product_name
API_GATEWAY_CONTAINER_NAME=$API_GATEWAY_CONTAINER_NAME
AMANAH_PORT_BE=$product_gateway_port
AMANAH_HOST_BE="0.0.0.0"
ENV_FILE_PATH=$ENV_FILE_PATH
EOF
  )


  AUTH_PORT=""
  # Append dynamic backend service URLs as API GATEWAY env variable
  for service in "${BACKENDS[@]}"; do
    SERVICE_ID="${product_name}-${service}-backend-service"
    PORT=$(grep "^${SERVICE_ID}," "$deployed_ports_file" | cut -d',' -f2)

    echo "Found port: ${PORT} for service ${SERVICE_ID}"

    if [[ -n "$PORT" ]]; then
      VAR_NAME="${service}_URL"
      ENV_CONTENT+="
${VAR_NAME}=http://${product_name}-${service}:$PORT"
      echo "Set ${VAR_NAME}=http://${product_name}-${service}:$PORT"

      # Set Auth URL
      if [[ -z "$AUTH_PORT" ]]; then
        AUTH_PORT="$PORT"
        AUTH_SERVICE="${product_name}-${service}"
      fi
    else
      echo "WARNING: Port not found for $service"
    fi
  done

  VAR_NAME="ServiceAuth_URL"
  ENV_CONTENT+="
${VAR_NAME}=http://${AUTH_SERVICE}:$AUTH_PORT"

  echo "$ENV_CONTENT" > "$ENV_FILE_PATH"
  echo "Environment file saved at: $ENV_FILE_PATH"

  ./orchestrate_container.sh $product_dir $ENV_FILE_PATH true
}

docker_container_fe(){
  trap 'error_deployment' ERR
  echo "orchestrating frontend"

  FRONTEND_CONTAINER_NAME="${product_name}-static"

  ENV_FILE_PATH="$product_dir/container.env"
  : > "$ENV_FILE_PATH" # Emptied file

    # Generate .env content dynamically 
  ENV_CONTENT=$(cat <<EOF
NET=$NETWORK_NAME
FRONTEND_MAIN_CLASS=$product_name
FRONTEND_CONTAINER_NAME=$FRONTEND_CONTAINER_NAME
FRONTEND_PORT=$product_static_port
MAIN_CLASS=$product_name
API_GATEWAY_CONTAINER_NAME=$API_GATEWAY_CONTAINER_NAME
AMANAH_PORT_BE=$product_gateway_port
AMANAH_HOST_BE="0.0.0.0"
ENV_FILE_PATH=$ENV_FILE_PATH
EOF
  )

  echo "$ENV_CONTENT" > "$ENV_FILE_PATH"
  echo "Environment file saved at: $ENV_FILE_PATH"

  ./orchestrate_container.sh $product_dir $ENV_FILE_PATH false
}

error_deployment() {
  deploy_error_code=${1:-$?}
  deploy_error_msg=${2:-$($BASH_COMMAND 2>&1)}
  echo -e "There has been error on the product's deployment.\nERROR: $deploy_error_msg";
  exit $deploy_error_code;
}

cleanup_existing_deployment() {
  echo "Performing comprehensive cleanup of existing deployment..."
  
  # Stop and remove all containers related to this product
  echo "Stopping all containers for product: $product_name"
  docker ps -a --filter "name=${product_name}" --format "{{.Names}}" | xargs -r docker stop 2>/dev/null || true
  docker ps -a --filter "name=${product_name}" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true
  
  # Clean up networks
  if docker network ls --format '{{.Name}}' | grep -qw "$NETWORK_NAME"; then
    echo "Cleaning up network: $NETWORK_NAME"
    docker network disconnect "$NETWORK_NAME" $(docker network inspect "$NETWORK_NAME" --format '{{range .Containers}}{{.Name}} {{end}}') 2>/dev/null || true
    docker network rm "$NETWORK_NAME" 2>/dev/null || true
  fi
  
  # Clean up volumes
  docker volume ls --filter "name=${product_name}" --format "{{.Name}}" | xargs -r docker volume rm 2>/dev/null || true
  
  # Clean up any orphaned containers
  docker container prune -f 2>/dev/null || true
  
  echo "Cleanup completed"
}

new_deployment_microservice() {
  cleanup_existing_deployment
  init_port_microservice
  generate_nginx_config
  # Set up nginx early to ensure it's configured even if Docker setup fails later
  nginx_setup || echo "Warning: Nginx setup failed, but continuing deployment"
  docker_network_setup
  docker_rabbit_mq_and_db
  locate_backend_service
  docker_backend 
  docker_container_fe_gateway
  echo "Deployment for $product_name complete"
}

redeployment_microservice() {
  cleanup_existing_deployment
  load_ports_from_file_microservice
  generate_nginx_config
  # Set up nginx early to ensure it's configured even if Docker setup fails later
  nginx_setup || echo "Warning: Nginx setup failed, but continuing deployment"
  docker_network_setup
  docker_rabbit_mq_and_db
  locate_backend_service
  docker_backend 
  docker_container_fe_gateway
  echo "Redeployment for $product_name complete"
}

new_deployment() {
  cleanup_existing_deployment
  init_port
  generate_nginx_config
  # Set up nginx early to ensure it's configured even if Docker setup fails later
  nginx_setup || echo "Warning: Nginx setup failed, but continuing deployment"
  docker_network_setup
  docker_rabbit_mq_and_db
  locate_backend_service
  docker_backend 
  docker_container_fe
  echo "Deployment for $product_name complete"
}

redeployment() {
  cleanup_existing_deployment
  load_ports_from_file
  generate_nginx_config
  # Set up nginx early to ensure it's configured even if Docker setup fails later
  nginx_setup || echo "Warning: Nginx setup failed, but continuing deployment"
  docker_network_setup
  docker_rabbit_mq_and_db
  locate_backend_service
  docker_backend 
  docker_container_fe
  echo "Redeployment for $product_name complete"
}

port_reserver() {
  product_component=$1
  ports_file=$2
  lower_port=${3:-30000}
  upper_port=${4:-40000}
  available_port=$(sudo python /home/prices-deployment/port_reserver/port_reserver.py $product_component $lower_port $upper_port $ports_file)
  if [ "$available_port" = "None" ]; then
    error_deployment "No available port for $product_component"
  else
    echo "$available_port"
  fi
}

init_deployment $1 $2
