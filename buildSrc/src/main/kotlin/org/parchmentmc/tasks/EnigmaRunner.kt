package org.parchmentmc.tasks

import org.gradle.api.JavaVersion
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

abstract class EnigmaArgumentProvider : CommandLineArgumentProvider {

    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:InputDirectory
    abstract val mappings: DirectoryProperty

    @get:InputFile
    abstract val profile: RegularFileProperty

    @get:CompileClasspath
    abstract val libraries: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> {
        val args = mutableListOf("--no-edit-all", "--edit-parameters", "--edit-javadocs", "--single-class-tree")

        args.add("--jar=${inputJar.get().asFile.absolutePath}")
        args.add("--mappings=${mappings.get().asFile.absolutePath}")
        args.add("--profile=${profile.get().asFile.absolutePath}")
        libraries.files.forEach { file ->
            args.add("--library=${file.absolutePath}")
        }

        return args.toList()
    }
}

abstract class EnigmaRunner @Inject constructor(
    objects: ObjectFactory,
    javaToolchains: JavaToolchainService
) : JavaExec() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputJar: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mappings: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val profile: RegularFileProperty

    @get:CompileClasspath
    abstract val libraries: ConfigurableFileCollection

    init {
        val provider = objects.newInstance<EnigmaArgumentProvider>()
        provider.inputJar.set(inputJar)
        provider.mappings.set(mappings)
        provider.profile.set(profile)
        provider.libraries.setFrom(libraries)
        argumentProviders.add(provider)

        // Enigma runs on Java 17. If the Gradle JVM supports Java 17, then we are fine
        // If not, then we set the java launcher via JVM toolchain so Gradle downloads a Java 17 JVM
        if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
            javaLauncher = javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(17)
            }
        }
    }
}
