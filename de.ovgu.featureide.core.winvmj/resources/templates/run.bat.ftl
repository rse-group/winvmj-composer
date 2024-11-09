echo SELECT 'CREATE DATABASE ${dbname}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${dbname}') \gexec | psql "postgresql://${dbUsername}:${dbPassword}@localhost"
for %%G in (${SQLFolder}/*.sql) do psql -a -f ${SQLFolder}/%%G "postgresql://${dbUsername}:${dbPassword}@localhost/${dbname}"

java -cp ${product} --module-path ${product} -m ${product}