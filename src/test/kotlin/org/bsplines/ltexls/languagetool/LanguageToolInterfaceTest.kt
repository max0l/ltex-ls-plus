/* Copyright (C) 2019-2025
 * Julian Valentin, Daniel Spitzer, LTeX+ Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.languagetool

import org.bsplines.ltexls.parsing.AnnotatedTextFragment
import org.bsplines.ltexls.parsing.CodeFragment
import org.bsplines.ltexls.server.DocumentCheckerTest
import org.bsplines.ltexls.settings.Settings
import org.languagetool.markup.AnnotatedTextBuilder
import org.languagetool.rules.RuleMatch
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageToolInterfaceTest {
  @Test
  fun testGeneratedPlaceholderMatchesAreFiltered() {
    val code = "See @sec:x."
    val fragment =
      AnnotatedTextFragment(
        AnnotatedTextBuilder()
          .addText("See ")
          .addMarkup("@sec:x", "section")
          .addText(".")
          .build(),
        CodeFragment("typst", code, 0, Settings()),
        DocumentCheckerTest.createDocument("typst", code),
      )
    val matches =
      listOf(
        createMatch(4, 10, listOf("the section")),
        createMatch(0, 3, listOf("Look")),
        createMatch(0, 10, listOf("Dummy12")),
      )
    val languageTool = StubLanguageToolInterface(matches)

    assertEquals(listOf(matches[1]), languageTool.check(fragment))
  }

  @Test
  fun testGeneratedPlaceholderMessageMatchesAreFiltered() {
    val code = "The \\textbf{object} protects memory."
    val fragment =
      AnnotatedTextFragment(
        AnnotatedTextBuilder()
          .addText("The ")
          .addMarkup("\\textbf{object}", "object")
          .addText(" protects memory.")
          .build(),
        CodeFragment("latex", code, 0, Settings()),
        DocumentCheckerTest.createDocument("latex", code),
      )
    val matches =
      listOf(
        LanguageToolRuleMatch(
          "TEST_RULE",
          "The object protects memory.",
          4,
          10,
          "Use 'object' instead of 'Dummy0'.",
          listOf("object"),
          RuleMatch.Type.Hint,
          "en-US",
        ),
        createMatch(14, 22, listOf("stores")),
      )
    val languageTool = StubLanguageToolInterface(matches)

    assertEquals(listOf(matches[1]), languageTool.check(fragment))
  }

  private class StubLanguageToolInterface(
    private val matches: List<LanguageToolRuleMatch>,
  ) : LanguageToolInterface() {
    override fun checkInternal(annotatedTextFragment: AnnotatedTextFragment) = matches

    override fun isInitialized() = true

    override fun activateDefaultFalseFriendRules() = Unit

    override fun activateLanguageModelRules(languageModelRulesDirectory: String) = Unit

    override fun enableRules(ruleIds: Set<String>) = Unit

    override fun enableEasterEgg() = Unit
  }

  companion object {
    private fun createMatch(
      fromPos: Int,
      toPos: Int,
      replacements: List<String>,
    ) = LanguageToolRuleMatch(
      "TEST_RULE",
      "See section.",
      fromPos,
      toPos,
      "Test message",
      replacements,
      RuleMatch.Type.Hint,
      "en-US",
    )
  }
}
