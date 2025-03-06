echo SELECT 'CREATE DATABASE <service_database_name>' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '<service_database_name>') \gexec | psql "postgresql://postgres:postgres@localhost"
for %%G in (sql/*.sql) do psql -a -f sql/%%G "postgresql://postgres:postgres@localhost/<service_database_name>"

java -cp . --module-path . -m <service.module.name>