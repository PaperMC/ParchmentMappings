package io.papermc.parchment.gradle

import io.papermc.parchment.gradle.task.ApplyFilePatch
import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.named

abstract class UnpickExtension {
    abstract val disablePatch: Property<Boolean>
    abstract val remapIntermediaryDefinitions: Property<Boolean>
    abstract val intermediaryMcVersion: Property<String>

    init {
        init()
    }

    fun init() {
        disablePatch.convention(false)
        remapIntermediaryDefinitions.convention(true)
    }

    fun includeDefinitions(task: AbstractArchiveTask) {
        task.from(task.project.tasks.named<ApplyFilePatch>("applyUnpickDefinitionsPatch").flatMap { it.outputFile }) {
            into("extras")
            rename(".*", "definitions.unpick")
        }
    }
}
