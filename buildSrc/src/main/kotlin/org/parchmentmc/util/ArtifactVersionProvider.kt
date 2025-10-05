package org.parchmentmc.util

import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.parchmentmc.data.MavenMetadata
import java.net.http.HttpClient

abstract class ArtifactVersionProvider : ValueSource<String, ArtifactVersionProvider.BuildIdParameters> {
    interface BuildIdParameters : ValueSourceParameters {
        val repoUrl: Property<String>
        val version: Property<String> // minecraft version
        val ci: Property<Boolean>
    }

    override fun obtain(): String {
        return parameters.version.get() + "+build." + buildVersion()
    }

    private fun buildVersion(): String {
        if (!parameters.ci.get()) {
            return "local-SNAPSHOT"
        }

        val client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build()

        val metaUrl = parameters.repoUrl.get().removeSuffix("/") + "/io/papermc/parchment/data/parchment/maven-metadata.xml"
        val meta = try {
            client.getXml<MavenMetadata>(metaUrl)
        } catch (_: NotFoundException) {
            return 1.toString()
        }

        val version = parameters.version.get()

        val buildIdentifier = "+build."
        val currentMaxVersion = meta.versioning.versions.asSequence()
            .filter { it.contains(buildIdentifier) }
            .filter { it.substringBefore(buildIdentifier) == version }
            .mapNotNull { it.substringAfter(buildIdentifier).toIntOrNull() }
            .maxOrNull() ?: 0

        return (currentMaxVersion + 1).toString()
    }
}
