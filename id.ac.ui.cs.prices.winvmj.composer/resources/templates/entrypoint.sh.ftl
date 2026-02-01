#!/bin/sh
# WinVMJ Docker Entrypoint
# Generates hibernate.properties from ENV and starts the application

set -e

# Generate hibernate.properties from environment variables
cat > /app/hibernate.properties << EOF
hibernate.driver_class=org.postgresql.Driver
hibernate.connection.url=${r"${AMANAH_DB_URL}"}
hibernate.connection.username=${r"${AMANAH_DB_USERNAME}"}
hibernate.connection.password=${r"${AMANAH_DB_PASSWORD}"}
hibernate.hbm2ddl.auto=update
hibernate.hbm2ddl.import_files=/app/import.sql
hibernate.dialect=org.hibernate.dialect.PostgreSQL94Dialect
hibernate.show_sql=false
EOF

echo "Generated hibernate.properties with DB_URL: ${r"${AMANAH_DB_URL}"}"

# Combine all SQL files into import.sql for Hibernate auto-seed (idempotent)
if [ -d "/app/sql" ] && [ "$(ls -A /app/sql/*.sql 2>/dev/null)" ]; then
    cat /app/sql/*.sql > /app/import.sql
    echo "Generated import.sql from sql/ folder for auto-seeding"
else
    touch /app/import.sql
    echo "Created empty import.sql (no sql files found)"
fi

# Run the application
# Classpath: jars/* (dependencies) + classes/ (compiled aspects) + . (resources)
<#if hasMonitoringAspect>
exec java -javaagent:jars/aspectjweaver-1.9.22.jar -cp "jars/*:classes:." ${productPackage}.${productName}
<#else>
exec java -cp "jars/*:classes:." ${productPackage}.${productName}
</#if>
