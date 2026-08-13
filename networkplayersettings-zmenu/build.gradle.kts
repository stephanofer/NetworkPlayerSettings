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

    implementation("com.hera.craftkit:craftkit-zmenu:2.0.0")
    compileOnly("fr.maxlego08.menu:zmenu-api:1.1.1.7")
    implementation("com.stephanofer.boostedyaml:boosted-yaml:1.3.7")
    implementation(platform("org.incendo:cloud-minecraft-bom:2.0.0"))
    implementation("org.incendo:cloud-paper")
    implementation("org.incendo:cloud-minecraft-extras")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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
            "dev.dejvokep.boostedyaml",
            "com.stephanofer.networkplayersettingszmenu.libs.boostedyaml"
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

    test {
        useJUnitPlatform()
    }
}
