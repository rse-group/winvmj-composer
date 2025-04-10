#!/bin/bash

cleanup() {
    echo "Exiting script..."
    pkill -P $$
    exit 1
}

trap cleanup SIGINT

read -p "Enter the path to the frontend directory: " frontend_dir

echo "SELECT 'CREATE DATABASE ${dbname}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${dbname}') \gexec" | psql "postgresql://${dbUsername}:${dbPassword}@localhost"
for file in ${SQLFolder}/*.sql; do
    psql -a -f "$file" "postgresql://${dbUsername}:${dbPassword}@localhost/${dbname}"
done

java -cp ${product} --module-path ${product} -m ${product} &

cd $frontend_dir && {
    npm install && {
        npm run json:server &
        npm run start &
    }
}

wait