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

package com.treode.twitter

import scala.util.{Failure, Success}

import com.treode.async.Async, Async.async
import com.treode.async.implicits._
import com.twitter.util.{Future, Promise, Return, Throw}

package object util {

  implicit class TwitterRichAsync [A] (async: Async [A]) {

    def toTwitterFuture: Future [A] = {
      val promise = new Promise [A]
      async run {
        case Success (v) => promise.setValue (v)
        case Failure (t) => promise.setException (t)
      }
      promise
    }}

  implicit class RichTwitterFuture [A] (fut: Future [A]) {

    def toAsync: Async [A] =
      async { cb =>
        fut.respond {
          case Return (v) => cb.pass (v)
          case Throw (t) => cb.fail (t)
        }}}}