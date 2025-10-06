package org.parchmentmc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import org.parchmentmc.lodestone.LodestoneExtension
import org.parchmentmc.lodestone.tasks.*

const val PLUGIN_TASK_GROUP = "blackstone"

class BlackstonePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.apply(plugin = "org.parchmentmc.lodestone")
        target.apply(plugin = "maven-publish")

        val mcVersion = target.providers.gradleProperty("mcVersion")
        val createDistribution = registerTasks(target, mcVersion.get())

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

    private fun registerTasks(target: Project, mcVersion: String): TaskProvider<Zip> {
        val downloadLauncherMeta by target.tasks.registering(DownloadLauncherMetadata::class) {
            group = PLUGIN_TASK_GROUP
        }

        val downloadVersionMeta by target.tasks.registering(DownloadVersionMetadata::class) {
            group = PLUGIN_TASK_GROUP
            input = downloadLauncherMeta.flatMap { it.output }
        }

        val downloadVersion by target.tasks.registering(DownloadVersion::class) {
            group = PLUGIN_TASK_GROUP
            input = downloadVersionMeta.flatMap { it.output }
        }

        val createProGuardMetadata by target.tasks.registering(ExtractMetadataFromProguardFile::class) {
            group = PLUGIN_TASK_GROUP
            input = downloadVersion.flatMap { it.output.file("client.txt") }
        }

        val createJarMetadata by target.tasks.registering(ExtractMetadataFromJarFiles::class) {
            group = PLUGIN_TASK_GROUP
            input = downloadVersion.flatMap { it.output.file("client.jar") }
            libraries = downloadVersion.flatMap { it.output.dir("libraries") }
        }

        val mergeMetadata by target.tasks.registering(MergeMetadata::class) {
            group = PLUGIN_TASK_GROUP
            leftSource = createJarMetadata.flatMap { it.output }
            rightSource = createProGuardMetadata.flatMap { it.output }
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
