/* Copyright (C) 2019-2025
 * Julian Valentin, Daniel Spitzer, LTeX+ Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing

import org.bsplines.ltexls.server.DocumentCheckerTest
import org.bsplines.ltexls.settings.Settings
import org.languagetool.markup.AnnotatedTextBuilder
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnnotatedTextFragmentTest {
  @Test
  fun testGeneratedMarkupRanges() {
    val code = "See @sec:x."
    val annotatedText =
      AnnotatedTextBuilder()
        .addText("See ")
        .addMarkup("@sec:x", "section")
        .addText(".")
        .build()
    val fragment =
      AnnotatedTextFragment(
        annotatedText,
        CodeFragment("typst", code, 0, Settings()),
        DocumentCheckerTest.createDocument("typst", code),
      )

    assertTrue(fragment.isRangeEntirelyMarkup(4, 10))
    assertTrue(fragment.doesRangeIntersectMarkup(4, 10))
    assertFalse(fragment.isRangeEntirelyMarkup(0, code.length))
    assertTrue(fragment.doesRangeIntersectMarkup(0, code.length))
    assertFalse(fragment.doesRangeIntersectMarkup(0, 3))
  }
}
