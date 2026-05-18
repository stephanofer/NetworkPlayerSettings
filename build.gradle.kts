plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.groupez.dev/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.64-stable")
    implementation("com.stephanofer:network-platform-paper:1.0.0-SNAPSHOT")
    implementation("com.stephanofer:network-platform-database:1.0.0-SNAPSHOT")
    implementation("com.stephanofer:network-platform-hooks:1.0.0-SNAPSHOT")
    implementation("com.stephanofer:network-platform-menus:1.0.0-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
        relocate(
            "com.stephanofer.networkplatform",
            "com.stephanofer.networkplayersettings.libs.networkplatform"
        )
    }

    assemble {
        dependsOn(shadowJar)
    }
}
