package org.parchmentmc.validation

import org.parchmentmc.compass.data.sanitation.Sanitizer
import org.parchmentmc.compass.data.validation.Validator
import org.parchmentmc.compass.data.visitation.DataVisitor
import org.parchmentmc.compass.data.visitation.ModifyingDataVisitor
import org.parchmentmc.feather.mapping.MappingDataContainer
import org.parchmentmc.feather.metadata.ClassMetadata
import org.parchmentmc.feather.metadata.MethodMetadata
import org.parchmentmc.feather.metadata.SourceMetadata
import java.lang.constant.ConstantDescs

class DuplicateData {
    class DataValidator : Validator("duplicate data") {

        lateinit var container: MappingDataContainer

        override fun visit(container: MappingDataContainer, metadata: SourceMetadata?): Boolean {
            this.container = container
            return metadata != null // Only visit when we have metadata available
        }

        override fun preVisit(type: DataVisitor.DataType): Boolean {
            return DataVisitor.DataType.METHODS.test(type)
        }

        override fun visitMethod(
            classData: MappingDataContainer.ClassData, methodData: MappingDataContainer.MethodData,
            classMetadata: ClassMetadata?, methodMetadata: MethodMetadata?
        ): Boolean {
            if (classMetadata == null || methodMetadata == null) {
                return false
            }

            if (methodMetadata.isPrivate || methodMetadata.isLambda || methodMetadata.isStatic) {
                return false
            }

            if (methodMetadata.parent.isPresent) {
                error("Duplicate method already inherited from ${methodMetadata.parent.get().name}")
                return false
            }

            if (methodMetadata.name.mojangName.orElseThrow() == ConstantDescs.INIT_NAME && !classMetadata.superName.isEmpty) {
                val superClass = container.getClass(classMetadata.superName.mojangName.orElseThrow())
                if (superClass != null && superClass.getMethod(
                        methodMetadata.name.mojangName.orElseThrow(),
                        methodMetadata.descriptor.mojangName.orElseThrow()
                    ) != null
                ) {
                    error("Duplicate constructor already inherited from ${superClass.name}")
                    return false
                }
            }
            return false
        }
    }

    class DataSanitizer : Sanitizer("duplicate data") {

        lateinit var container: MappingDataContainer

        override fun visit(container: MappingDataContainer, metadata: SourceMetadata?): Boolean {
            this.container = container
            return metadata != null // Only visit when we have metadata available
        }

        override fun preVisit(type: DataVisitor.DataType): Boolean {
            return DataVisitor.DataType.METHODS.test(type)
        }

        override fun modifyMethod(
            classData: MappingDataContainer.ClassData, methodData: MappingDataContainer.MethodData,
            classMetadata: ClassMetadata?, methodMetadata: MethodMetadata?
        ): ModifyingDataVisitor.Action<MappingDataContainer.MethodData> {
            if (classMetadata == null || methodMetadata == null) {
                return ModifyingDataVisitor.Action.skip()
            }

            if (methodMetadata.isPrivate || methodMetadata.isLambda || methodMetadata.isStatic) {
                return ModifyingDataVisitor.Action.skip()
            }

            if (methodMetadata.parent.isPresent) {
                return ModifyingDataVisitor.Action.delete()
            }

            if (methodMetadata.name.mojangName.orElseThrow() == ConstantDescs.INIT_NAME && !classMetadata.superName.isEmpty) {
                val superClass = container.getClass(classMetadata.superName.mojangName.orElseThrow())
                if (superClass != null && superClass.getMethod(
                        methodMetadata.name.mojangName.orElseThrow(),
                        methodMetadata.descriptor.mojangName.orElseThrow()
                    ) != null
                ) {
                    return ModifyingDataVisitor.Action.delete()
                }
            }
            return ModifyingDataVisitor.Action.skip()
        }
    }
}
