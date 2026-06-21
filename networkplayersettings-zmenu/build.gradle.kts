plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.stephanofer"
version = "2.0.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.groupez.dev/releases")
}

dependencies {
    compileOnly(project(":"))
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.69-stable")

    implementation("com.hera.craftkit:craftkit-zmenu:1.1.0")
    implementation("org.incendo:cloud-paper:2.0.0-beta.15")
    implementation("org.incendo:cloud-minecraft-extras:2.0.0-beta.15")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")
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
        destinationDirectory.set(rootProject.layout.projectDirectory.dir("target"))
        archiveBaseName.set("NetworkPlayerSettingsZMenu")
        archiveClassifier.set("")
        mergeServiceFiles()
        exclude("INFO_BIN", "INFO_SRC", "README")
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        relocate(
            "com.hera.craftkit",
            "com.stephanofer.networkplayersettingszmenu.libs.craftkit"
        )
        relocate(
            "org.incendo.cloud",
            "com.stephanofer.networkplayersettingszmenu.libs.cloud"
        )
        relocate(
            "io.leangen.geantyref",
            "com.stephanofer.networkplayersettingszmenu.libs.geantyref"
        )
        relocate(
            "xyz.jpenilla.reflectionremapper",
            "com.stephanofer.networkplayersettingszmenu.libs.reflectionremapper"
        )
        relocate(
            "net.fabricmc.mappingio",
            "com.stephanofer.networkplayersettingszmenu.libs.mappingio"
        )
        relocate(
            "com.github.benmanes.caffeine",
            "com.stephanofer.networkplayersettingszmenu.libs.caffeine"
        )
    }

    assemble {
        dependsOn(shadowJar)
    }
}
