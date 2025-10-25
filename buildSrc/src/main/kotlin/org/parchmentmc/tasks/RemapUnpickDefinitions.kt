package org.parchmentmc.tasks

import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Reader
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Remapper
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Writer
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader
import net.fabricmc.mappingio.tree.MemoryMappingTree
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.parchmentmc.util.openZip
import org.parchmentmc.util.path
import kotlin.io.path.bufferedReader
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

@CacheableTask
abstract class RemapUnpickDefinitions : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDefinitionsJar: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputDefinitionsFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val intermediaryJar: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mojangMappings: RegularFileProperty

    @TaskAction
    fun remap() {
        val fromOfficial = MemoryMappingTree()
        intermediaryJar.files.single().toPath().openZip().use { fs ->
            fs.getPath("mappings/mappings.tiny").bufferedReader().use { reader ->
                Tiny2FileReader.read(reader, fromOfficial)
            }
        }
        mojangMappings.path.bufferedReader().use {
            ProGuardFileReader.read(
                it,
                "mojang",
                "official",
                MappingSourceNsSwitch(fromOfficial, "official")
            )
        }
        val fromIntermediary = MemoryMappingTree()
        fromIntermediary.srcNamespace = "intermediary"
        fromOfficial.accept(MappingSourceNsSwitch(fromIntermediary, "intermediary"))

        inputDefinitionsJar.files.single().toPath().openZip().use { fs ->
            fs.getPath("extras/definitions.unpick").bufferedReader().use { reader ->
                val reader = UnpickV3Reader(reader)
                val writer = UnpickV3Writer()
                val remapper = makeRemapper(writer, fromIntermediary)
                reader.accept(remapper)
                outputDefinitionsFile.path.createParentDirectories()
                outputDefinitionsFile.path.writeText(writer.output)
            }
        }
    }

    fun makeRemapper(downstream: UnpickV3Visitor, tree: MemoryMappingTree): UnpickV3Remapper {
        return object : UnpickV3Remapper(downstream) {
            override fun mapClassName(className: String): String {
                return tree.getClass(className.replace(".", "/"))
                    ?.getName("mojang")?.replace("/", ".")
                    ?: className
            }

            override fun mapFieldName(
                className: String,
                fieldName: String,
                fieldDesc: String
            ): String {
                val classMapping = tree.getClass(className.replace(".", "/")) ?: return fieldName
                val fieldMapping = classMapping.getField(fieldName, fieldDesc) ?: return fieldName
                return fieldMapping.getName("mojang") ?: fieldName
            }

            override fun mapMethodName(
                className: String,
                methodName: String,
                methodDesc: String
            ): String {
                val classMapping = tree.getClass(className.replace(".", "/")) ?: return methodName
                val methodMapping = classMapping.getMethod(methodName, methodDesc) ?: return methodName
                return methodMapping.getName("mojang") ?: methodName
            }

            override fun getClassesInPackage(pkg: String): List<String> {
                return tree.classes
                    .filter {
                        val pkgSlash = pkg.replace(".", "/") + "/"
                        it.srcName.startsWith(pkgSlash) && !it.srcName.substringAfter(pkgSlash).contains('/')
                    }
                    .map { it.srcName.replace("/", ".") }
            }

            override fun getFieldDesc(className: String, fieldName: String): String {
                val classMapping = tree.getClass(className.replace(".", "/")) ?: return ""
                val fieldMapping = classMapping.getField(fieldName, null) ?: return ""
                return fieldMapping.srcDesc ?: ""
            }
        }
    }
}
