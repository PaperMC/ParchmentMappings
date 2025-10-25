package org.parchmentmc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import org.parchmentmc.compass.tasks.GenerateExport
import org.parchmentmc.compass.tasks.SanitizeData
import org.parchmentmc.compass.tasks.ValidateData
import org.parchmentmc.lodestone.LodestoneExtension
import org.parchmentmc.lodestone.tasks.*
import org.parchmentmc.tasks.DownloadSingleVersionDownload
import org.parchmentmc.tasks.RemapUnpickDefinitions

class BlackstonePlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_TASK_GROUP = "blackstone"
    }

    override fun apply(target: Project) {
        target.apply(plugin = "org.parchmentmc.lodestone")
        target.apply(plugin = "maven-publish")

        val mcVersion = target.providers.gradleProperty("mcVersion")
        val createDistribution = registerTasks(target, mcVersion.get())
        applyUnpick(target)

        val blackstoneConfig = target.configurations.named("blackstone") {
            val cfc = target.files(createDistribution.flatMap { it.archiveFile })
            cfc.builtBy(createDistribution)
            dependencies.add(target.dependencyFactory.create(cfc))
        }
        // Fix faulty task wiring
        target.tasks.withType(SanitizeData::class.java).configureEach {
            inputs.files(blackstoneConfig)
        }
        target.tasks.withType(ValidateData::class.java).configureEach {
            inputs.files(blackstoneConfig)
        }
        target.tasks.withType(GenerateExport::class.java).configureEach {
            inputs.files(blackstoneConfig)
        }

        target.afterEvaluate {
            target.configure<LodestoneExtension> {
                this.mcVersion = mcVersion
            }

            target.configure<PublishingExtension> {
                publications.register<MavenPublication>("blackstone") {
                    artifact(createDistribution)
                    artifactId = "blackstone"
                    version = mcVersion.get()
                    groupId = "org.parchmentmc.data"
                }
            }
        }
    }

    private fun applyUnpick(target: Project) {
        val defs = target.configurations.register("unpickDefinitions")
        val intermediary = target.configurations.register("unpickDefinitionsIntermediary")
        val ext = target.extensions.create<UnpickExtension>("unpick")

        val downloadIntermediaryVersionMeta by target.tasks.registering(DownloadVersionMetadata::class) {
            input = project.tasks.named<DownloadLauncherMetadata>("downloadLauncherMeta").flatMap { it.output }
            output = target.layout.buildDirectory.file("$name-intermediary.json")
            outputs.cacheIf { true }
            mcVersion.set(ext.intermediaryMcVersion)
        }
        val dlMaps = target.tasks.register<DownloadSingleVersionDownload>("downloadIntermediaryMojmap") {
            download.set("client_mappings")
            output.set(project.layout.buildDirectory.file("intermediary-mojmap.txt"))
            versionManifest.set(downloadIntermediaryVersionMeta.flatMap { it.output })
        }
        target.tasks.register<RemapUnpickDefinitions>("remapUnpickDefinitions") {
            inputDefinitionsJar.setFrom(defs)
            outputDefinitionsFile.set(target.layout.buildDirectory.file("definitions.unpick"))
            intermediaryJar.setFrom(intermediary)
            mojangMappings.set(dlMaps.flatMap { it.output })
        }
    }

    private fun registerTasks(target: Project, mcVersion: String): TaskProvider<Zip> {
        val downloadLauncherMeta by target.tasks.registering(DownloadLauncherMetadata::class) {
            group = PLUGIN_TASK_GROUP
        }

        val downloadVersionMeta by target.tasks.registering(DownloadVersionMetadata::class) {
            group = PLUGIN_TASK_GROUP
            input = downloadLauncherMeta.flatMap { it.output }
            output = target.layout.buildDirectory.file(name + ".json") // Remove subdir to fix caching
            outputs.cacheIf { true }
        }

        val buildCachePush = target.providers.gradleProperty("paperBuildCachePush").orNull?.toBoolean()
            ?: System.getProperty("CI").toBoolean()
        val downloadVersion by target.tasks.registering(DownloadVersion::class) {
            group = PLUGIN_TASK_GROUP
            input = downloadVersionMeta.flatMap { it.output }
            // Remove subdir to fix caching
            output = target.layout.buildDirectory.dir(name)
            doFirst {
                output.asFile.get().deleteRecursively()
            }
            outputs.cacheIf { !buildCachePush }
        }

        val createProGuardMetadata by target.tasks.registering(ExtractMetadataFromProguardFile::class) {
            group = PLUGIN_TASK_GROUP
            input = downloadVersion.flatMap { it.output.file("client.txt") }
            outputs.cacheIf { true }
        }

        val createJarMetadata by target.tasks.registering(ExtractMetadataFromJarFiles::class) {
            group = PLUGIN_TASK_GROUP
            input = downloadVersion.flatMap { it.output.file("client.jar") }
            libraries = downloadVersion.flatMap { it.output.dir("libraries") }
            outputs.cacheIf { true }
        }

        val mergeMetadata by target.tasks.registering(MergeMetadata::class) {
            group = PLUGIN_TASK_GROUP
            leftSource = createJarMetadata.flatMap { it.output }
            rightSource = createProGuardMetadata.flatMap { it.output }
            outputs.cacheIf { true }
        }

        val createDistribution by target.tasks.registering(Zip::class) {
            group = PLUGIN_TASK_GROUP
            archiveFileName = "$mcVersion.zip"
            destinationDirectory = project.layout.buildDirectory.dir("dist")

            from(mergeMetadata.flatMap { it.output })
        }
        return createDistribution
    }
}
