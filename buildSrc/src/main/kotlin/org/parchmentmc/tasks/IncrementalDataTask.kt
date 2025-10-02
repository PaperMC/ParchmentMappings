package org.parchmentmc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class IncrementalDataTask : DefaultTask() {

    init {
        // Ignore outputs for up-to-date checks as there aren't any (so only inputs are checked)
        outputs.upToDateWhen { true }
    }

    @get:Incremental
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputMapping: DirectoryProperty

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    abstract fun execute(changes: InputChanges)

    @TaskAction
    fun run(changes: InputChanges) {
        val fileChanged =
            changes.getFileChanges(inputMapping).asSequence()
                .filter { change -> change.changeType !== ChangeType.REMOVED && change.fileType === FileType.FILE }
                .map(FileChange::getFile)
                .any()

        if (!fileChanged) {
            // Nothing changed, nothing to do!
            return
        }

        execute(changes)
    }
}
