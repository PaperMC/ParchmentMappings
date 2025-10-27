package io.papermc.parchment.gradle.task

import io.codechicken.diffpatch.cli.DiffOperation
import io.codechicken.diffpatch.util.LogLevel
import io.codechicken.diffpatch.util.Output
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.parchmentmc.util.path
import java.io.PrintStream
import java.nio.file.StandardOpenOption
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name

abstract class RebuildFilePatch : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val originalFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val modifiedFile: RegularFileProperty

    @get:OutputFile
    abstract val patchFile: RegularFileProperty

    @TaskAction
    fun rebuild() {
        patchFile.path.createParentDirectories()
        val log = temporaryDir.resolve("log.txt")
        log.toPath().createParentDirectories().deleteIfExists()

        val aDir = temporaryDir.resolve("a")
        aDir.deleteRecursively()
        val bDir = temporaryDir.resolve("b")
        bDir.deleteRecursively()
        val patchDir = temporaryDir.resolve("patch")
        patchDir.deleteRecursively()

        val aFile = aDir.resolve(originalFile.path.name).toPath().createParentDirectories()
        originalFile.path.copyTo(aFile)
        val bFile = bDir.resolve(modifiedFile.path.name).toPath().createParentDirectories()
        modifiedFile.path.copyTo(bFile)

        val result = PrintStream(log, Charsets.UTF_8).use { logger ->
            DiffOperation.builder()
                .logTo(logger)
                .level(LogLevel.ALL)
                .summary(false)
                .lineEnding("\n")
                .autoHeader(true)
                .baseInput(io.codechicken.diffpatch.util.Input.MultiInput.folder(aDir.toPath()))
                .changedInput(io.codechicken.diffpatch.util.Input.MultiInput.folder(bDir.toPath()))
                .patchesOutput(
                    Output.SingleOutput.path(
                        patchFile.path,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE
                    )
                )
                .build()
                .operate()
        }
    }
}
