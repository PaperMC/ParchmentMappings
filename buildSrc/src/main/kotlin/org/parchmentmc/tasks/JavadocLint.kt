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
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import kotlin.streams.asStream

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
            val PARAM_DOC_LINE: Pattern = Pattern.compile("^@param\\s+[^<].*$")

            private fun isRegularMethodParameter(line: String): Boolean {
                return PARAM_DOC_LINE.matcher(line).matches()
            }

            private fun getFirstWord(str: String): String {
                val i = str.indexOf(' ')
                return if (i != -1) str.take(i) else str
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
        }

        override fun execute() {
            try {
                val files = parameters.inputMapping.asFileTree.map(File::toPath)
                val mappings = HashEntryTree<EntryMapping>()

                for (file in files) {
                    val read = MappingFormat.ENIGMA_FILE.read(
                        file,
                        ProgressListener.none(),
                        MappingSaveParameters(MappingFileNameFormat.BY_DEOBF),
                        null
                    )
                    read.forEach { entry ->
                        mappings.insert(
                            entry.entry,
                            entry.getValue()
                        )
                    }
                }

                val errors = mutableListOf<String>()

                mappings.allEntries.parallel().forEach { entry ->
                    val mapping = mappings.get(entry)!!
                    val javadoc = mapping.javadoc()
                    if (javadoc != null && javadoc.isNotEmpty()) {
                        val localErrors = mutableListOf<String>()

                        if (entry is LocalVariableEntry && entry.isArgument) {
                            if (javadoc.endsWith('.')) {
                                localErrors.add("parameter javadoc ends with '.'")
                            }

                            if (javadoc[0].isUpperCase()) {
                                val word = getFirstWord(javadoc)

                                // ignore single-letter "words" (like X or Z)
                                if (word.length > 1) {
                                    localErrors.add("parameter javadoc starts with uppercase word '$word'")
                                }
                            }
                        } else if (entry is MethodEntry) {
                            if (javadoc.lineSequence().asStream().anyMatch(LintAction::isRegularMethodParameter)) {
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

                    throw GradleException("Found ${errors.size} javadoc format errors! See the log for details.")
                }
            } catch (e: IOException) {
                throw GradleException("Could not read and parse mappings", e)
            } catch (e: MappingParseException) {
                throw GradleException("Could not read and parse mappings", e)
            }
        }
    }
}
