package com.gharunsyntaxsupport

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

class GithubScriptResolver : LanguageResolver {
    override fun resolveLanguage(
        element: YAMLScalar,
        virtualFile: VirtualFile
    ): Language? {
        val scriptKeyValue = element.parent as? YAMLKeyValue ?: return null
        if (scriptKeyValue.keyText != "script") return null

        val scriptParent = scriptKeyValue.parent as? YAMLMapping ?: return null
        val withMapping = scriptParent.parent as? YAMLKeyValue ?: return null
        if (withMapping.keyText != "with") return null

        val parentMapping = withMapping.parent as? YAMLMapping ?: return null
        val usesValue = parentMapping.keyValues.find { it.keyText == "uses" }?.valueText ?: return null

        return if (usesValue.contains("github-script")) Language.findLanguageByID("JavaScript") else null
    }

}
