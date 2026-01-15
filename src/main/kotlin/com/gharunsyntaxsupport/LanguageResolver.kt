package com.gharunsyntaxsupport

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.psi.YAMLScalar

interface LanguageResolver {
    fun resolveLanguage(element: YAMLScalar, virtualFile: VirtualFile): Language?
}