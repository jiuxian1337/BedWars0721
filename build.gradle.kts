plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0-beta8"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/nms/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.alessiodp.com/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
    compileOnly(fileTree("libs"))
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    compileOnly("org.spongepowered:configurate-yaml:4.2.0")
    implementation("com.alessiodp.libby:libby-bukkit:2.0.0-SNAPSHOT")
    compileOnly("org.ow2.asm:asm:9.10.1")
    compileOnly("org.ow2.asm:asm-tree:9.10.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    val buildNative by registering(Exec::class) {
        group = "native"
        description = "Build native libraries for all platforms using Zig"
        workingDir = file("NativeUtils")
        commandLine("zig", "build", "all")
    }

    val copyNativeLibs by registering(Copy::class) {
        group = "native"
        description = "Copy built native libraries into resources"
        dependsOn(buildNative)
        from(fileTree("NativeUtils/zig-out"))
        into(layout.buildDirectory.dir("resources/main/natives"))
    }

    processResources {
        dependsOn(copyNativeLibs)
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        relocate("com.alessiodp.libby", "cc.xpWars.libby")
    }

    runServer {
        minecraftVersion("1.8.8")
        systemProperty("com.mojang.eula.agree", "true")
        jvmArgs("-Xmx2G", "-Xms2G")
        runDirectory(file("run"))
    }
}
