package com.gharunsyntaxsupport

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.jsonSchema.ide.JsonSchemaService
import org.jetbrains.annotations.NotNull
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.impl.YAMLScalarImpl

class GithubActionsRunHighlighterInjector : MultiHostInjector {
    val resolvers = listOf(
        RunActionResolver(),
        GithubScriptResolver()
    )

    override fun getLanguagesToInject(@NotNull registrar: MultiHostRegistrar, @NotNull context: PsiElement) {
        val virtualFile = context.containingFile.virtualFile ?: return
        if (!isGithubActionsFile(context, virtualFile)) return

        if (context is YAMLScalarImpl) {
            val originalRanges = context.contentRanges
            if (originalRanges.isEmpty()) {
                return
            }

            val ranges = mutableListOf<TextRange>()
            for (originalRange in originalRanges) {
                var range = originalRange
                // avoid highlighting blank lines
                while (range.substring(context.text).endsWith("\n\n")) {
                    range = TextRange(range.startOffset, range.endOffset - 1)
                }
                ranges.add(range)
            }

            val language = resolveLanguage(context, virtualFile) ?: return

            val ghexpRanges = resolveGithubExpressionRanges(registrar, context)

            // Skip GitHub Expressions
            val bashRanges = resolveExclusions(ranges, ghexpRanges)

            if (bashRanges.isNotEmpty()) {
                registrar.startInjecting(language)
                for (range in bashRanges) {
                    registrar.addPlace(null, null, context, range)
                }
                registrar.doneInjecting()
            }
        }
    }

    private fun resolveExclusions(
        ranges: Collection<TextRange>,
        exclusions: Collection<TextRange>
    ): MutableList<TextRange> {
        val resolved = mutableListOf<TextRange>()
        for (range in ranges) {
            var nextStartOffset = range.startOffset
            for (excluded in exclusions) {
                if (range.intersects(excluded)) {
                    resolved.add(TextRange(nextStartOffset, excluded.startOffset))
                    nextStartOffset = excluded.endOffset
                }
            }
            if (nextStartOffset < range.endOffset) {
                resolved.add(TextRange(nextStartOffset, range.endOffset))
            }
        }
        return resolved
    }

    private fun resolveGithubExpressionRanges(
        registrar: MultiHostRegistrar,
        context: YAMLScalar
    ): List<TextRange> {
        val language = Language.findLanguageByID("GithubExpressionLanguage") ?: return emptyList()
        val ranges = mutableListOf<TextRange>()
        for (match in Regex("\\$\\{\\{.*?}}").findAll(context.text)) {
            val range = TextRange(match.range.first, match.range.last + 1)
            registrar.startInjecting(language)
            registrar.addPlace(null, null, context, range)
            registrar.doneInjecting()
            ranges.add(range)
        }
        return ranges
    }

    private fun isGithubActionsFile(
        context: PsiElement,
        virtualFile: VirtualFile
    ): Boolean {
        return hasGithubActionOrWorkflowSchema(context, virtualFile)
    }

    private fun hasGithubActionOrWorkflowSchema(
        context: PsiElement,
        virtualFile: VirtualFile
    ): Boolean {
        val schemaService = JsonSchemaService.Impl.get(context.project)

        return schemaService
            .getSchemaFilesForFile(virtualFile)
            .any { schemaFile ->
                listOf("github-action", "github-workflow").any { schemaFile.name == "${it}.json" }
            }
    }

    private fun resolveLanguage(
        context: YAMLScalar,
        virtualFile: VirtualFile
    ): Language? {
        var language: Language? = null
        for (resolver in resolvers) {
            language = resolver.resolveLanguage(context, virtualFile)
            if (language != null) break
        }
        return language
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(YAMLScalar::class.java)
    }
}
