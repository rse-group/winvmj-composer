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

def jarsDirFile = file('${product}')
def jarFile = file('${product}/${productName}.jar')
def jf = new java.util.jar.JarFile(jarFile)
def mainCls = jf.manifest?.mainAttributes?.getValue('Main-Class')

def dbUser = '${dbUsername}'
def dbPass = '${dbPassword}'
def dbName = "${dbname}"

// === 1. CREATE DATABASE ===
task createDB(type: Exec) {
    group = 'application'
    description = 'Creates DB if not exists.'

    <#noparse>
    environment 'PGPASSWORD', dbPass
    doFirst {
        println 'Creating database if not exists...'
        def sql = "SELECT 'CREATE DATABASE ${dbName}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${dbName}') \\gexec"
        def os = System.getProperty('os.name').toLowerCase()
        if (os.contains('windows')) {
            commandLine 'cmd', '/c', "echo ${sql} | psql -U ${dbUser}"
        } else {
            commandLine 'bash', '-c', "echo \"${sql}\" | psql -U ${dbUser}"
        }
        </#noparse>
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
            process.in.eachLine { line -> 
            	println line 
            }
        }

        // Wait for creating database table (adjust duration as needed)
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
    <#noparse>
    def sqlDir = file("${rootProject.projectDir}/sql")
    def sqlFiles = sqlDir.exists() ? fileTree(dir: sqlDir, include: ['**/*.sql']).files : []

    doFirst {
        println 'Loading SQL scripts into the database...'
        if (!sqlDir.exists()) throw new GradleException("SQL directory '${sqlDir}' does not exist.")
        if (sqlFiles.isEmpty()) throw new GradleException("No SQL files found in ${sqlDir}.")

        def os = System.getProperty('os.name').toLowerCase()
        if (os.contains('windows')) {
    
            def command = sqlFiles.collect { f -> "psql -a -f \"${f.absolutePath}\" \"postgresql://${dbUser}:${dbPass}@localhost/${dbName}\"" }.join(" & ")
            commandLine 'cmd', '/c', command
        } else {
            def command = sqlFiles.collect { f -> "psql -a -f \"${f.absolutePath}\" \"postgresql://${dbUser}:${dbPass}@localhost/${dbName}\"" }.join(" ; ")
            commandLine 'bash', '-c', command
        }
    }
    </#noparse>
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
