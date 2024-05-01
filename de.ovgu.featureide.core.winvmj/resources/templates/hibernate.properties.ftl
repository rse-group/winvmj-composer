hibernate.driver_class = org.postgresql.Driver
hibernate.connection.url = jdbc:postgresql://localhost:5432/${dbname}
hibernate.connection.username = ${dbUsername}
hibernate.connection.password = ${dbPassword}
hibernate.hbm2ddl.auto = update
hibernate.dialect = org.hibernate.dialect.PostgreSQL94Dialect
hibernate.show_sql = false