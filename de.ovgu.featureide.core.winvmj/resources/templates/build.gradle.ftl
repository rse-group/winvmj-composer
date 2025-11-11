plugins {
    id 'java'
    id 'application'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java {
            srcDirs = ['${product}']
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'javax.persistence:javax.persistence-api:2.2'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'org.hibernate:hibernate-core:5.5.0.Final'
    implementation 'com.auth0:java-jwt:4.4.0'
    implementation 'org.postgresql:postgresql:42.7.4'

}


def jarFile = file('${product}/${productName}.jar')
def jf = new java.util.jar.JarFile(jarFile)
def mainCls = jf.manifest?.mainAttributes?.getValue('Main-Class')

// === 1. CREATE DATABASE ===
task createDB(type: Exec) {
    group = 'application'
    description = 'Creates DB if not exists.'

    environment 'PGPASSWORD', '${dbPassword}'
    doFirst {
        println 'Creating database if not exists...'
        def dbName = "${dbname}"
        def sql = "SELECT 'CREATE DATABASE ${dbName}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${dbName}') \\gexec"
        def os = System.getProperty('os.name').toLowerCase()
        if (os.contains('windows')) {
            commandLine 'cmd', '/c', "echo ${sql} | psql -U postgres"
        } else {
            commandLine 'bash', '-c', "echo \"${sql}\" | psql -U postgres"
        }
    }
    doLast {
        println "Database created or already exists."
    }
}

// === 2. RUN HALF (AUTO STOP) ===
task createTable {
    group = 'application'
    description = 'Run the Java app halfway (auto-stop).'
    dependsOn createDB

    doLast {
        println "Starting app (half run)..."
        def process = ['java', '-jar', jarFile.absolutePath].execute()
        Thread.start {
            process.in.eachLine { println it }
        }

        // Wait for ~50% runtime (adjust duration as needed)
        sleep(15000) // 15 seconds as an example
        println "Stopping halfway..."
        process.destroy()
    }
}

// === 3. LOAD SQL ===
task loadSql(type: Exec) {
    group = 'application'
    description = 'Loads all SQL scripts into PostgreSQL.'
    dependsOn createTable

    def sqlDir = file("${rootProject.projectDir}/sql")
    def sqlFiles = sqlDir.exists() ? fileTree(dir: sqlDir, include: ['**/*.sql']).files : []

    doFirst {
        println 'Loading SQL scripts into the database...'
        if (!sqlDir.exists()) throw new GradleException("SQL directory '${sqlDir}' does not exist.")
        if (sqlFiles.isEmpty()) throw new GradleException("No SQL files found in ${sqlDir}.")

        def os = System.getProperty('os.name').toLowerCase()
        if (os.contains('windows')) {
            def command = sqlFiles.collect { f -> "psql -a -f \"${f.absolutePath}\" \"postgresql://postgres:admin@localhost/bankaccount_product_overdraftaccount\"" }.join(" & ")
            commandLine 'cmd', '/c', command
        } else {
            def command = sqlFiles.collect { f -> "psql -a -f \"${f.absolutePath}\" \"postgresql://postgres:admin@localhost/bankaccount_product_overdraftaccount\"" }.join(" ; ")
            commandLine 'bash', '-c', command
        }
    }
    doLast {
        println "All SQL scripts executed."
    }
}

// === 4. RUN FINAL ===
tasks.register("runWinVMJ", JavaExec) {
    group = 'application'
    dependsOn loadSql
    mainClass.set(mainCls)
    classpath = files(jarFile)
    classpath.from fileTree(dir: jarsDirFile, include: ['**/*.jar'])
    classpath.from fileTree(dir: 'libs', include: ['**/*.jar'])
    classpath.from files(project.projectDir)
    classpath.from configurations.runtimeClasspath
}
