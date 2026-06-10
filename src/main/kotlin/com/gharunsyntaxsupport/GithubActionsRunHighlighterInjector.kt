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
    companion object {
        private val SCHEMA_FILE_NAMES = listOf("github-action.json", "github-workflow.json")
        private val GITHUB_EXPRESSION_REGEX = Regex("\\$\\{\\{.*?}}")
    }

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

            val text = context.text
            val ranges = mutableListOf<TextRange>()
            for (originalRange in originalRanges) {
                var range = originalRange
                // trim trailing blank lines so they are not highlighted
                while (range.length >= 2 &&
                    text[range.endOffset - 1] == '\n' &&
                    text[range.endOffset - 2] == '\n'
                ) {
                    range = TextRange(range.startOffset, range.endOffset - 1)
                }
                ranges.add(range)
            }

            val language = resolveLanguage(context, virtualFile) ?: return

            val expressionRanges = findGithubExpressionRanges(text)
            injectGithubExpressions(registrar, context, expressionRanges)

            // Skip GitHub Expressions
            val bashRanges = resolveExclusions(ranges, expressionRanges)

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
                    if (nextStartOffset < excluded.startOffset) {
                        resolved.add(TextRange(nextStartOffset, excluded.startOffset))
                    }
                    nextStartOffset = maxOf(nextStartOffset, excluded.endOffset)
                }
            }
            if (nextStartOffset < range.endOffset) {
                resolved.add(TextRange(nextStartOffset, range.endOffset))
            }
        }
        return resolved
    }

    private fun findGithubExpressionRanges(text: String): List<TextRange> {
        return GITHUB_EXPRESSION_REGEX.findAll(text)
            .map { TextRange(it.range.first, it.range.last + 1) }
            .toList()
    }

    private fun injectGithubExpressions(
        registrar: MultiHostRegistrar,
        context: YAMLScalar,
        ranges: List<TextRange>
    ) {
        if (ranges.isEmpty()) return
        val language = Language.findLanguageByID("GithubExpressionLanguage") ?: return
        for (range in ranges) {
            registrar.startInjecting(language)
            registrar.addPlace(null, null, context, range)
            registrar.doneInjecting()
        }
    }

    private fun isGithubActionsFile(
        context: PsiElement,
        virtualFile: VirtualFile
    ): Boolean {
        val schemaService = JsonSchemaService.Impl.get(context.project)

        return schemaService
            .getSchemaFilesForFile(virtualFile)
            .any { schemaFile ->
                SCHEMA_FILE_NAMES.any { schemaFile.name.equals(it, ignoreCase = true) }
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
