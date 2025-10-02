package org.parchmentmc.data

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("metadata")
data class MavenMetadata(
    val versioning: MavenMetadataVersioning,
)
