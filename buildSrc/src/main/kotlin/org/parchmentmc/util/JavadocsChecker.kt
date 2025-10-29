package org.parchmentmc.util

object JavadocsChecker {

    @JvmStatic
    val PARAM_DOC_LINE = Regex("^@param\\s+[^<].*$")
    @JvmStatic
    val RETURN_DOC_LINE = Regex("^\\{@return\\s+(?<ireturn>[^<].*)}|@return\\s+(?<return>[^<].*)$")

    private fun String.replaceBetween(prefix: String, suffix: String, value: String): String {
        return replace(Regex("(?<=$prefix).*?(?=$suffix)"), value)
    }

    fun enforceParam(javadoc: List<String>, error: (String) -> Unit) {
        if (javadoc.isEmpty()) {
            return
        }

        if (javadoc[0][0].isUpperCase()) {
            val word = javadoc[0].substringBefore(' ') // first word

            // ignore single-letter "words" (like X or Z) and abbreviation like UUID, AABB
            //if (word.any { it.isLowerCase() }) { // todo maybe but should be 100% accurate
            error("parameter javadoc starts with uppercase word '$word'")
            //}
        }

        if (javadoc.size == 1) {
            val rawComment = javadoc[0]
                .replaceBetween("\\{", "}", "") // skip javadoc tags
                .replaceBetween("<", ">", "") // skip html tags
            if (rawComment.endsWith('.') && !rawComment.contains(". ")) {
                error("parameter javadoc ends with '.'")
            }
        }
    }

    fun enforceMethod(javadoc: List<String>, error: (String) -> Unit) {
        javadoc.forEach { line ->
            if (PARAM_DOC_LINE.matches(line)) {
                error("method javadoc contains parameter docs, which should be on the parameter itself")
            }

            val ret = RETURN_DOC_LINE.find(line)
            if (ret != null) {
                val content = (ret.groups["return"] ?: ret.groups["ireturn"])?.value
                if (content == null) {
                    return@forEach
                }
                if (content[0].isUpperCase()) {
                    val word = content.substringBefore(' ') // first word
                    error("method return javadoc starts with uppercase word '$word'")
                }
                val rawComment = content
                    .replaceBetween("\\{", "}", "") // skip javadoc tags
                    .replaceBetween("<", ">", "") // skip html tags
                if (rawComment.endsWith('.') && (ret.groups["return"] == null || !rawComment.contains(". "))) {
                    error("method return javadoc ends with '.'")
                }
            }
        }
    }
}
