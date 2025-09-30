#!/bin/bash

file "$0" | grep CRLF && echo "Warning: This script has CRLF line endings!" && exit 1

# Get the directory of the current script and set it as current dir
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

MODE="$1"  # systemd or docker
PROVISION="$2"  # yes or no

if [[ "$MODE" != "systemd" && "$MODE" != "docker" ]]; then
    echo "Usage: $0 <systemd|docker> <yes|no> [args...]"
    exit 1
fi

if [[ "$PROVISION" != "yes" && "$PROVISION" != "no" ]]; then
    echo "Usage: $0 <systemd|docker> <yes|no> [args...]"
    exit 1
fi

# Shift two argument to the left (remove MODE and PROVISION)
shift 2

if [[ "$PROVISION" == "yes" ]]; then
    echo "[INFO] Provisioning VM using Terraform..."
    chmod +x terraform/create_vm.sh

    declare -A GCP_MACHINE_TYPE_MAP=(
        [SMALL]=e2-small
        [MEDIUM]=e2-medium
        [LARGE]=e2-standard-2
    )
    declare -A GCP_ZONE_MAP=(
        [US]=us-central1-a
        [SINGAPORE]=asia-southeast1-b
        [JAKARTA]=asia-southeast2-a
    )
    declare -A AWS_MACHINE_TYPE_MAP=(
        [SMALL]=t3.small
        [MEDIUM]=t2.medium
        [LARGE]=t2.large
    )
    declare -A AWS_ZONE_MAP=(
        [US]=us-east-1
        [SINGAPORE]=ap-southeast-1
        [EUROPE]=eu-central-1
    )
  
    USERNAME="$1"
    MACHINE_TYPE="$2"
    ZONE="$3"
    CREDENTIALS="$4"
    PROVIDER="$5"
    INSTANCE_NAME="$6"
    PUBLIC_KEY="$7"

    if [ "$PROVIDER" == "aws" ]; then
        USERNAME="ubuntu"
        MACHINE_TYPE=${AWS_MACHINE_TYPE_MAP[$2]}
        ZONE=${AWS_ZONE_MAP[$3]}
    elif [ "$PROVIDER" == "gcp" ]; then
        USERNAME=$1
        MACHINE_TYPE=${GCP_MACHINE_TYPE_MAP[$2]}
        ZONE=${GCP_ZONE_MAP[$3]}
    else
        echo "Error: Unsupported provider!"
        exit 1
    fi

    ./terraform/create_vm.sh "$USERNAME" "$MACHINE_TYPE" "$ZONE" "$CREDENTIALS" "$PROVIDER" "$INSTANCE_NAME" "$PUBLIC_KEY"

    echo "Please wait for a minute to make sure your instance is ready..."
    sleep 60

    INSTANCE_IP=$(cat instance_ip.txt)
    echo "[INFO] Instance IP: $INSTANCE_IP"
    shift 7

    # Auto-detect deployment mode based on certificate name
    # After shift 7, parameters are: product_name, cert_name, nginx_cert_name, product_prefix, zip_path, private_key, threads
    PRODUCT_NAME="$1"
    CERT_NAME="$2"
    NGINX_CERT_NAME="$3"
    
    # If certificate names are placeholders for HTTP mode, replace with IP
    if [[ "$CERT_NAME" == "HTTP_PLACEHOLDER" ]] || [[ "$NGINX_CERT_NAME" == "HTTP_PLACEHOLDER" ]]; then
        echo "[INFO] HTTP-only deployment detected. Using IP address for certificates."
        # Skip product_name, and replace the two placeholder parameters, keep the rest
        shift 1  # Remove product_name
        shift 2  # Remove the two HTTP_PLACEHOLDER parameters 
        set -- "$USERNAME" "$INSTANCE_IP" "$PRODUCT_NAME" "$INSTANCE_IP" "$INSTANCE_IP" "$@"
    else
        # HTTPS deployment with domain names
        echo "[INFO] HTTPS deployment detected. Using provided domain names."
        set -- "$USERNAME" "$INSTANCE_IP" "$@"
    fi
else
    echo "[INFO] Skipping provisioning. Reading existing IP from instance_ip.txt..."
    
    if [[ -f "instance_ip.txt" ]]; then
        INSTANCE_IP=$(cat instance_ip.txt | tr -d '[:space:]')
        echo "[INFO] Found existing IP: $INSTANCE_IP"
        
        # For HTTP-only deployment with existing IP, use this format:
        # username, ip, certificate_name(ip), nginx_certificate_name(ip), product, zip, private_key, threads
        USERNAME="ubuntu"  # Default for AWS
        
        # Auto-detect if first parameter is not username format (starts with /)
        if [[ "$1" =~ ^/.* ]] || [[ "$1" =~ .*\.zip$ ]] || [[ "$1" == "bankaccount" ]]; then
            # Parameters are: product, zip, private_key, threads
            echo "[INFO] Auto-detected parameter format: product zip private_key threads"
            set -- "$USERNAME" "$INSTANCE_IP" "$INSTANCE_IP" "$INSTANCE_IP" "$@"
        else
            # Parameters include username: username, product, zip, private_key, threads  
            echo "[INFO] Using provided parameters with detected IP"
            USERNAME="$1"
            shift 1
            set -- "$USERNAME" "$INSTANCE_IP" "$INSTANCE_IP" "$INSTANCE_IP" "$@"
        fi
    else
        echo "[ERROR] instance_ip.txt not found! Please provision infrastructure first."
        echo "[INFO] Use: bash wrapper.sh docker yes ubuntu SMALL SINGAPORE /path/to/credentials.json aws instance-name ~/.ssh/id_ed25519.pub"
        exit 1
    fi
fi


if [[ "$MODE" == "systemd" ]]; then
    echo "Deploying with systemd..."
    chmod +x systemd/systemd_wrapper.sh
    ./systemd/systemd_wrapper.sh "$@"
elif [[ "$MODE" == "docker" ]]; then
    echo "Deploying with docker..."
    chmod +x docker/docker_wrapper.sh
    ./docker/docker_wrapper.sh "$@"
else
    echo "Usage: $0 <systemd|docker> [args...]"
    exit 1
fi








