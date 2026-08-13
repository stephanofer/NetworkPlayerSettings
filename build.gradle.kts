plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.stephanofer"
version = "2.1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.groupez.dev/releases")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.25-alpha")
    compileOnly("me.clip:placeholderapi:2.12.2")

    implementation("com.hera.craftkit:craftkit-database:2.0.0")
    implementation("com.stephanofer.boostedyaml:boosted-yaml:1.3.7")
    implementation("com.maxmind.geoip2:geoip2:5.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:26.2.build.25-alpha")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "networkplayersettings"
            artifact(tasks.named("jar"))
        }
    }
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        destinationDirectory.set(rootProject.layout.projectDirectory.dir("target"))
        archiveClassifier.set("")
        mergeServiceFiles()
        exclude("INFO_BIN", "INFO_SRC", "README")
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        relocate(
            "com.hera.craftkit",
            "com.stephanofer.networkplayersettings.libs.craftkit"
        )
        relocate(
            "dev.dejvokep.boostedyaml",
            "com.stephanofer.networkplayersettings.libs.boostedyaml"
        )
        relocate(
            "com.maxmind",
            "com.stephanofer.networkplayersettings.libs.maxmind"
        )
        relocate(
            "com.zaxxer",
            "com.stephanofer.networkplayersettings.libs.zaxxer"
        )
        relocate(
            "org.flywaydb",
            "com.stephanofer.networkplayersettings.libs.flyway"
        )
        relocate(
            "tools.jackson",
            "com.stephanofer.networkplayersettings.libs.jackson3"
        )
        relocate(
            "com.fasterxml.jackson",
            "com.stephanofer.networkplayersettings.libs.jackson"
        )
        relocate(
            "com.mysql",
            "com.stephanofer.networkplayersettings.libs.mysql"
        )
        relocate(
            "com.google.protobuf",
            "com.stephanofer.networkplayersettings.libs.protobuf"
        )
        relocate(
            "com.github.benmanes.caffeine",
            "com.stephanofer.networkplayersettings.libs.caffeine"
        )
    }

    assemble {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}
