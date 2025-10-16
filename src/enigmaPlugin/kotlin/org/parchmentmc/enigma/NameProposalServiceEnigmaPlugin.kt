package org.parchmentmc.enigma

import cuchaz.enigma.api.EnigmaPlugin
import cuchaz.enigma.api.EnigmaPluginContext
import cuchaz.enigma.api.service.JarIndexerService
import cuchaz.enigma.api.service.NameProposalService

class NameProposalServiceEnigmaPlugin : EnigmaPlugin {

    override fun init(ctx: EnigmaPluginContext) {
        val service = EnigmaNameProposalService()
        ctx.registerService("paper:name_proposal", NameProposalService.TYPE) { _ -> service }
        ctx.registerService("paper:jar_indexer", JarIndexerService.TYPE) { _ -> service }
    }
}
