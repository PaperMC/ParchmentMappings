plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") {
        name = "FabricMC"
    }
}

dependencies {
    implementation(libs.enigma)
    implementation(libs.bundles.asm)
}
