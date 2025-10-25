package org.parchmentmc

import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.named
import org.parchmentmc.tasks.RemapUnpickDefinitions

abstract class UnpickExtension {
    abstract val remapIntermediaryDefinitions: Property<Boolean>

    init {
        init()
    }

    fun init() {
        remapIntermediaryDefinitions.convention(true)
    }

    fun includeDefinitions(task: AbstractArchiveTask) {
        if (remapIntermediaryDefinitions.get()) {
            task.from(task.project.tasks.named<RemapUnpickDefinitions>("remapUnpickDefinitions")) {
                into("extras")
            }
        } else {
            task.from(task.project.configurations.named("unpickDefinitions")) {
                into("extras")
                rename(".*", "definitions.unpick")
            }
        }
    }
}
