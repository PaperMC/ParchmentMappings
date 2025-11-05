package org.parchmentmc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.parchmentmc.lodestone.util.OfflineChecker
import org.parchmentmc.util.path
import java.net.URI
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

@CacheableTask
abstract class DownloadFile : DefaultTask() {

    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun run() {
        OfflineChecker.checkOffline(project)
        val downloadUrl = URI.create(url.get()).toURL()

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
