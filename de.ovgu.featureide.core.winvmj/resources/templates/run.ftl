echo <#if !isWindows>"</#if>SELECT 'CREATE DATABASE ${dbname}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${dbname}') \gexec<#if !isWindows>"</#if> | psql "postgresql://${dbUsername}:${dbPassword}@localhost"
java -cp ${product} --module-path ${product} -m ${product}