package org.parchmentmc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import org.parchmentmc.feather.manifests.VersionManifest
import java.io.OutputStream
import javax.inject.Inject

abstract class RemapJarArgumentProvider : CommandLineArgumentProvider {

    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:InputFile
    abstract val mappings: RegularFileProperty

    @get:CompileClasspath
    abstract val minecraftClasspath: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    override fun asArguments(): Iterable<String> {
        val output = outputJar.get().asFile

        val args = mutableListOf("--ann-fix", "--record-fix", "--ids-fix", "--src-fix", "--strip-sigs", "--disable-abstract-param")
        args.add("--reverse") // Input mappings is expected to be OBF->MOJ (ProGuard layout)

        args.add("--input=${inputJar.get().asFile.absolutePath}")
        args.add("--map=${mappings.get().asFile.absolutePath}")
        args.add("--output=${output.absolutePath}")
        args.add("--log=${output.toPath().resolveSibling("${output.name}.log").toAbsolutePath()}")
        minecraftClasspath.files.forEach { file ->
            args.add("--lib=" + file.absolutePath)
        }

        return args.toList()
    }
}

@CacheableTask
abstract class RemapJar @Inject constructor(
    private val objects: ObjectFactory,
    private val exec: ExecOperations
) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mappings: RegularFileProperty

    @get:CompileClasspath
    abstract val remapperClasspath: ConfigurableFileCollection

    @get:CompileClasspath
    abstract val minecraftClasspath: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun run() {
        exec.javaexec {
            classpath(remapperClasspath)
            mainClass.set("net.neoforged.art.Main")

            val provider = objects.newInstance<RemapJarArgumentProvider>()
            provider.inputJar.set(inputJar)
            provider.mappings.set(mappings)
            provider.outputJar.set(outputJar)
            provider.minecraftClasspath.from(minecraftClasspath)

            argumentProviders.add(provider)

            standardOutput = OutputStream.nullOutputStream()
        }
    }

    companion object {
        @JvmStatic
        fun configureMinecraftClasspath(project: Project, manifest: Provider<VersionManifest>) {
            project.dependencies {
                for (library in manifest.get().libraries) {
                    "minecraft"(library.name) {
                        isTransitive = false
                    }
                }
            }
        }
    }
}
