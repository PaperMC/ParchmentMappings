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
            .setEnabledWhen { getCursorTargetReference(gui) != null }
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK))
            .setAction {
                val textToCopy = getCursorTargetReference(gui)
                if (textToCopy != null) {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(textToCopy), null)
                }
            }
        registrar.add("unpick.copyConstantReference")
            .setEnabledWhen { canBeConstant(gui.project, gui.cursorReference) }
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK))
            .setAction {
                val project = gui.project ?: return@setAction
                if (!canBeConstant(project, gui.cursorReference)) {
                    return@setAction
                }

                val obfField = gui.cursorReference!!.entry as FieldEntryView
                val deobfField = project.deobfuscate(obfField)
                val textToCopy = deobfField.parent.fullName.replace('/', '.') + "." + deobfField.name
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(textToCopy), null)
            }
    }

    private fun getCursorTargetReference(gui: GuiView): String? {
        val hoveredReference = gui.cursorReference ?: return null
        val project = gui.project ?: return null

        val obfEntry = hoveredReference.entry
        val deobfEntry = project.deobfuscate(hoveredReference.entry)

        return when (deobfEntry) {
            // todo uncomment for unpick v4
            /*is ClassEntryView if (project.jarIndex.entryIndex.getAccess(deobfEntry) and Opcodes.ACC_ANNOTATION) != 0 -> "target_annotation %s".format(
                deobfEntry.fullName.replace('/', '.')
            )*/

            is FieldEntryView -> "target_field %s %s %s".format(
                deobfEntry.parent.fullName.replace('/', '.'),
                deobfEntry.name,
                deobfEntry.descriptor
            )

            is MethodEntryView -> "target_method %s %s %s".format(
                deobfEntry.parent.fullName.replace('/', '.'),
                deobfEntry.name,
                deobfEntry.descriptor
            )

            is LocalVariableEntryView -> {
                val localIndex = getLocalIndex(project, obfEntry as LocalVariableEntryView)
                if (localIndex == -1) null else "param $localIndex"
            }

            else -> null
        }
    }

    private fun getLocalIndex(project: ProjectView, local: LocalVariableEntryView): Int {
        val argTypes = Type.getArgumentTypes(local.parent.descriptor)
        val methodAccess = project.jarIndex.entryIndex.getAccess(local.parent)
        var varIndex = if ((methodAccess and Opcodes.ACC_STATIC) != 0) 0 else 1

        for (localIndex in argTypes.indices) {
            if (varIndex == local.index) {
                return localIndex
            }

            varIndex += argTypes[localIndex].size
        }

        return -1
    }

    private fun canBeConstant(project: ProjectView?, reference: EntryReferenceView?): Boolean {
        if (reference == null || project == null) {
            return false
        }

        val field = reference.entry
        if (field !is FieldEntryView) {
            return false
        }

        val access = project.jarIndex.entryIndex.getAccess(field)
        if ((access and Opcodes.ACC_FINAL) == 0) {
            return false
        }

        return when (field.descriptor) {
            "B", "C", "D", "F", "I", "J", "S", "Ljava/lang/String;", "Ljava/lang/Class;" -> true
            else -> false
        }
    }
}
