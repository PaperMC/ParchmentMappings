package org.parchmentmc.tasks

import cuchaz.enigma.ProgressListener
import cuchaz.enigma.translation.mapping.EntryMapping
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat
import cuchaz.enigma.translation.mapping.serde.MappingFormat
import cuchaz.enigma.translation.mapping.serde.MappingParseException
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters
import cuchaz.enigma.translation.mapping.tree.EntryTree
import cuchaz.enigma.translation.mapping.tree.HashEntryTree
import cuchaz.enigma.translation.representation.entry.ClassEntry
import cuchaz.enigma.translation.representation.entry.Entry
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry
import cuchaz.enigma.translation.representation.entry.MethodEntry
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.kotlin.dsl.submit
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.parchmentmc.util.path
import java.io.IOException

abstract class JavadocLint : IncrementalDataTask() {

    override fun execute(changes: InputChanges) {
        val workQueue = workerExecutor.noIsolation()

        workQueue.submit(LintAction::class) {
            inputMapping.set(this@JavadocLint.inputMapping)
        }
    }

    interface LintParams : WorkParameters {
        val inputMapping: DirectoryProperty
    }

    abstract class LintAction : WorkAction<LintParams> {

        companion object {
            @JvmStatic
            val PARAM_DOC_LINE = Regex("^@param\\s+[^<].*$")
        }

        private fun isRegularMethodParameter(line: String): Boolean {
            return PARAM_DOC_LINE.matches(line)
        }

        private fun getFullName(mappings: EntryTree<EntryMapping>, entry: Entry<*>): String? {
            var name = if (entry is MethodEntry) {
                entry.name + ' ' +  entry.desc.toString()
            } else if (entry is ClassEntry) {
                entry.name
            } else {
                mappings.get(entry)?.targetName()
            }

            if (entry.parent != null) {
                name = getFullName(mappings, entry.parent!!) + '.' + name
            }

            return name
        }

        private fun String.replaceBetween(prefix: String, suffix: String, value: String): String {
            return replace("(?<=$prefix).*?(?=$suffix)", value)
        }

        override fun execute() {
            try {
                val mappings = HashEntryTree<EntryMapping>()
                val tree = MappingFormat.ENIGMA_DIRECTORY.read(
                    parameters.inputMapping.path,
                    ProgressListener.none(),
                    MappingSaveParameters(MappingFileNameFormat.BY_DEOBF),
                    null
                )

                tree.forEach { node ->
                    mappings.insert(node.entry, node.value)
                }

                val errors = mutableListOf<String>()

                mappings.allEntries.parallel().forEachOrdered { entry ->
                    val mapping = mappings[entry] ?: error("Mapping entry not found")
                    val javadoc = mapping.javadoc()
                    if (javadoc != null && javadoc.isNotEmpty()) {
                        val localErrors = mutableListOf<String>()

                        if (entry is LocalVariableEntry && entry.isArgument) {
                            if (javadoc
                                    .replaceBetween("{", "}", "") // skip javadoc tags
                                    .replaceBetween("<", ">", "") // skip html tags
                                    .count { it == '.' } == 1) {
                                localErrors.add("parameter javadoc ends with '.'")
                            }

                            if (javadoc[0].isUpperCase()) {
                                val word = javadoc.substringBefore(' ') // first word

                                // ignore single-letter "words" (like X or Z) and abbreviation like UUID, AABB
                                //if (word.any { it.isLowerCase() }) { // todo maybe but should be 100% accurate
                                localErrors.add("parameter javadoc starts with uppercase word '$word'")
                                //}
                            }
                        } else if (entry is MethodEntry) {
                            if (javadoc.lineSequence().any(::isRegularMethodParameter)) {
                                localErrors.add("method javadoc contains parameter docs, which should be on the parameter itself")
                            }
                        }

                        // new rules can be added here in the future
                        if (localErrors.isNotEmpty()) {
                            val name = getFullName(mappings, entry)

                            for (error in localErrors) {
                                errors.add("$name: $error")
                            }
                        }
                    }
                }

                if (errors.isNotEmpty()) {
                    for (error in errors) {
                        println("lint: $error")
                    }

                    error("Found ${errors.size} javadoc format errors! See the log for details.")
                }
            } catch (e: IOException) {
                throw GradleException("Could not read and parse mappings", e)
            } catch (e: MappingParseException) {
                throw GradleException("Could not read and parse mappings", e)
            }
        }
    }
}
