package org.parchmentmc.enigma

import cuchaz.enigma.api.EnigmaPlugin
import cuchaz.enigma.api.EnigmaPluginContext
import cuchaz.enigma.api.service.JarIndexerService
import cuchaz.enigma.api.service.NameProposalService
import cuchaz.enigma.api.service.ProjectService
import kotlin.io.path.Path

class NameProposalServiceEnigmaPlugin : EnigmaPlugin {

    override fun init(ctx: EnigmaPluginContext) {
        val service = EnigmaNameProposalService(System.getProperty("client.unobfuscated")?.let { Path(it) })
        ctx.registerService("paper:name_proposal", NameProposalService.TYPE, { service })
        ctx.registerService("paper:jar_indexer", JarIndexerService.TYPE, { service })
        ctx.registerService("paper:project", ProjectService.TYPE, { service })
    }
}
