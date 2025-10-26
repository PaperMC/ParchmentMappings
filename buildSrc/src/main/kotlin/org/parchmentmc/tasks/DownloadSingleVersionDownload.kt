package org.parchmentmc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.parchmentmc.feather.manifests.VersionManifest
import org.parchmentmc.lodestone.tasks.DownloadLauncherMetadata
import org.parchmentmc.lodestone.util.OfflineChecker
import org.parchmentmc.util.path
import java.io.FileReader
import java.net.URI
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.io.path.createParentDirectories

@CacheableTask
abstract class DownloadSingleVersionDownload : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(value = PathSensitivity.NONE)
    abstract val versionManifest: RegularFileProperty

    @get:Input
    abstract val download: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun run() {
        OfflineChecker.checkOffline(project)
        val gson = DownloadLauncherMetadata.getLauncherManifestGson()
        val manifest = FileReader(versionManifest.asFile.get()).use { r ->
            gson.fromJson(r, VersionManifest::class.java)
        }
        val fileInfo = manifest.downloads[download.get()]
            ?: error("Download '${download.get()}' not found in version manifest")
        val downloadUrl = URI.create(fileInfo.url).toURL()

        output.path.createParentDirectories()
        Channels.newChannel(downloadUrl.openStream()).use { input ->
            FileChannel.open(
                output.path,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { output ->
                output.transferFrom(input, 0L, Long.MAX_VALUE)
            }
        }
    }
}
