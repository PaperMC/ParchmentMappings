package org.parchmentmc.enigma.unpick

import cuchaz.enigma.api.EnigmaPlugin
import cuchaz.enigma.api.EnigmaPluginContext
import cuchaz.enigma.api.service.GuiService
import cuchaz.enigma.api.service.I18nService

class UnpickEnigmaPlugin : EnigmaPlugin {

    override fun init(ctx: EnigmaPluginContext) {
        ctx.registerService("unpick:i18n", I18nService.TYPE, { UnpickI18nService() })
        ctx.registerService("unpick:gui", GuiService.TYPE, { UnpickGuiService() })
    }
}
