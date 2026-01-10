package com.gharunsyntaxsupport

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GithubActionsRunHighlighterInjectorTest : LightPlatformCodeInsightFixture4TestCase() {

    override fun getTestDataPath(): String = "src/test/testData"

    @Test
    fun `shell script injection in run step`() {
        runTestCase("run-bash.yaml", "Shell Script")
    }

    @Test
    fun `shell script injection with github expressions in run step`() {
        runTestCase("run-bash-with-github-expressions.yaml", "GithubExpressionLanguage")
    }

    @Test
    fun `python script injection in run step`() {
        runTestCase("run-python.yaml", "Python")
    }

    @Test
    fun `github script injection in run step`() {
        runTestCase("run-ghscript.yaml", "JavaScript")
    }

    private fun runTestCase(file: String, language: String) {
        val file = myFixture.configureByFile(".github/${file}")
        val offset = myFixture.caretOffset
        val element = file.findElementAt(offset)
        assertThat(element).isNotNull
        assertThat(element!!.language.id).isEqualTo(language)
    }
}