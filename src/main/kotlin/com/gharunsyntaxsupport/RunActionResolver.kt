package com.gharunsyntaxsupport

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import kotlin.text.contains

class RunActionResolver : LanguageResolver {
    override fun resolveLanguage(
        element: YAMLScalar,
        virtualFile: VirtualFile
    ): Language? {
        val runKeyValue = element.parent as? YAMLKeyValue ?: return null
        if (runKeyValue.keyText != "run") return null

        val parentMapping = runKeyValue.parent as? YAMLMapping ?: return null
        val shellValue = parentMapping.keyValues.find { it.keyText == "shell" }?.valueText

        // Determine which language to inject based on shell
        val language = when {
            shellValue == null -> Language.findLanguageByID("Shell Script")
            shellValue.contains("pwsh", true) -> Language.findLanguageByID("PowerShell")
            shellValue.contains("python", true) -> Language.findLanguageByID("Python")
            shellValue.contains("cmd", true) -> Language.findLanguageByID("Batch")
            shellValue.contains("bash", true) -> Language.findLanguageByID("Shell Script")
            shellValue.contains("sh", true) -> Language.findLanguageByID("Shell Script")
            else -> Language.findLanguageByID("Shell Script")
        } ?: return null

        return language
    }

}