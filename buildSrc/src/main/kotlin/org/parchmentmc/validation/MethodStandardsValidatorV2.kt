package org.parchmentmc.validation

import org.parchmentmc.compass.data.validation.impl.MethodStandardsValidator
import org.parchmentmc.compass.data.visitation.DataVisitor
import org.parchmentmc.feather.mapping.MappingDataContainer
import org.parchmentmc.feather.metadata.ClassMetadata
import org.parchmentmc.feather.metadata.MethodMetadata
import org.parchmentmc.util.JavadocsChecker

class MethodStandardsValidatorV2 : MethodStandardsValidator() {

    data class ParamInfo(val lvtIndex: Byte, val name: String)

    private val standardMethods: Map<String, Array<ParamInfo>> = mapOf(
        "equals (Ljava/lang/Object;)Z" to arrayOf(
            ParamInfo(1, "other")
        ),
        "compareTo (<owner>)I" to arrayOf( // doesn't work well for FriendlyByteBuf and LinkFSPath
            ParamInfo(1, "other")
        )
    )

    override fun preVisit(type: DataVisitor.DataType): Boolean {
        return DataVisitor.DataType.METHODS.test(type) || DataVisitor.DataType.PARAMETERS.test(type)
    }

    override fun visitMethod(
        classData: MappingDataContainer.ClassData, methodData: MappingDataContainer.MethodData,
        classMetadata: ClassMetadata?, methodMetadata: MethodMetadata?
    ): Boolean {
        val standardParams = standardMethods[methodData.name + " " + methodData.descriptor.replace("L${classData.name};", "<owner>")]
        if (standardParams != null) {
            methodData.parameters.zip(standardParams) { paramData, standardParam ->
                if (paramData.index == standardParam.lvtIndex && paramData.name != standardParam.name) {
                    error("Parameter #${paramData.index} doesn't match the expected name: ${standardParam.name}.")
                    return false
                }
            }
        }

        JavadocsChecker.enforceMethod(methodData.javadoc) { message ->
            error(message)
        }
        return true // keep checking parameter for possible mistakes too
    }

    override fun visitParameter(
        classData: MappingDataContainer.ClassData, methodData: MappingDataContainer.MethodData, paramData: MappingDataContainer.ParameterData,
        classMetadata: ClassMetadata?, methodMetadata: MethodMetadata?
    ) {
        paramData.javadoc?.let {
            JavadocsChecker.enforceParam(listOf(it)) { message ->
                error(message)
            }
        }
    }
}
