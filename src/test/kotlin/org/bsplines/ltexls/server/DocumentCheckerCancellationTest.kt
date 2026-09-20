/* Copyright (C) 2019-2025
 * Julian Valentin, Daniel Spitzer, LTeX+ Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.bsplines.ltexls.settings.Settings
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DocumentCheckerCancellationTest {
  @Test
  fun testHttpCheckStopsBeforeNextFragmentAfterCancellation() {
    val requestCount = AtomicInteger()
    lateinit var document: LtexTextDocumentItem
    val httpServer =
      HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v2/check") { exchange: HttpExchange ->
          if (requestCount.incrementAndGet() == 1) document.cancelCheck()
          respondWithEmptyMatches(exchange)
        }
        start()
      }
    val languageServer = LtexLanguageServer()

    try {
      languageServer.settingsManager.settings =
        Settings(
          _enabled = setOf("latex"),
          _languageShortCode = "en-US",
          _languageToolHttpServerUri = "http://127.0.0.1:${httpServer.address.port}",
          _paragraphCacheEnabled = false,
        )
      document =
        LtexTextDocumentItem(
          languageServer,
          "untitled:references.bib",
          "bibtex",
          1,
          """
          @article{first,
            title = {First checked field},
            abstract = {Second field must not be checked after cancellation}
          }
          """.trimIndent(),
        )

      assertFailsWith<CancellationException> {
        languageServer.documentChecker.check(document)
      }
      assertEquals(1, requestCount.get())
    } finally {
      languageServer.shutdown()
      httpServer.stop(0)
    }
  }

  companion object {
    private fun respondWithEmptyMatches(exchange: HttpExchange) {
      val response = """{"language":{"code":"en-US"},"matches":[]}"""
      val bytes: ByteArray = response.toByteArray(StandardCharsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
  }
}
