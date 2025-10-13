package org.parchmentmc.validation

import org.parchmentmc.compass.data.validation.impl.MemberExistenceValidator
import org.parchmentmc.feather.mapping.MappingDataContainer
import org.parchmentmc.feather.metadata.ClassMetadata
import org.parchmentmc.feather.metadata.FieldMetadata

class MemberExistenceValidatorV2 : MemberExistenceValidator() {

    override fun visitField(
        classData: MappingDataContainer.ClassData, fieldData: MappingDataContainer.FieldData,
        classMetadata: ClassMetadata?, fieldMetadata: FieldMetadata?
    ) {
        super.visitField(classData, fieldData, classMetadata, fieldMetadata)
        if (fieldMetadata != null) {
            if (fieldMetadata.descriptor.mojangName.orElseThrow() != fieldData.descriptor) {
                error("Field descriptor does not match according to metadata");
            }
        } else {
            // Fields existence are already checked in MemberExistenceValidator
        }
    }
}
