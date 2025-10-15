package org.parchmentmc.validation

import org.parchmentmc.compass.data.validation.impl.MethodStandardsValidator
import org.parchmentmc.feather.mapping.MappingDataContainer
import org.parchmentmc.feather.metadata.ClassMetadata
import org.parchmentmc.feather.metadata.MethodMetadata

class MethodStandardsValidatorV2 : MethodStandardsValidator() {

    private val standardMethods: Map<String, Array<ParamInfo>> = mapOf(
        "equals (Ljava/lang/Object;)Z" to arrayOf(
            ParamInfo(1, "other")
        ),
        "compareTo (<owner>)I" to arrayOf(
            ParamInfo(1, "other")
        )
    )

    override fun visitMethod(
        classData: MappingDataContainer.ClassData, methodData: MappingDataContainer.MethodData,
        classMetadata: ClassMetadata?, methodMetadata: MethodMetadata?
    ): Boolean {
        val standardParams = standardMethods[methodData.name + " " + methodData.descriptor.replace("L${classData.name};", "<owner>")]
        if (standardParams != null) {
            methodData.parameters.forEachIndexed { index, paramData ->
                val standardParam = standardParams[index]
                if (paramData.index == standardParam.lvtIndex && paramData.name != standardParam.name) {
                    error("Parameter #${paramData.index} doesn't match the expected name: ${standardParam.name}")
                    return false
                }
            }

        }

        return super.visitMethod(classData, methodData, classMetadata, methodMetadata)
    }

    data class ParamInfo(val lvtIndex: Byte, val name: String)
}
