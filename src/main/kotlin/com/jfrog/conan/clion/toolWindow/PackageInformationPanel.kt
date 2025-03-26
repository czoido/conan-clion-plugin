package com.jfrog.conan.clion.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.jfrog.conan.clion.bundles.UIBundle
import com.jfrog.conan.clion.services.ConanService
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import java.awt.Color


class PackageInformationPanel(private val project: Project) : JBPanelWithEmptyText() {
    private val readmePanel: ReadmePanel
    private val versionModel = DefaultComboBoxModel<String>()
    private val conanService: ConanService = project.service<ConanService>()

    init {
        layout = GridBagLayout()
        alignmentX = Component.LEFT_ALIGNMENT
        readmePanel = ReadmePanel(project)
    }

    private fun getTitle(name: String): JBLabel {
        return JBLabel(readmePanel.getTitleHtml(name)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    fun updatePanel(name: String, versions: List<String>) {
        // Actualiza el modelo de versiones
        versionModel.apply {
            removeAllElements()
            addAll(versions)
            if (versions.isNotEmpty()) {
                selectedItem = versions[0]
            }
        }
        removeAll()

        val c = GridBagConstraints()
        c.anchor = GridBagConstraints.NORTHWEST

        // Título
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridx = 0
        c.gridy = 0
        add(getTitle(name), c)

        // Panel con el ComboBox de versiones y botones de instalar/eliminar
        val buttonsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            val comboBox = ComboBox(versionModel)
            add(comboBox)
            val addButton = JButton(UIBundle.message("library.description.button.install"))
            val removeButton = JButton(UIBundle.message("library.description.button.remove"))

            // Lógica condicional original: mostrar uno u otro
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
                // Notificar al usuario...
            }

            removeButton.addActionListener {
                val selectedVersion = comboBox.selectedItem as String
                conanService.runRemoveRequirementFlow(name, selectedVersion)
                val required = conanService.getRequirements().any { it.startsWith("$name/") }
                addButton.isVisible = !required
                removeButton.isVisible = required
                comboBox.isEnabled = !required
                comboBox.toolTipText = null
                // Notificar al usuario...
            }
        }
        c.gridx = 0
        c.gridy = 0 + 1
        add(buttonsPanel, c)

        // Panel de selección de modo: "How to use" y "Scan vulnerabilities"
        val modePanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            // Botón para "How to use" (por defecto)
            val howToUseButton = JToggleButton("How to use", true)
            // Botón para "Scan vulnerabilities" con emoji de escudo
            val scanVulnButton = JToggleButton("🛡️ Scan vulnerabilities")
            val buttonGroup = ButtonGroup()
            buttonGroup.add(howToUseButton)
            buttonGroup.add(scanVulnButton)
            add(howToUseButton)
            add(scanVulnButton)

            // Variables para indicar el modo actual; se usa "how_to_use" por defecto
            var currentMode = "how_to_use"

            // Función que actualiza el contenido según el modo seleccionado
            val updateContent: () -> Unit = {
                val selectedVersion = (buttonsPanel.components.find { it is ComboBox<*> } as? ComboBox<*>)?.selectedItem as? String ?: ""
                readmePanel.updateContent(name, selectedVersion, currentMode)
            }

            // Indicadores visuales simples: cambia el color de la fuente cuando se selecciona
            howToUseButton.addActionListener {
                currentMode = "how_to_use"
                howToUseButton.foreground = Color.BLUE
                scanVulnButton.foreground = Color.BLACK
                updateContent()
            }
            scanVulnButton.addActionListener {
                currentMode = "scan_vulnerabilities"
                scanVulnButton.foreground = Color.BLUE
                howToUseButton.foreground = Color.BLACK
                updateContent()
            }
        }
        c.gridx = 0
        c.gridy = 2
        add(modePanel, c)

        // Panel de contenido que mostrará la información (ReadmePanel)
        val contentPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            val defaultVersion = versionModel.getElementAt(0) ?: ""
            // Se carga por defecto el contenido "How to use"
            readmePanel.updateContent(name, defaultVersion, "how_to_use")
            add(readmePanel.getHTMLPackageInfo(name))
        }

        val scrollPane = JBScrollPane(contentPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        c.fill = GridBagConstraints.BOTH
        c.weighty = 1.0
        c.weightx = 1.0
        c.gridx = 0
        c.gridy = 3
        add(scrollPane, c)

        revalidate()
        repaint()
    }
}
