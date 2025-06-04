#!/bin/bash
NUM_BACKENDS=$3

PRODUCT_PREFIX="$4"
PRODUCT_PREFIX="${PRODUCT_PREFIX:-aisco}"

DB_URL="$5"
DB_URL="${DB_URL:-10.119.106.132:5432}"

DB_USERNAME="$6"
DB_USERNAME="${DB_USERNAME:-deployer}"

DB_PASSWORD="$7"
DB_PASSWORD="${DB_PASSWORD:-rseamanah}"

RABBITMQ_HOST="$8"
RABBITMQ_HOST="${RABBITMQ_HOST:-rabbitmq}"

RABBITMQ_USER="$9"
RABBITMQ_USER="${RABBITMQ_USER:-guest}"

RABBITMQ_PASS="${10}"
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
  trap 'error_deployment' ERR
  echo "Generating nginx configuration for $product_name product in $product_name.conf"
  PRODUCT=$product_name
  PRODUCT_DIR=$product_dir
  STATIC_PORT=$product_static_port
  BE_PORT=$product_gateway_port
  OUT=$product_name.conf
  cat <<EOF | sudo tee "$OUT" >/dev/null
server {
  server_name ${PRODUCT}.amanah-staging.cs.ui.ac.id;
  listen 80;
  listen [::]:80;

  access_log ${PRODUCT_DIR}/logs/nginx_proxy_access.log;
  error_log ${PRODUCT_DIR}/logs/nginx_proxy_error.log;

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
  }

  location /auth {
    proxy_pass http://localhost:${BE_PORT};
  }

  location /static-data{
    try_files \$uri @admin_endpoint;
  }

  location /appearance{
    try_files \$uri @admin_endpoint;
  }
  

}
EOF
}

nginx_setup() {
  trap 'error_deployment' ERR
  echo "Setting up nginx web server..."
  sudo mkdir -p "$product_dir"/logs
  sudo touch "$product_dir"/logs/nginx_proxy_access.log "$product_dir"/logs/nginx_proxy_error.log
  sudo mv "$product_name".conf /var/www/products/nginx
  sudo systemctl restart nginx
}

wait_for_db() {
  trap 'error_deployment' ERR
  echo "Waiting for DB to be ready..."
  db_name=$1
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
  for sql_file in $product_dir/$service_name/sql/*.sql; do
    echo "Seeding: $sql_file"
    PGPASSWORD="$DB_PASSWORD" psql -U $DB_USERNAME -h ${DB_URL%%:*} -p ${DB_URL##*:} -d "$db_name" -f "$sql_file"
  done
}

docker_network_setup() {
  trap 'error_deployment' ERR
  echo "Checking docker network '$NETWORK_NAME'..."
  if ! docker network ls --format '{{.Name}}' | grep -qw "$NETWORK_NAME"; then
    echo "Creating network '$NETWORK_NAME'..."
    docker network create "$NETWORK_NAME"
  else
    echo "Network '$NETWORK_NAME' already exists."
  fi
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
    if [[ "$service_name" != "frontend" && "$service_name" != "ApiGateway" && "$service_name" != "postman" ]]; then
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
    PRODUCT_FULL_NAME="${PRODUCT_PREFIX}.product.$SERVICE_NAME"
    PRODUCT_FULL_NAME="${PRODUCT_FULL_NAME,,}"

    echo "SERVICE NAME  ${SERVICE_NAME}"
    echo "DB NAME  ${DB_NAME}"

    # handle naming on monolith
    if [[ "$NUM_BACKENDS" -eq 1 ]]; then
      SERVICE_FULL_NAME=$SERVICE_NAME
    fi

    echo "Checking service port for redeployment"
    BE_PORT=$(grep "^${SERVICE_FULL_NAME}-backend-service" "$deployed_ports_file" | cut -d',' -f2 | xargs | tr -d '\r')

    if [ -z "$BE_PORT" ]; then
      echo "Port not found, reserving new port..."
      BE_PORT=$(port_reserver "$SERVICE_FULL_NAME-backend-service" "$deployed_ports_file")      
    fi
    
    echo "PORT USED FOR BE: ${BE_PORT}"

    echo "Setting Up Database"
    database_setup $DB_NAME

    wait_for_db $DB_NAME

    # Making sure hibernate.properties is correct
    HIBERNATE_PROPERTIES_FILE="${product_dir}/${SERVICE_NAME}/${PRODUCT_FULL_NAME}/hibernate.properties"
    if [[ "$DB_URL" == *"localhost"* || "$DB_URL" == *"127.0.0.1"* ]]; then
      sed -i "s/localhost:5432/${BACKEND_DB_URL}/g" "$HIBERNATE_PROPERTIES_FILE"
    else
      sed -i "s/localhost:5432/${DB_URL}/g" "$HIBERNATE_PROPERTIES_FILE"
      BACKEND_DB_URL=$DB_URL
    fi

    echo "Final DB URL: $BACKEND_DB_URL"

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

new_deployment_microservice() {
  init_port_microservice
  generate_nginx_config
  docker_network_setup
  docker_rabbit_mq_and_db
  locate_backend_service
  docker_backend 
  docker_container_fe_gateway
  nginx_setup
  echo "Deployment for $product_name complete"
}

redeployment_microservice() {
  load_ports_from_file_microservice
  generate_nginx_config
  docker_network_setup
  docker_rabbit_mq_and_db
  locate_backend_service
  docker_backend 
  docker_container_fe_gateway
  nginx_setup
  echo "Redeployment for $product_name complete"
}

new_deployment() {
  init_port
  generate_nginx_config
  docker_network_setup
  docker_rabbit_mq_and_db
  locate_backend_service
  docker_backend 
  docker_container_fe
  nginx_setup
  echo "Deployment for $product_name complete"
}

redeployment() {
  load_ports_from_file
  generate_nginx_config
  docker_network_setup
  docker_rabbit_mq_and_db
  locate_backend_service
  docker_backend 
  docker_container_fe
  nginx_setup
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
