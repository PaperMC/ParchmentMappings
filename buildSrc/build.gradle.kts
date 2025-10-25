plugins {
    java
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.2.0" // stay in sync with gradle built-in version of the DSL
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://maven.parchmentmc.org/") {
        name = "ParchmentMC"
    }
    maven("https://maven.fabricmc.net/") {
        name = "FabricMC"
    }
    maven("https://maven.minecraftforge.net/") {
        name = "MinecraftForge"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("io.github.pdvrieze.xmlutil:core-jvm:0.86.3")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.86.3")

    implementation("org.ow2.asm:asm:9.9")
    implementation("org.ow2.asm:asm-tree:9.9")

    implementation("org.parchmentmc:lodestone:0.10.0")
    implementation("org.parchmentmc:compass:0.10.0")
    implementation("cuchaz:enigma:4.0.2")
    implementation("org.cadixdev:lorenz:0.5.8")
    implementation("org.cadixdev:lorenz-io-enigma:0.5.8")

    implementation("net.fabricmc.unpick:unpick:3.0.0-beta.11")
    implementation("net.fabricmc.unpick:unpick-format-utils:3.0.0-beta.11")
    implementation("net.fabricmc:mapping-io:0.7.1")
}
