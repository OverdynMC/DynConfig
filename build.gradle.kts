import java.util.Properties

// --- Load .env file ---
val envProps = Properties()
val envFile = rootProject.file(".env")

if (envFile.exists()) {
    envFile.forEachLine { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
            val (key, value) = trimmed.split("=", limit = 2)
            envProps[key.trim()] = value.trim()
        }
    }
    println("✔ Loaded .env variables")
} else {
    println("⚠ No .env file found")
}


plugins {
    java
    `maven-publish`
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "xyz.overdyn"
version = "1.0.0.24"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")

    // SpongePowered Configurate for YAML/JSON/HOCON support
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("org.spongepowered:configurate-gson:4.1.2")
    implementation("org.spongepowered:configurate-hocon:4.1.2")
}

tasks.runServer {
    minecraftVersion("1.18.2")
}

// Task to run manual tests
tasks.register<JavaExec>("runManualTest") {
    group = "verification"
    description = "Runs manual ConfigManager test"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("xyz.overdyn.dynconfig.ConfigManagerTest")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "dynconfig"
            version = project.version.toString()

            pom {
                name.set("dynconfig")
                description.set("Config Framework for Overdyn")
                url.set("https://overdyn.xyz")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("Overdyn")
                        name.set("Overdyn Studio")
                        email.set("support@overdyn.xyz")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            val isSnapshot = project.version.toString().endsWith("SNAPSHOT")
            val repoType = if (isSnapshot) "snapshots" else "releases"

            name = "overdynRepo"
            url = uri("https://repo.overdyn.xyz/$repoType")

            credentials {
                username = envProps["REPOSILITE_USER"]?.toString()
                password = envProps["REPOSILITE_TOKEN"]?.toString()
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
