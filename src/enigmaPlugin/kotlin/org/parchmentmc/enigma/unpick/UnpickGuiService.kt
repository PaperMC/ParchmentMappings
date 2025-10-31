package org.parchmentmc.enigma.unpick

import cuchaz.enigma.api.service.GuiService
import cuchaz.enigma.api.service.GuiService.MenuRegistrar
import cuchaz.enigma.api.view.GuiView
import cuchaz.enigma.api.view.ProjectView
import cuchaz.enigma.api.view.entry.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class UnpickGuiService : GuiService {

    override fun addToEditorContextMenu(gui: GuiView, registrar: MenuRegistrar) {
        registrar.addSeparator()

        registrar.add("unpick.copyTargetReference")
            .setEnabledWhen { getUnpickReferenceOnCursor(gui) != null }
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK)) // hopefully keybinds will be controllable at some point
            .setAction {
                val textToCopy = getUnpickReferenceOnCursor(gui) ?: return@setAction
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(textToCopy), null)
            }
        registrar.add("unpick.copyConstantReference")
            .setEnabledWhen { getEligibleConstantOnCursor(gui) != null }
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK))
            .setAction {
                val field = getEligibleConstantOnCursor(gui) ?: return@setAction
                val textToCopy = field.parent.fullName.replace('/', '.') + "." + field.name
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(textToCopy), null)
            }
    }

    private fun getUnpickReferenceOnCursor(gui: GuiView): String? {
        val hoveredReference = gui.cursorReference ?: return null
        val project = gui.project ?: return null

        val entry = hoveredReference.entry
        return when (entry) {
            is ClassEntryView if (project.jarIndex.entryIndex.getAccess(entry) and Opcodes.ACC_ANNOTATION) != 0 -> "target_annotation %s".format(
                entry.fullName.replace('/', '.')
            )

            is FieldEntryView -> "target_field %s %s %s".format(
                entry.parent.fullName.replace('/', '.'),
                entry.name,
                entry.descriptor
            )

            is MethodEntryView -> "target_method %s %s %s".format(
                entry.parent.fullName.replace('/', '.'),
                entry.name,
                entry.descriptor
            )

            is LocalVariableEntryView if (entry.isArgument) -> {
                val paramIndex = getParamIndex(project, entry) // should never be -1 except for https://github.com/FabricMC/Enigma/issues/572
                if (paramIndex == -1) null else "param $paramIndex"
            }

            else -> null
        }
    }

    private fun getParamIndex(project: ProjectView, local: LocalVariableEntryView): Int {
        val argTypes = Type.getArgumentTypes(local.parent.descriptor)
        var lvtIndex = if ((project.jarIndex.entryIndex.getAccess(local.parent) and Opcodes.ACC_STATIC) != 0) 0 else 1

        for (paramIndex in argTypes.indices) {
            if (lvtIndex == local.index) {
                return paramIndex
            }

            lvtIndex += argTypes[paramIndex].size
        }

        return -1
    }

    private fun getEligibleConstantOnCursor(gui: GuiView): FieldEntryView? {
        val hoveredReference = gui.cursorReference ?: return null
        val project = gui.project ?: return null

        val field = hoveredReference.entry
        if (field !is FieldEntryView) {
            return null
        }

        if ((project.jarIndex.entryIndex.getAccess(field) and Opcodes.ACC_FINAL) == 0) {
            return null
        }

        return when (field.descriptor) {
            "B", "C", "D", "F", "I", "J", "S", "Ljava/lang/String;", "Ljava/lang/Class;" -> field
            else -> null
        }
    }
}
