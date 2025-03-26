package com.jfrog.conan.clion.toolWindow

import com.intellij.openapi.components.service
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.jfrog.conan.clion.bundles.UIBundle
import com.jfrog.conan.clion.services.ConanService
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class PackageInformationPanel(private val project: com.intellij.openapi.project.Project) : JBPanelWithEmptyText() {

    private val versionModel = DefaultComboBoxModel<String>()
    private val conanService: ConanService = project.service<ConanService>()

    init {
        layout = GridBagLayout()
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private fun getTitle(name: String): JBLabel {
        return JBLabel(getTitleHtml(name)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    // Método auxiliar para construir el título (puedes personalizarlo)
    private fun getTitleHtml(name: String): String {
        return "<html><strong>$name</strong></html>"
    }

    fun updatePanel(name: String, versions: List<String>) {
        // Actualizamos el modelo de versiones
        versionModel.apply {
            removeAllElements()
            versions.forEach { addElement(it) }
            if (versions.isNotEmpty()) {
                selectedItem = versions[0]
            }
        }
        removeAll()

        val c = GridBagConstraints().apply {
            anchor = GridBagConstraints.NORTHWEST
        }

        // 1. Título
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 0
        c.gridy = 0
        add(getTitle(name), c)

        // 2. Panel con JComboBox y botones "install"/"remove"
        val buttonsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            val comboBox: JComboBox<String> = JComboBox(versionModel)
            add(comboBox)
            val addButton = JButton(UIBundle.message("library.description.button.install"))
            val removeButton = JButton(UIBundle.message("library.description.button.remove"))

            // Lógica condicional: si la librería ya está añadida, mostramos "remove"
            val isRequired = conanService.getRequirements().any { it.startsWith("$name/") }
            addButton.isVisible = !isRequired
            removeButton.isVisible = isRequired
            comboBox.isEnabled = !isRequired

            add(addButton)
            add(removeButton)

            addButton.addActionListener {
                val selectedVersion = comboBox.selectedItem as String
                conanService.runUseFlow(name, selectedVersion)
                val required = conanService.getRequirements().any { it.startsWith("$name/") }
                addButton.isVisible = !required
                removeButton.isVisible = required
                comboBox.isEnabled = !required
                comboBox.toolTipText = UIBundle.message("library.description.combo.disabled")
            }

            removeButton.addActionListener {
                val selectedVersion = comboBox.selectedItem as String
                conanService.runRemoveRequirementFlow(name, selectedVersion)
                val required = conanService.getRequirements().any { it.startsWith("$name/") }
                addButton.isVisible = !required
                removeButton.isVisible = required
                comboBox.isEnabled = !required
                comboBox.toolTipText = null
            }
        }
        c.gridx = 0
        c.gridy = 1
        add(buttonsPanel, c)

        // 3. Panel de pestañas (JTabbedPane) con dos instancias independientes de ReadmePanel
        val tabbedPane = JTabbedPane()
        val defaultVersion = versionModel.getElementAt(0) ?: ""

        // Pestaña "How to use"
        val howToUsePanel = JPanel(BorderLayout())
        val howToUseReadmePanel = ReadmePanel(project)
        howToUseReadmePanel.updateContent(name, defaultVersion, "how_to_use")
        howToUsePanel.add(howToUseReadmePanel.getHTMLPackageInfo(name), BorderLayout.CENTER)
        tabbedPane.addTab("How to use", null, howToUsePanel, "Instructions for using the package")

        // Pestaña "Scan vulnerabilities" (con emoji de escudo)
        val scanPanel = JPanel(BorderLayout())
        val scanReadmePanel = ReadmePanel(project)
        scanReadmePanel.updateContent(name, defaultVersion, "scan_vulnerabilities")
        scanPanel.add(scanReadmePanel.getHTMLPackageInfo(name), BorderLayout.CENTER)
        tabbedPane.addTab("🛡️ Scan vulnerabilities", null, scanPanel, "Scan the package for vulnerabilities")

        c.fill = GridBagConstraints.BOTH
        c.weighty = 1.0
        c.weightx = 1.0
        c.gridx = 0
        c.gridy = 2
        add(tabbedPane, c)

        revalidate()
        repaint()
    }
}
