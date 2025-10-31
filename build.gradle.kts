import org.parchmentmc.compass.CompassPlugin
import org.parchmentmc.compass.data.validation.impl.MemberExistenceValidator
import org.parchmentmc.compass.data.validation.impl.MethodStandardsValidator
import org.parchmentmc.compass.tasks.*
import org.parchmentmc.tasks.*
import org.parchmentmc.util.ArtifactVersionProvider
import org.parchmentmc.util.replace
import org.parchmentmc.validation.DuplicateData
import org.parchmentmc.validation.MemberExistenceValidatorV2
import org.parchmentmc.validation.MethodStandardsValidatorV2

plugins {
    java
    `kotlin-dsl`
    `maven-publish`
    id("org.parchmentmc.compass")
    id("blackstone")
    id("unpick")
}

val mcVersion = providers.gradleProperty("mcVersion")
compass {
    version = mcVersion
}

repositories {
    maven("https://libraries.minecraft.net/") {
        name = "Minecraft"
    }
    mavenCentral() // for Enigma's dependency flatlaf
    maven("https://maven.neoforged.net/") {
        name = "NeoForged"
    }
    maven("https://maven.fabricmc.net/") {
        name = "FabricMC"
    }
}

val enigmaPlugin by sourceSets.creating

val enigma by configurations.registering {
    extendsFrom(configurations.getByName(enigmaPlugin.implementationConfigurationName))
}
val remapper by configurations.registering
val minecraft by configurations.registering {
    isTransitive = false
}

configurations.jammer {
    resolutionStrategy.force(
        libs.asm.tree, // bump ParchmentJam's ASM for Java 21+ required since MC 1.20.5
        // update JarAwareMapping versions too to match the new version
        libs.asm.core,
        libs.asm.util,
        libs.asm.commons
    )
}

dependencies {
    // MCPConfig for the SRG intermediate
    mcpconfig(libs.mcp.config)

    // ART for remapping the client JAR
    remapper(libs.art)

    // Enigma, pretty interface for editing mappings
    enigma(libs.enigma.gui)
    enigma(libs.vineflower)

    enigma(enigmaPlugin.output)

    enigmaPlugin.implementationConfigurationName(gradleKotlinDsl())
    enigmaPlugin.implementationConfigurationName(libs.bundles.enigma.plugin)

    // ParchmentJAM, JAMMER integration for migrating mapping data
    jammer(libs.jammer)

    // Minecraft classpath for inheritance check in ART and to prevent types coming from libraries to be printed as FQN in enigma
    val manifest = project.plugins.getPlugin(CompassPlugin::class).manifestsDownloader.versionManifest
    for (library in manifest.get().libraries) {
        minecraft(library.name)
    }
}

// For unpick definitions
val yarnVersion = "25w44a+build.4"
val intermediaryVersion = yarnVersion.substringBefore('+')

dependencies {
    unpickDefinitions("net.fabricmc:yarn:${yarnVersion}:extras")
    unpickDefinitionsIntermediary("net.fabricmc:intermediary:${intermediaryVersion}:v2")
}

unpick {
    disablePatch = false
    intermediaryMcVersion = intermediaryVersion
    remapIntermediaryDefinitions = true
}

val remapJar by tasks.registering(RemapJar::class) {
    group = "parchment"
    description = "Remaps the client JAR with the Mojang obfuscation mappings."

    inputJar = tasks.downloadVersion.flatMap { it.output.file("client.jar") }
    mappings = tasks.downloadVersion.flatMap { it.output.file("client.txt") }

    remapperClasspath.setFrom(remapper)
    minecraftClasspath.setFrom(minecraft)

    outputJar = project.layout.buildDirectory.dir("remapped")
        .zip(mcVersion) { d, ver -> d.file("$ver-client.jar") }
}

tasks.register<ScanConstructorParameters>("scanInitParams") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    inputJar = remapJar.flatMap { it.outputJar }
    inputMapping = project.compass.productionData
}

tasks.register<ScanParameter>("scanParam") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    inputMapping = project.compass.productionData
    inputJar = remapJar.flatMap { it.outputJar }
}

tasks.register<EnigmaRunner>("enigma") {
    group = "parchment"
    description = "Runs the Enigma mapping tool"
    classpath(enigma)
    mainClass = "cuchaz.enigma.gui.Main"
    inputJar = remapJar.flatMap { it.outputJar }
    mappings = project.compass.productionData
    profile = project.layout.projectDirectory.file("enigma_profile.json")
    libraries.setFrom(minecraft)
}

tasks.withType<ValidateData>().configureEach {
    validators.replace(MemberExistenceValidator::class) { -> MemberExistenceValidatorV2() }
    validators.replace(MethodStandardsValidator::class) { -> MethodStandardsValidatorV2() }
    //validators.add(DuplicateData.DataValidator())
}

tasks.withType<SanitizeData>().configureEach {
    //sanitizers.add(DuplicateData.DataSanitizer())
}

tasks.withType<GenerateExport>().configureEach {
    // Disable blackstone if UPDATING is set.
    // This will ensure cascaded method data does not get mixed into the production data when updating.
    useBlackstone = !project.findProperty("UPDATING")?.toString().toBoolean()
}

val artifactVersionProvider = providers.of(ArtifactVersionProvider::class) {
    parameters {
        repoUrl = "https://artifactory.papermc.io/artifactory/releases/"
        version = mcVersion
        ci = providers.environmentVariable("CI").map { it.toBooleanStrict() }.orElse(false)
    }
}

val generateSanitizedExport by tasks.registering(GenerateSanitizedExport::class) {
    group = CompassPlugin.COMPASS_GROUP
    description = "Generates an export file using the \"official\" intermediate provider and production data"
    input = project.compass.productionData
    inputFormat = project.compass.productionDataFormat
}

val officialExportZip by tasks.registering(Zip::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Creates a ZIP archive containing the export produced by the \"official\" intermediate provider and production data"
    from(tasks.generateOfficialExport.flatMap { it.output })
    unpick.includeDefinitions(this)
    archiveBaseName = "officialExport"
}

val officialSanitizedExportZip by tasks.registering(Zip::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Creates a ZIP archive containing the sanitized export produced by the \"official\" intermediate provider and production data"
    from(generateSanitizedExport.flatMap { it.output })
    unpick.includeDefinitions(this)
    archiveBaseName = "officialSanitizedExport"
}

val officialStagingExportZip by tasks.registering(Zip::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Creates a ZIP archive containing the export produced by the \"official\" intermediate provider and staging data"
    from(tasks.generateOfficialStagingExport.flatMap { it.output })
    unpick.includeDefinitions(this)
    archiveBaseName = "officialStagingExport"
}

tasks.withType<Zip>().named { name -> name.startsWith("official") }.configureEach {
    rename(".*\\.json", "parchment.json")
    destinationDirectory = project.layout.buildDirectory.dir("exportZips")
}

publishing {
    publications.withType(MavenPublication::class).configureEach {
        pom {
            name = "Parchment Mappings"
            description = "Parameter names and javadoc mappings for Minecraft: Java Edition."
            organization {
                name = "ParchmentMC"
                url = "https://github.com/ParchmentMC"
            }
            licenses {
                license {
                    name = "CC0-1.0"
                    url = "https://creativecommons.org/publicdomain/zero/1.0/legalcode"
                }
            }
            properties.put("minecraft_version", mcVersion)
        }
        artifactId = "parchment"
    }

    publications.register<MavenPublication>("export") {
        artifact(officialExportZip)
        artifact(officialSanitizedExportZip) {
            classifier = "checked"
        }
        version = artifactVersionProvider.get()
    }

    publications.register<MavenPublication>("staging") {
        artifact(officialStagingExportZip)
        version = "staging-SNAPSHOT"
    }
    repositories {
        maven("https://artifactory.papermc.io/artifactory/releases/") {
            name = "paper"
            credentials(PasswordCredentials::class)
        }
    }
}

val printVersion = tasks.register("printVersion") {
    inputs.property("ver", artifactVersionProvider)
    doFirst {
        println(artifactVersionProvider.get())
    }
}
