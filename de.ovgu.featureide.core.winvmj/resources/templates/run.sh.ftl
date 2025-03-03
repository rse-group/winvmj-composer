#!/bin/bash
source ~/.zshrc  

cleanup() {
    pkill -P $$
    rm java.log
    exit 1
}

trap cleanup SIGINT

java -cp ${product} --module-path ${product} -m ${product} 2>&1 | tee java.log &
JAVA_PID=$!
TEE_PID=$(pgrep -n tee)
tail -f java.log --pid=$TEE_PID | while read -r LINE; do
    if [[ "$LINE" == *"== CREATING OBJECTS AND BINDING ENDPOINTS =="* ]]; then
        break
    fi
done

echo "SELECT 'CREATE DATABASE ${dbname}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${dbname}') \gexec" | psql "postgresql://${dbUsername}:${dbPassword}@localhost"
for file in ${SQLFolder}/*.sql; do
    psql -a -f "$file" "postgresql://${dbUsername}:${dbPassword}@localhost/${dbname}"
done

wait