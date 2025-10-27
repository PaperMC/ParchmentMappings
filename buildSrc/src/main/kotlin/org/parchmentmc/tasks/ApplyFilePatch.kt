package org.parchmentmc.tasks

import io.codechicken.diffpatch.cli.DiffPatchCli
import io.codechicken.diffpatch.cli.PatchOperation
import io.codechicken.diffpatch.patch.Patcher
import io.codechicken.diffpatch.util.Input.SingleInput
import io.codechicken.diffpatch.util.LogLevel
import io.codechicken.diffpatch.util.Output
import io.codechicken.diffpatch.util.PatchMode
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.parchmentmc.util.path
import java.io.PrintStream
import java.nio.file.StandardOpenOption
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

abstract class ApplyFilePatch : DefaultTask() {
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val patchFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val targetFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val rejectsFile: RegularFileProperty

    @get:Input
    abstract val mode: Property<PatchMode>

    @get:Input
    abstract val minFuzz: Property<String>

    @TaskAction
    fun patch() {
        outputFile.path.createParentDirectories()
        rejectsFile.path.createParentDirectories()
        val log = temporaryDir.resolve("log.txt")
        log.toPath().createParentDirectories().deleteIfExists()

        if (!patchFile.isPresent) {
            targetFile.path.copyTo(outputFile.path)
            return
        }

        val result = PrintStream(log, Charsets.UTF_8).use { logger ->
            PatchOperation.builder()
                .logTo(logger)
                .level(LogLevel.ALL)
                .mode(PatchMode.OFFSET)
                .minFuzz(minFuzz.get().toFloat())
                .summary(false)
                .baseInput(SingleInput.path(targetFile.path))
                .patchesInput(SingleInput.path(patchFile.path))
                .patchedOutput(Output.SingleOutput.path(outputFile.path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))
                .rejectsOutput(Output.SingleOutput.path(rejectsFile.path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))
                .build()
                .operate()
        }

        if (result.exit != 0) {
            throw RuntimeException("Failed to apply patch, exit code ${result.exit}, see log at ${log.absolutePath}")
        }
    }
}
