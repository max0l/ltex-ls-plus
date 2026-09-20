/* Copyright (C) 2019-2025
 * Julian Valentin, Daniel Spitzer, LTeX+ Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.server

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrioritizedSingleThreadExecutorTest {
  @Test
  fun testInteractiveWorkBypassesQueuedBackgroundWork() {
    val executor = PrioritizedSingleThreadExecutor()
    val runningStarted = CountDownLatch(1)
    val releaseRunning = CountDownLatch(1)
    val queuedFinished = CountDownLatch(2)
    val order = CopyOnWriteArrayList<String>()

    try {
      executor.execute {
        runningStarted.countDown()
        releaseRunning.await()
        order.add("running")
      }
      assertTrue(runningStarted.await(5, TimeUnit.SECONDS))

      executor.execute {
        order.add("background")
        queuedFinished.countDown()
      }
      executor.executeInteractive {
        order.add("interactive")
        queuedFinished.countDown()
      }

      releaseRunning.countDown()
      assertTrue(queuedFinished.await(5, TimeUnit.SECONDS))
      assertEquals(listOf("running", "interactive", "background"), order)
    } finally {
      releaseRunning.countDown()
      executor.shutdownNow()
    }
  }
}
