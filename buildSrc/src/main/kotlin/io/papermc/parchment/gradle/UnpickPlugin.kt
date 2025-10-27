package io.papermc.parchment.gradle

import io.codechicken.diffpatch.util.PatchMode
import io.papermc.parchment.gradle.task.ApplyFilePatch
import io.papermc.parchment.gradle.task.DownloadSingleVersionDownload
import io.papermc.parchment.gradle.task.RebuildFilePatch
import io.papermc.parchment.gradle.task.RemapUnpickDefinitions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.parchmentmc.BlackstonePlugin
import org.parchmentmc.lodestone.tasks.DownloadLauncherMetadata
import org.parchmentmc.lodestone.tasks.DownloadVersionMetadata
import org.parchmentmc.util.fileExists
import java.io.File

abstract class UnpickPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.apply(BlackstonePlugin::class) // Ensure downloadLauncherMeta exists

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
        val remap = target.tasks.register<RemapUnpickDefinitions>("remapUnpickDefinitions") {
            inputDefinitionsJar.setFrom(defs)
            outputDefinitionsFile.set(target.layout.buildDirectory.file("definitions.unpick"))
            intermediaryJar.setFrom(intermediary)
            mojangMappings.set(dlMaps.flatMap { it.output })
        }
        val patchUnpickDefinitions = target.tasks.register<ApplyFilePatch>("applyUnpickDefinitionsPatch") {
            patchFile.convention(project.provider { project.layout.projectDirectory.file("definitions.unpick.patch") }
                .fileExists())
            targetFile.convention(remap.flatMap { it.outputDefinitionsFile })
            mode.convention(PatchMode.OFFSET)
            minFuzz.convention("0.5")
            outputFile.convention(target.layout.projectDirectory.file("definitions.unpick"))
            rejectsFile.convention(target.layout.projectDirectory.file("definitions.unpick.rej"))
        }
        val rebuildUnpickPatch = target.tasks.register<RebuildFilePatch>("rebuildUnpickDefinitionsPatch") {
            originalFile.convention(remap.flatMap { it.outputDefinitionsFile })
            modifiedFile.convention(target.layout.projectDirectory.file("definitions.unpick"))
            patchFile.convention(project.layout.projectDirectory.file("definitions.unpick.patch"))
        }
        target.afterEvaluate {
            if (!ext.remapIntermediaryDefinitions.get()) {
                patchUnpickDefinitions {
                    targetFile.convention(target.layout.file(defs.map { it.singleFile }))
                }
                rebuildUnpickPatch {
                    originalFile.convention(target.layout.file(defs.map { it.singleFile }))
                }
            }
            if (ext.disablePatch.get()) {
                patchUnpickDefinitions {
                    patchFile.set(null as File?)
                }
            }
        }
    }
}
