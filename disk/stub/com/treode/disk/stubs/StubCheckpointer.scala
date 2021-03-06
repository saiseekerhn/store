/*
 * Copyright 2014 Treode, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.treode.disk.stubs

import scala.collection.immutable.Queue
import scala.util.Random

import com.treode.async.{Async, Callback, Fiber, Scheduler}
import com.treode.async.implicits._
import com.treode.disk.CheckpointRegistry

import Callback.{fanout, ignore}

private class StubCheckpointer (implicit
    random: Random,
    scheduler: Scheduler,
    disk: StubDiskDrive,
    config: StubDisk.Config
) {

  val fiber = new Fiber
  var checkpoints: CheckpointRegistry = null
  var entries = 0
  var checkreqs = List.empty [Callback [Unit]]
  var engaged = true

  private def reengage() {
    fanout (checkreqs) .pass (())
    checkreqs = List.empty
    entries = 0
    engaged = false
  }

  private def _checkpoint() {
    engaged = true
    val mark = disk.mark()
    checkpoints .checkpoint() .map { _ =>
      disk.checkpoint (mark)
      fiber.execute (reengage())
    } .run (ignore)
  }

  def launch (checkpoints: CheckpointRegistry): Unit =
    fiber.execute {
      this.checkpoints = checkpoints
      if (!checkreqs.isEmpty || config.checkpoint (entries))
        _checkpoint()
      else
        engaged = false
    }

  def checkpoint(): Async [Unit] =
    fiber.async { cb =>
      checkreqs ::= cb
      if (!engaged)
        _checkpoint()
    }

  def tally(): Unit =
    fiber.execute {
      entries += 1
      if (!engaged && config.checkpoint (entries))
        _checkpoint()
    }}
