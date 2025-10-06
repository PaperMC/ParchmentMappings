import org.parchmentmc.compass.CompassPlugin
import org.parchmentmc.compass.data.validation.impl.MemberExistenceValidator
import org.parchmentmc.compass.tasks.GenerateExport
import org.parchmentmc.compass.tasks.GenerateSanitizedExport
import org.parchmentmc.compass.tasks.ValidateData
import org.parchmentmc.compass.tasks.VersionDownload
import org.parchmentmc.tasks.*
import org.parchmentmc.util.ArtifactVersionProvider
import org.parchmentmc.util.MemberExistenceValidatorV2

plugins {
    java
    `maven-publish`
    id("org.parchmentmc.compass")
}

//apply<org.parchmentmc.BlackstonePlugin>() apply when Parchment doesn't publish blackstone metadata in time for validate and sanitize tasks

val mcVersion = providers.gradleProperty("mcVersion")
compass {
    version = mcVersion
}

repositories {
    maven("https://libraries.minecraft.net/") {
        name = "Minecraft"
    }
    mavenCentral() // for Enigma's dependency flatlaf
    maven("https://repo.papermc.io/repository/maven-public") {
        name = "PaperMC"
    }
    maven("https://maven.neoforged.net/") {
        name = "NeoForged"
    }
    maven("https://maven.fabricmc.net/") {
        name = "FabricMC"
    }
}

val enigma by configurations.registering
val remapper by configurations.registering
val minecraft by configurations.registering

configurations.jammer {
    val asmVersion = "9.8"
    resolutionStrategy.force(
        "org.ow2.asm:asm-tree:$asmVersion", // bump ParchmentJam's ASM for Java 21+ required since MC 1.20.5
        // update JarAwareMapping versions too to match the new version
        "org.ow2.asm:asm:$asmVersion",
        "org.ow2.asm:asm-util:$asmVersion",
        "org.ow2.asm:asm-commons:$asmVersion"
    )
}

dependencies {
    // MCPConfig for the SRG intermediate
    mcpconfig("de.oceanlabs.mcp:mcp_config:1.19.3-20221207.122022")

    // ART for remapping the client JAR
    remapper("net.neoforged:AutoRenamingTool:2.0.15")

    // Enigma, pretty interface for editing mappings
    enigma("cuchaz:enigma-swing:2.5.2")

    // ParchmentJAM, JAMMER integration for migrating mapping data
    jammer("org.parchmentmc.jam:jam-parchment:0.1.0")

    // Minecraft classpath for inheritance check in ART and later for enigma v3+
    val manifest = project.plugins.getPlugin(CompassPlugin::class).manifestsDownloader.versionManifest
    for (library in manifest.get().libraries) {
        minecraft(library.name) {
            isTransitive = false
        }
    }
}

val downloadClientJar by tasks.registering(VersionDownload::class) {
    group = CompassPlugin.COMPASS_GROUP
    description = "Downloads the client JAR for the current version set in Compass."
}

val remapJar by tasks.registering(RemapJar::class) {
    group = "parchment"
    description = "Remaps the client JAR with the Mojang obfuscation mappings."

    val obfDL = project.plugins.getPlugin(CompassPlugin::class).obfuscationMapsDownloader
    inputJar = downloadClientJar.flatMap { it.outputFile }
    mappings = obfDL.obfuscationMap.flatMap {  _ -> obfDL.clientDownloadOutput }

    remapperClasspath.from(remapper)
    minecraftClasspath.from(minecraft)

    outputJar = project.layout.buildDirectory.dir("remapped")
        .zip(mcVersion) { d, ver -> d.file("$ver-client.jar") }
}

tasks.register<ScanInitParamsJar>("scanInitParams") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    inputJar = remapJar.flatMap { it.outputJar }
    inputMapping = project.compass.productionData
}

tasks.register<ScanParameter>("scanParameter") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    inputMapping = project.compass.productionData
}

tasks.register<JavadocLint>("checkJavadoc") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    inputMapping = project.compass.productionData
}

tasks.register<EnigmaRunner>("enigma") {
    group = "parchment"
    description = "Runs the Enigma mapping tool"
    inputJar = remapJar.flatMap { it.outputJar }
    mappings = project.compass.productionData
}

tasks.withType<ValidateData>().configureEach {
    if (validators.removeIf { validator -> validator is MemberExistenceValidator }) {
        validators.add(MemberExistenceValidatorV2())
    }
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
    from(tasks.named<GenerateExport>("generateOfficialExport").flatMap { it.output })
    archiveBaseName = "officialExport"
}

val officialSanitizedExportZip by tasks.registering(Zip::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Creates a ZIP archive containing the sanitized export produced by the \"official\" intermediate provider and production data"
    from(generateSanitizedExport.flatMap { it.output })
    archiveBaseName = "officialSanitizedExport"
}

val officialStagingExportZip by tasks.registering(Zip::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Creates a ZIP archive containing the export produced by the \"official\" intermediate provider and staging data"
    from(tasks.named<GenerateExport>("generateOfficialStagingExport").flatMap { it.output })
    archiveBaseName = "officialStagingExport"
}

tasks.withType<Zip>().named { name -> name.startsWith("official") }.configureEach {
    rename { "parchment.json" }
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
            name = "paperSnapshots"
            credentials(PasswordCredentials::class)
        }
    }
}
