source ~/.zshrc  

echo "SELECT 'CREATE DATABASE ${dbname}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${dbname}') \gexec" | psql "postgresql://${dbUsername}:${dbPassword}@localhost"
for file in ${SQLFolder}/*.sql; do
    psql -a -f "$file" "postgresql://${dbUsername}:${dbPassword}@localhost/${dbname}"
done

java -cp ${product} --module-path ${product} -m ${product}