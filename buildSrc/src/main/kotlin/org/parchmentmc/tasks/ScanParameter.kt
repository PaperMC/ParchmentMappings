package org.parchmentmc.tasks

import org.cadixdev.bombe.type.FieldType
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.lorenz.model.ClassMapping
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.submit
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.parchmentmc.util.AsmUtil
import org.parchmentmc.util.lorenz.FabricEnigmaReader

abstract class ScanParameter : IncrementalDataTask() {

    @get:Input
    @get:Option(option = "paramType", description = "Configures the type of the parameter.")
    abstract val paramType: Property<String>

    @get:Input
    @get:Option(option = "expectedName", description = "Configures the expected name of the parameter.")
    abstract val expectedName: Property<String>

    override fun execute(changes: InputChanges) {
        val workQueue = workerExecutor.noIsolation()

        workQueue.submit(ScanAction::class) {
            paramType.set(this@ScanParameter.paramType)
            expectedName.set(this@ScanParameter.expectedName)
            inputMapping.set(this@ScanParameter.inputMapping)
        }
    }

    interface ScanParams : WorkParameters {
        val paramType: Property<String>
        val expectedName: Property<String>
        val inputMapping: DirectoryProperty
    }

    abstract class ScanAction : WorkAction<ScanParams>, AsmUtil {

        private val paramType: FieldType by lazy {
            FieldType.of(parameters.paramType.get())
        }

        private var failureCount = 0

        private fun scanClass(classMap: ClassMapping<*, *>) {
            val expectedName = parameters.expectedName.get()

            for (methodMap in classMap.methodMappings) {
                for (paramMap in methodMap.parameterMappings) {
                    if (paramMap.deobfuscatedName == expectedName) {
                        continue
                    }
                    if (methodMap.deobfuscatedName.startsWith("lambda$")) {
                        continue // skip lambda for now since they require some additional work
                    }

                    val paramIndex = fromLvtToParamIndex(
                        paramMap.index,
                        methodMap.hasParameterMapping(0),
                        methodMap.descriptor.paramTypes
                    )
                    if (paramIndex != -1 && methodMap.descriptor.paramTypes[paramIndex] == paramType) {
                        println("""Expected "$expectedName" but got "${paramMap.deobfuscatedName}" in ${methodMap.deobfuscatedName} ${methodMap.descriptor} at ${classMap.fullDeobfuscatedName}""")
                        failureCount++
                    }
                }
            }
            classMap.innerClassMappings.forEach(::scanClass)
        }

        override fun execute() {
            val set = MappingSet.create()
            parameters.inputMapping.asFileTree.filter { file -> file.name.endsWith(".mapping") }
                .forEach { file ->
                    FabricEnigmaReader(file.bufferedReader(Charsets.UTF_8)).use { reader ->
                        reader.read(set)
                    }
                }

            set.topLevelClassMappings.forEach(::scanClass)

            if (failureCount > 0) {
                throw GradleException("Found $failureCount parameter errors! See the log for details.")
            }
        }
    }
}
