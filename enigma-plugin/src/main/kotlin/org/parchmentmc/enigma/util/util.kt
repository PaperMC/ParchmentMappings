package org.parchmentmc.enigma.util

import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Path

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

operator fun Int.contains(value: Int): Boolean {
    return value and this != 0
}
