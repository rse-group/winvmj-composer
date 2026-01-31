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
hibernate.dialect=org.hibernate.dialect.PostgreSQL94Dialect
hibernate.show_sql=false
EOF

echo "Generated hibernate.properties with DB_URL: ${r"${AMANAH_DB_URL}"}"

# Run the application
# All JARs (main + dependencies) are in /app/jars/
<#if hasMonitoringAspect>
exec java -javaagent:jars/aspectjweaver-1.9.22.jar -cp "jars/*:." ${productPackage}.${productName}
<#else>
exec java -cp "jars/*:." ${productPackage}.${productName}
</#if>
