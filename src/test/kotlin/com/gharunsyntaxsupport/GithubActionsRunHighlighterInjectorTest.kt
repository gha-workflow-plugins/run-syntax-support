package com.gharunsyntaxsupport

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GithubActionsRunHighlighterInjectorTest : LightPlatformCodeInsightFixture4TestCase() {

    override fun getTestDataPath(): String = "src/test/testData"


    @Test
    fun `gracefully handles unresolvable cases`() {
        val file = myFixture.configureByFile(".github/workflows/run-unresolvable.yaml")
        val offset = myFixture.caretOffset
        val element = file.findElementAt(offset)
        assertThat(element).isNotNull
        assertThat(element!!.language.id).isEqualTo("yaml")
    }

    @Test
    fun `does not inject in non workflow github yaml file`() {
        val file = myFixture.configureByFile(".github/run-bash.yaml")
        val offset = myFixture.caretOffset
        val element = file.findElementAt(offset)
        assertThat(element).isNotNull
        assertThat(element!!.language.id).isEqualTo("yaml")
    }

    @Test
    fun `does not inject in indent prefix`() {
        runTestCase("run-unresolvable-in-indent-prefix.yaml", "yaml")
    }

    @Test
    fun `shell script injection in run step`() {
        runTestCase("run-bash.yaml", "Shell Script")
    }

    @Test
    fun `shell script injection on later line of multiline run step`() {
        runTestCase("run-bash-multiline.yaml", "Shell Script")
    }

    @Test
    fun `shell script injection with github expressions in run step`() {
        runTestCase("run-bash-with-github-expressions.yaml", "GithubExpressionLanguage")
    }

    @Test
    fun `github expression injection when run step starts with expression`() {
        runTestCase("run-bash-leading-github-expression.yaml", "GithubExpressionLanguage")
    }

    @Test
    fun `python script injection in run step`() {
        runTestCase("run-python.yaml", "Python")
    }

    @Test
    fun `github script injection in run step`() {
        runTestCase("run-ghscript.yaml", "JavaScript")
    }

    @Test
    fun `shell script injection in action run step`() {
        runActionTestCase("run-bash", "Shell Script")
    }

    private fun runActionTestCase(dir: String, language: String) {
        runTestCase("action.yaml", language, dir)
    }

    private fun runTestCase(file: String, language: String, dir: String = ".github/workflows") {
        val file = myFixture.configureByFile("$dir/$file")
        val offset = myFixture.caretOffset
        val element = file.findElementAt(offset)
        assertThat(element).isNotNull
        assertThat(element!!.language.id).isEqualTo(language)
    }
}
