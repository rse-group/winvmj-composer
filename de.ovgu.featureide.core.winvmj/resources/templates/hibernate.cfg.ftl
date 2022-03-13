<?xml version='1.0' encoding='utf-8'?>
<!--
  ~ Hibernate, Relational Persistence for Idiomatic Java
  ~
  ~ License: GNU Lesser General Public License (LGPL), version 2.1 or later.
  ~ See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
  -->
<!DOCTYPE hibernate-configuration PUBLIC
        "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
        "http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">

<hibernate-configuration>

    <session-factory>

        <!-- Database connection settings -->
        <property name="connection.driver_class">org.postgresql.Driver</property>
        <property name="connection.url">jdbc:postgresql://localhost:5432/${dbname}</property>
        <property name="connection.username">${dbUsername}</property>
        <property name="connection.password">${dbPassword}</property>

        <!-- JDBC connection pool (use the built-in) -->
        <!-- <property name="connection.pool_size">1</property> -->

        <!-- set to create to re-create database, set to update to modify the database (the record still intact)-->
        <property name="hbm2ddl.auto">update</property>

        <!-- SQL dialect -->
        <!-- <property name="dialect">org.hibernate.dialect.MySQL5Dialect</property> -->
        <property name="dialect">org.hibernate.dialect.PostgreSQLDialect</property>
        
        <!-- <property name="current_session_context_class">thread</property> -->

        <!-- Disable the second-level cache  -->
        <!-- <property name="cache.provider_class">org.hibernate.cache.internal.NoCacheProvider</property> -->

        <!-- Echo all executed SQL to stdout -->
        <property name="show_sql">false</property>

    </session-factory>

</hibernate-configuration>