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
    implementation(libs.bundles.serialize)
    implementation(libs.bundles.xml)
    implementation(libs.bundles.asm)

    implementation("org.parchmentmc:lodestone:0.10.0")
    implementation("org.parchmentmc:compass:0.10.0")

    implementation(libs.enigma)
    implementation(libs.mapping.io)
    implementation(libs.bundles.unpick)
    implementation(libs.diffpatch)
}
