plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.stephanofer"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.groupez.dev/releases")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    compileOnly("me.clip:placeholderapi:2.12.2")

    implementation("com.hera.craftkit:craftkit-database:1.1.0")
    implementation("com.hera.craftkit:craftkit-zmenu:1.1.0")
    implementation("com.stephanofer.boostedyaml:boosted-yaml:1.3.7")
    implementation("org.incendo:cloud-paper:2.0.0-beta.15")
    implementation("org.incendo:cloud-minecraft-extras:2.0.0-beta.15")
    implementation("com.maxmind.geoip2:geoip2:5.1.0")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "networkplayersettings"
            from(components["java"])
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
            "org.incendo.cloud",
            "com.stephanofer.networkplayersettings.libs.cloud"
        )
        relocate(
            "io.leangen.geantyref",
            "com.stephanofer.networkplayersettings.libs.geantyref"
        )
        relocate(
            "xyz.jpenilla.reflectionremapper",
            "com.stephanofer.networkplayersettings.libs.reflectionremapper"
        )
        relocate(
            "net.fabricmc.mappingio",
            "com.stephanofer.networkplayersettings.libs.mappingio"
        )
        relocate(
            "com.maxmind",
            "com.stephanofer.networkplayersettings.libs.maxmind"
        )
        relocate(
            "com.zaxxer",
            "com.stephanofer.networkplayersettings.libs.zaxxer"
        )
    }

    assemble {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}
