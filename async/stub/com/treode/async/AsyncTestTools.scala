package com.treode.async

import org.scalatest.Assertions

import Assertions.assertResult
import Async.supply

trait AsyncTestTools {

  implicit class RichAsync [A] (async: Async [A]) {

    def capture(): CallbackCaptor [A] = {
      val cb = CallbackCaptor [A]
      async run cb
      cb
    }

    def pass (implicit scheduler: StubScheduler): A = {
      val cb = capture()
      scheduler.runTasks()
      cb.passed
    }

    def fail [E] (implicit scheduler: StubScheduler, m: Manifest [E]): E = {
      val cb = capture()
      scheduler.runTasks()
      cb.failed [E]
    }

    def expect (expected: A) (implicit scheduler: StubScheduler): Unit =
      assertResult (expected) (pass)

    def expectSeq [B] (xs: B*) (implicit s: StubScheduler, w: A <:< Seq [B]): Unit =
      assertResult (xs) (pass)
  }

  implicit class RichAsyncIterator [A] (iter: AsyncIterator [A]) {

    /** Iterate the entire asynchronous iterator and build a standard sequence. */
    def toSeq (implicit scheduler: StubScheduler): Seq [A] = {
      val builder = Seq.newBuilder [A]
      iter.foreach (x => supply (builder += x)) .pass
      builder.result
    }}}

object AsyncTestTools extends AsyncTestTools
