package org.parchmentmc.util

import org.parchmentmc.compass.data.validation.Validator
import org.parchmentmc.compass.data.visitation.DataVisitor
import org.parchmentmc.feather.mapping.MappingDataContainer
import org.parchmentmc.feather.metadata.ClassMetadata
import org.parchmentmc.feather.metadata.FieldMetadata

class FieldDescriptorValidator : Validator {
    constructor() : super("field descriptor existence")

    override fun preVisit(type: DataVisitor.DataType): Boolean {
        return DataVisitor.DataType.FIELDS.test(type)
    }

    override fun visitField(
        classData: MappingDataContainer.ClassData, fieldData: MappingDataContainer.FieldData,
        classMetadata: ClassMetadata?, fieldMetadata: FieldMetadata?
    ) {
        if (fieldMetadata != null) {
            fieldMetadata.descriptor.mojangName.ifPresent {
                if (it != fieldData.descriptor) {
                    error("Field descriptor does not match according to metadata");
                }
            }
        } else {
            // Fields existence are already checked in MemberExistenceValidator
        }
    }
}
