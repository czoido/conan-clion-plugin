package com.jfrog.conan.clion.toolWindow

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JCEFHtmlPanel
import java.awt.Color
import javax.swing.JComponent

class ReadmePanel(val project: Project) {
    private val htmlPanel = JCEFHtmlPanel(null).apply {
        loadHTML("")
        setOpenLinksInExternalBrowser(true)
    }

    // Actualiza el contenido del panel según el modo
    fun updateContent(libraryName: String, version: String, mode: String) {
        val html = if (mode == "how_to_use") getHtml(libraryName) else getAuditHtml(libraryName, version)
        htmlPanel.loadHTML(html)
    }

    // Retorna el componente que contiene el HTML
    fun getHTMLPackageInfo(libraryName: String): JComponent {
        return htmlPanel.component
    }

    // Genera el HTML para "How to use"
    fun getHtml(libraryName: String): String {
        val themeStyles = generateThemeStyles()
        val script = getScript(libraryName)
        return """
        <html>
        <head>
            <style>
                $themeStyles
            </style>
            <script>
                $script
            </script>
        </head>
        <body onload="fillExtraData()">
            <div id="info"></div>
        </body>
        </html>
        """.trimIndent()
    }

    // Genera el HTML para "Scan vulnerabilities"
    fun getAuditHtml(libraryName: String, version: String): String {
        val themeStyles = generateThemeStyles()
        return """
        <html>
        <head>
            <style>
                $themeStyles
            </style>
        </head>
        <body>
            <div id="info">
                <h2>🔍 Ready to secure your dependencies in seconds?</h2>
                <p>Register for free at <a href="https://audit.conan.io/register" target="_blank">audit.conan.io/register</a>.</p>
                <p>Save your token and activate it via the confirmation email you receive.</p>
                <p>Configure Conan to use your token:</p>
                <pre class="code">conan audit provider auth conancenter --token=&lt;token&gt;</pre>
                <p>Scan for vulnerabilities:</p>
                <pre class="code"># Check a specific reference
conan audit list ${'$'}{libraryName}/${'$'}{version}

# Scan the entire dependency graph
conan audit scan --requires=${'$'}{libraryName}/${'$'}{version}</pre>
                <p>Note: For more details on the Conan Audit command, please read <a href="https://example.com" target="_blank">this post</a>.</p>
                <p>Tip: To avoid exposing your token in shell history, authenticate using an environment variable (e.g., CONAN_AUDIT_PROVIDER_TOKEN_CONANCENTER=&lt;token&gt;). For more info, see the documentation.</p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    // Ejemplo de script para el modo "How to use"
    private fun getScript(libraryName: String): String {
        return """
            function fillExtraData() {
                var infoDiv = document.getElementById("info");
                infoDiv.innerHTML = "<h2>Using $libraryName with CMake</h2>" +
                                    "<p>To use " + "$libraryName" + " in your project, add the corresponding CMake commands.</p>";
            }
        """.trimIndent()
    }

    // Genera estilos CSS simples
    private fun generateThemeStyles(): String {
        val theme = LafManager.getInstance().currentUIThemeLookAndFeel
        val isDark = theme.isDark
        val background = if (isDark) "#3C3F41" else "#F2F2F2"
        val foreground = if (isDark) "#BBBBBB" else "#000000"
        return """
            body {
                font-family: sans-serif;
                background-color: $background;
                color: $foreground;
                padding: 10px;
            }
            .code {
                background-color: #F4F4F4;
                border: 1px solid #DDD;
                padding: 10px;
                border-radius: 5px;
                white-space: pre-wrap;
            }
        """.trimIndent()
    }
}
