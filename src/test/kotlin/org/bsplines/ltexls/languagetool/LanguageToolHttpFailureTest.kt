/* Copyright (C) 2019-2025
 * Julian Valentin, Daniel Spitzer, LTeX+ Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.languagetool

import com.sun.net.httpserver.HttpServer
import org.bsplines.ltexls.parsing.AnnotatedTextFragment
import org.bsplines.ltexls.parsing.CodeFragment
import org.bsplines.ltexls.server.DocumentCheckerTest
import org.bsplines.ltexls.settings.Settings
import org.languagetool.markup.AnnotatedTextBuilder
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LanguageToolHttpFailureTest {
  @Test
  fun testRetriesServerError() {
    val requestCount = AtomicInteger()
    val server = createServer { requestCount.incrementAndGet() == 1 }
    try {
      val languageTool = LanguageToolHttpInterface(serverUrl(server), "en-US", "")
      assertEquals(emptyList(), languageTool.check(createFragment()))
      assertEquals(2, requestCount.get())
    } finally {
      server.stop(0)
    }
  }

  @Test
  fun testPersistentServerErrorIsNotAnEmptyResult() {
    val requestCount = AtomicInteger()
    val server =
      createServer {
        requestCount.incrementAndGet()
        true
      }
    try {
      val languageTool = LanguageToolHttpInterface(serverUrl(server), "en-US", "")
      assertFailsWith<RuntimeException> { languageTool.check(createFragment()) }
      assertEquals(3, requestCount.get())
    } finally {
      server.stop(0)
    }
  }

  companion object {
    private const val SUCCESS_RESPONSE = "{\"language\":{\"code\":\"en-US\"},\"matches\":[]}"

    private fun createServer(shouldFail: () -> Boolean): HttpServer {
      val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
      server.createContext("/v2/check") { exchange ->
        exchange.requestBody.use { it.readAllBytes() }
        val fail = shouldFail()
        val response = if (fail) "temporary failure" else SUCCESS_RESPONSE
        val bytes = response.toByteArray(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(if (fail) 500 else 200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
      }
      server.start()
      return server
    }

    private fun serverUrl(server: HttpServer): String = "http://127.0.0.1:${server.address.port}"

    private fun createFragment(): AnnotatedTextFragment {
      val code = "Text."
      return AnnotatedTextFragment(
        AnnotatedTextBuilder().addText(code).build(),
        CodeFragment("plaintext", code, 0, Settings()),
        DocumentCheckerTest.createDocument("plaintext", code),
      )
    }
  }
}
