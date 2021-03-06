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

package com.treode.store

import java.util.concurrent.ExecutorService
import scala.util.Random

import com.treode.async.{Async, Scheduler}
import com.treode.cluster.Cluster
import com.treode.disk.Disk
import com.treode.store.atomic.Atomic
import com.treode.store.catalog.Catalogs
import com.treode.store.paxos.Paxos

import Async.latch
import Store.Controller

private class RecoveryKit (implicit
    random: Random,
    scheduler: Scheduler,
    recovery: Disk.Recovery,
    config: Store.Config
) extends Store.Recovery {

  implicit val library = new Library

  implicit val _catalogs = Catalogs.recover()
  val _paxos = Paxos.recover()
  val _atomic = Atomic.recover()

  def launch (implicit launch: Disk.Launch, cluster: Cluster): Async [Controller] = {

    for {
      catalogs <- _catalogs.launch (launch, cluster)
      paxos <- _paxos.launch (launch, cluster)
      atomic <- _atomic.launch (launch, cluster, paxos)
    } yield {

      val librarian = Librarian { atlas =>
        latch (paxos.rebalance (atlas), atomic.rebalance (atlas)) .map (_ => ())
      } (scheduler, cluster, catalogs, library)

      new SimpleController (cluster, launch.controller, library, librarian, catalogs, atomic)
    }}}
