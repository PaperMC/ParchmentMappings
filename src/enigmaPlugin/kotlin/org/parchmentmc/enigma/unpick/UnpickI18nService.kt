package org.parchmentmc.enigma.unpick

import cuchaz.enigma.api.service.I18nService
import java.io.InputStream

class UnpickI18nService : I18nService {

    override fun getTranslationResource(language: String?): InputStream? {
        return javaClass.classLoader.getResourceAsStream("unpick_lang/$language.json")
    }
}
