package com.jfrog.conan.clionplugin.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jfrog.conan.clionplugin.bundles.UIBundle
import com.jfrog.conan.clionplugin.cmake.CMake
import com.jfrog.conan.clionplugin.models.PersistentStorageKeys
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

object ConanExecutableChooserDescriptor : FileChooserDescriptor(true, true, false, false, false, false) {
    init {
        withFileFilter { it.isConanExecutable }
        withTitle("Select Conan executable")
    }

    override fun isFileSelectable(file: VirtualFile?): Boolean {
        return super.isFileSelectable(file) && file != null && !file.isDirectory
    }
}

val VirtualFile.isConanExecutable: Boolean
    get() {
        return (extension == null || extension == "exe") && name == "conan"
    }

class ConanExecutableDialogWrapper(val project: Project) : DialogWrapper(true) {
    private val properties = project.service<PropertiesComponent>()
    private val cmake = CMake(project)
    private val profileCheckboxes: MutableList<JBCheckBox> = mutableListOf()

    private val fileChooserField1 = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            UIBundle.message("config.file.selector.title"),
            UIBundle.message("config.file.selector.description"),
            project,
            ConanExecutableChooserDescriptor
        )
    }

    private val automaticallyAddCheckbox =
        JBCheckBox(UIBundle.message("config.automanage.cmake.integrations")).apply {
            isSelected = properties.getValue(PersistentStorageKeys.AUTOMATIC_ADD_CONAN, "false") == "true"
        }

    private val useConanFromSystemCheckBox = JBCheckBox(UIBundle.message("config.use.system.conan")).apply {
        val conanExe = properties.getValue(PersistentStorageKeys.CONAN_EXECUTABLE, "")
        isSelected = conanExe == "conan"
        fileChooserField1.text = if (isSelected) "" else properties.getValue(PersistentStorageKeys.CONAN_EXECUTABLE, "")
        fileChooserField1.isEnabled = !isSelected
        addActionListener {
            fileChooserField1.isEnabled = !isSelected
            properties.setValue(PersistentStorageKeys.CONAN_EXECUTABLE, if (!isSelected) "" else "conan")
        }
    }

    init {
        init()
        title = UIBundle.message("config.title")
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(GridBagLayout()).apply {
            val gbcLabel = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                weightx = 0.0
                weighty = 0.0
                insets = JBUI.insetsTop(10)
            }
            val gbcField = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                weighty = 0.0
                gridwidth = GridBagConstraints.REMAINDER
            }
            val gbcCheckboxPanel = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.BOTH
                weightx = 1.0
                weighty = 1.0
                gridwidth = GridBagConstraints.REMAINDER
            }
            val newCheckConstraint = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                weighty = 0.0
                gridwidth = GridBagConstraints.REMAINDER
                insets = JBUI.insetsTop(10)
            }

            add(JBLabel(UIBundle.message("config.executable")), gbcLabel)
            add(fileChooserField1, gbcField)
            add(useConanFromSystemCheckBox, newCheckConstraint)

            val checkboxPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }

            val configurationsLabel = JBLabel(UIBundle.message("config.configurations.use")).apply {
                border = JBUI.Borders.emptyTop(10)
            }

            checkboxPanel.add(configurationsLabel)

            cmake.getActiveProfiles().forEach { profile ->
                val selected = cmake.checkConanUsedInProfile(profile.name)
                val checkbox = JBCheckBox(profile.name, selected)
                profileCheckboxes.add(checkbox)
                checkboxPanel.add(checkbox)
            }

            add(checkboxPanel, gbcCheckboxPanel)
            add(automaticallyAddCheckbox, newCheckConstraint)


            val automanageCMakeAdvancedSettings = project.service<PropertiesComponent>()
                .getBoolean(PersistentStorageKeys.AUTOMANAGE_CMAKE_ADVANCED_SETTINGS)
            val checkboxAdvancedSetting = JBCheckBox(
                UIBundle.message("config.automanage.cmake.parallel"),
                automanageCMakeAdvancedSettings
            ).apply {
                addActionListener {
                    project.service<PropertiesComponent>()
                        .setValue(PersistentStorageKeys.AUTOMANAGE_CMAKE_ADVANCED_SETTINGS, isSelected)
                }
            }

            add(checkboxAdvancedSetting, newCheckConstraint)
        }
    }

    override fun doOKAction() {
        if (!useConanFromSystemCheckBox.isSelected) {
            properties.setValue(PersistentStorageKeys.CONAN_EXECUTABLE, fileChooserField1.text)
        }
        else {
            properties.setValue(PersistentStorageKeys.CONAN_EXECUTABLE, "conan")
        }
        val selected = if (automaticallyAddCheckbox.isSelected) "true" else "false"
        properties.setValue(PersistentStorageKeys.AUTOMATIC_ADD_CONAN, selected)

        profileCheckboxes.forEach { checkbox ->
            val profileName = checkbox.text
            if (checkbox.isSelected) {
                cmake.injectDependencyProviderToProfile(profileName)
            } else {
                cmake.removeDependencyProviderFromProfile(profileName)
            }
        }

        super.doOKAction()
    }
}
