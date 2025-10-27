package org.parchmentmc.util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

import kotlin.io.path.exists

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

val xml = XML {
    recommended()
    defaultPolicy {
        ignoreUnknownChildren()
    }
}

fun FileSystem.walk(): Stream<Path> {
    return StreamSupport.stream(rootDirectories.spliterator(), false)
        .flatMap { Files.walk(it) }
}

val FileSystemLocation.path: Path
    get() = asFile.toPath()
val Provider<out FileSystemLocation>.path: Path
    get() = get().path
val Provider<out FileSystemLocation>.pathOrNull: Path?
    get() = orNull?.path

fun <T : FileSystemLocation> Provider<out T>.fileExists(): Provider<out T> {
    return map { it.takeIf { f -> f.path.exists() } }
}

private fun Path.jarUri(): URI {
    return URI.create("jar:${toUri()}")
}

fun Path.openZip(): FileSystem {
    return try {
        FileSystems.getFileSystem(jarUri())
    } catch (_: FileSystemNotFoundException) {
        FileSystems.newFileSystem(jarUri(), emptyMap<String, Any>())
    }
}

inline fun <reified T> HttpClient.getXml(url: String): T {
    return xml.decodeFromString(getText(url))
}

fun HttpClient.getText(url: String): String {
    val request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(url))
        .header("Cache-Control", "no-cache, max-age=0")
        .build()

    val response = send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
        if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw NotFoundException()
        }
        throw Exception("Failed to download file: $url")
    }

    return response.body()
}

class NotFoundException : Exception()
