/* Copyright (C) 2019-2025
 * Julian Valentin, Daniel Spitzer, LTeX+ Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.server

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * A single-thread executor that preserves FIFO order within each priority.
 *
 * Document checks must remain serialized because they share SettingsManager and
 * LanguageTool instances. Interactive work can nevertheless bypass queued
 * didOpen checks after the active check observes cancellation.
 */
internal class PrioritizedSingleThreadExecutor : AbstractExecutorService() {
  private val sequence = AtomicLong()
  private val delegate =
    ThreadPoolExecutor(
      1,
      1,
      0L,
      TimeUnit.MILLISECONDS,
      PriorityBlockingQueue(),
    )

  override fun execute(command: Runnable) {
    enqueue(command, Priority.Background)
  }

  fun executeInteractive(command: Runnable) {
    enqueue(command, Priority.Interactive)
  }

  private fun enqueue(
    command: Runnable,
    priority: Priority,
  ) {
    this.delegate.execute(PrioritizedTask(command, priority, this.sequence.getAndIncrement()))
  }

  override fun shutdown() {
    this.delegate.shutdown()
  }

  override fun shutdownNow(): MutableList<Runnable> =
    this.delegate
      .shutdownNow()
      .map { (it as? PrioritizedTask)?.command ?: it }
      .toMutableList()

  override fun isShutdown(): Boolean = this.delegate.isShutdown

  override fun isTerminated(): Boolean = this.delegate.isTerminated

  override fun awaitTermination(
    timeout: Long,
    unit: TimeUnit,
  ): Boolean = this.delegate.awaitTermination(timeout, unit)

  private enum class Priority {
    Interactive,
    Background,
  }

  private class PrioritizedTask(
    val command: Runnable,
    private val priority: Priority,
    private val sequence: Long,
  ) : Runnable,
    Comparable<PrioritizedTask> {
    override fun run() {
      this.command.run()
    }

    override fun compareTo(other: PrioritizedTask): Int {
      val priorityComparison: Int = this.priority.compareTo(other.priority)
      if (priorityComparison != 0) return priorityComparison
      return this.sequence.compareTo(other.sequence)
    }
  }
}
