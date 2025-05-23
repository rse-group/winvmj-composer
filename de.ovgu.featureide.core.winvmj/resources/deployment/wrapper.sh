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
        [SMALL]=t2.small
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

    # Put Username and instance ip in the argument
    set -- "$USERNAME" "$INSTANCE_IP" "$@"
else
    echo "[INFO] Skipping provisioning. Assuming instance is ready and IP is known."
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








