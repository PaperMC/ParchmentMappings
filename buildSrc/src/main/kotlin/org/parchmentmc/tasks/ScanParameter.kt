package org.parchmentmc.tasks

import cuchaz.enigma.Enigma
import cuchaz.enigma.ProgressListener
import cuchaz.enigma.translation.mapping.EntryMapping
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat
import cuchaz.enigma.translation.mapping.serde.MappingFormat
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters
import cuchaz.enigma.translation.mapping.tree.HashEntryTree
import cuchaz.enigma.translation.representation.TypeDescriptor
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.submit
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.parchmentmc.util.AsmUtil
import org.parchmentmc.util.path

abstract class ScanParameter : IncrementalDataTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputJar: RegularFileProperty

    @get:Input
    @get:Option(option = "desc", description = "Configures the descriptor of the parameter.")
    abstract val descriptor: Property<String>

    @get:Input
    @get:Option(option = "expectedName", description = "Configures the expected name of the parameter.")
    abstract val expectedName: Property<String>

    override fun execute(changes: InputChanges) {
        val workQueue = workerExecutor.noIsolation()

        workQueue.submit(ScanAction::class) {
            inputJar.set(this@ScanParameter.inputJar)
            descriptor.set(this@ScanParameter.descriptor)
            expectedName.set(this@ScanParameter.expectedName)
            inputMapping.set(this@ScanParameter.inputMapping)
        }
    }

    interface ScanParams : WorkParameters {
        val inputJar: RegularFileProperty
        val descriptor: Property<String>
        val expectedName: Property<String>
        val inputMapping: DirectoryProperty
    }

    abstract class ScanAction : WorkAction<ScanParams>, AsmUtil {

        private val desc: TypeDescriptor by lazy {
            val desc = parameters.descriptor.get()
            TypeDescriptor(TypeDescriptor.parseFirst(desc) ?: error("Invalid type descriptor: $desc"))
        }

        private fun paramMatches(param: LocalVariableEntry, expectedName: String, currentName: String, isStatic: ()->Boolean): Boolean {
            if (currentName == expectedName) {
                return true
            }

            if (currentName.startsWith("lambda$")) {
                return true // skip lambda for now since they require some additional work
            }

            val method = param.parent ?: error("Standalone variable found")
            val paramIndex = fromLvtToParamIndex(
                param.index,
                isStatic(),
                method.desc.argumentDescs
            )
            if (paramIndex != -1 && method.desc.argumentDescs[paramIndex] == desc) {
                println("""Expected "$expectedName" but got "$currentName" in ${method.name} ${method.descriptor} at ${method.parent?.fullName}""")
                return false
            }
            return true
        }

        override fun execute() {
            val indexer = Enigma.create().openJars(
                listOf(parameters.inputJar.path),
                listOf(),
                ProgressListener.none(),
                false
            ).jarIndex

            val mappings = HashEntryTree<EntryMapping>()
            val tree = MappingFormat.ENIGMA_DIRECTORY.read(
                parameters.inputMapping.path,
                ProgressListener.none(),
                MappingSaveParameters(MappingFileNameFormat.BY_DEOBF),
                indexer
            )

            tree.forEach { node ->
                mappings.insert(node.entry, node.value)
            }

            var failureCount = 0
            val expectedName = parameters.expectedName.get()
            mappings.allEntries.parallel().forEachOrdered { entry ->
                if (entry is LocalVariableEntry && entry.isArgument) {
                    val mapping = mappings[entry] ?: error("Mapping entry not found")
                    val currentName = mapping.targetName ?: return@forEachOrdered

                    if (!paramMatches(entry, expectedName, currentName) {
                            indexer.entryIndex.getMethodAccess(entry.parent)?.isStatic ?: error("Could not determine access flag of ${entry.parent}")
                        }) {
                        failureCount++
                    }
                }
            }

            if (failureCount > 0) {
                error("Found $failureCount parameter errors! See the log for details.")
            }
        }
    }
}
