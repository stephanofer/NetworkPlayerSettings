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
    implementation("com.stephanofer:network-platform-paper:1.0.1-SNAPSHOT")
    implementation("com.stephanofer:network-platform-database:1.0.1-SNAPSHOT")
    implementation("com.stephanofer:network-platform-hooks:1.0.1-SNAPSHOT")
    implementation("com.stephanofer:network-platform-menus:1.0.1-SNAPSHOT")
    implementation("com.maxmind.geoip2:geoip2:5.1.0")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.64-stable")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
        relocate(
            "com.maxmind",
            "com.stephanofer.networkplayersettings.libs.maxmind"
        )
    }

    assemble {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}
