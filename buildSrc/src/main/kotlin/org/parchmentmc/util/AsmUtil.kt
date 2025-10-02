package org.parchmentmc.util

import org.cadixdev.bombe.type.BaseType
import org.cadixdev.bombe.type.FieldType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

interface AsmUtil {
    companion object {
        const val RESET_ACCESS: Int = (Opcodes.ACC_PUBLIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED).inv()
    }

    operator fun Int.contains(value: Int): Boolean {
        return value and this != 0
    }

    /*
     * Transform the given LVT index into a parameter index.
     */
    fun fromLvtToParamIndex(lvtIndex: Int, method: MethodNode): Int {
        var currentParamIndex = 0
        var currentLvtIndex = if (Opcodes.ACC_STATIC in method.access) 0 else 1

        for (param in method.parameters) {
            if (currentLvtIndex == lvtIndex) {
                return currentParamIndex
            }

            currentParamIndex++
            currentLvtIndex++

            if (param.name == "java/lang/Long" || param.name == "java/lang/Double") {
                currentLvtIndex++
            }
        }
        return -1
    }

    /*
     * Transform the given param index into a lvt index.
     */
    fun fromParamToLvtIndex(paramIndex: Int, method: MethodNode): Int {
        var currentLvtIndex = if (Opcodes.ACC_STATIC in method.access) 0 else 1
        var currentParamIndex = 0

        for (param in method.parameters) {
            if (currentParamIndex == paramIndex) {
                return currentLvtIndex
            }

            currentParamIndex++
            currentLvtIndex++

            if (param.name == "java/lang/Long" || param.name == "java/lang/Double") {
                currentLvtIndex++
            }
        }

        return -1
    }

    /*
     * Transform the given LVT index into a parameter index.
     */
    fun fromLvtToParamIndex(lvtIndex: Int, isStatic: Boolean, parameters: List<FieldType>): Int {
        var currentParamIndex = 0
        var currentLvtIndex = if (isStatic) 0 else 1

        for (param in parameters) {
            if (currentLvtIndex == lvtIndex) {
                return currentParamIndex
            }

            currentParamIndex++
            currentLvtIndex++

            if (param == BaseType.LONG || param == BaseType.DOUBLE) {
                currentLvtIndex++
            }
        }
        return -1
    }

    /*
     * Transform the given param index into a lvt index.
     */
    fun fromParamToLvtIndex(paramIndex: Int, isStatic: Boolean, parameters: List<FieldType>): Int {
        var currentLvtIndex = if (isStatic) 0 else 1
        var currentParamIndex = 0

        for (param in parameters) {
            if (currentParamIndex == paramIndex) {
                return currentLvtIndex
            }

            currentParamIndex++
            currentLvtIndex++

            if (param == BaseType.LONG || param == BaseType.DOUBLE) {
                currentLvtIndex++
            }
        }

        return -1
    }
}
