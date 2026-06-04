plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/nms/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
    implementation(fileTree("libs"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(11)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
