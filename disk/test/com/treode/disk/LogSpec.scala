package com.treode.disk

import scala.util.Random

import com.treode.async._
import com.treode.async.implicits._
import com.treode.async.io.stubs.StubFile
import com.treode.async.stubs.StubScheduler
import com.treode.async.stubs.implicits._
import com.treode.disk.stubs.CrashChecks
import com.treode.pickle.{InvalidTagException, Picklers}
import com.treode.tags.Periodic
import org.scalatest.FlatSpec

import Async.{async, latch}
import DiskTestTools._

class LogSpec extends FlatSpec with CrashChecks {

  class DistinguishedException extends Exception

  implicit val config = TestDisksConfig()
  val geometry = TestDiskGeometry()

  object records {
    val str = RecordDescriptor (0xBF, Picklers.string)
    val stuff = RecordDescriptor (0x2B, Stuff.pickler)
  }

  "The logger" should "replay zero items" in {

    var file: StubFile = null

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile()
      implicit val recovery = Disks.recover()
      implicit val disks = recovery.attachAndLaunch (("a", file, geometry))
    }

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile (file.data)
      implicit val recovery = Disks.recover()
      val replayed = Seq.newBuilder [String]
      records.str.replay (replayed += _)
      recovery.reattachAndLaunch (("a", file))
      assertResult (Seq.empty) (replayed.result)
    }}

  it should "replay one item" in {

    var file: StubFile = null

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile()
      implicit val recovery = Disks.recover()
      implicit val disks = recovery.attachAndLaunch (("a", file, geometry))
      records.str.record ("one") .pass
    }

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile (file.data)
      implicit val recovery = Disks.recover()
      val replayed = Seq.newBuilder [String]
      records.str.replay (replayed += _)
      recovery.reattachAndLaunch (("a", file))
      assertResult (Seq ("one")) (replayed.result)
    }}

  it should "replay twice" in {

    var file: StubFile = null

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile()
      implicit val recovery = Disks.recover()
      implicit val disks = recovery.attachAndLaunch (("a", file, geometry))
      records.str.record ("one") .pass
    }

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile (file.data)
      implicit val recovery = Disks.recover()
      val replayed = Seq.newBuilder [String]
      records.str.replay (replayed += _)
      implicit val disks = recovery.reattachAndLaunch (("a", file))
      assertResult (Seq ("one")) (replayed.result)
      records.str.record ("two") .pass
    }

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile (file.data)
      implicit val recovery = Disks.recover()
      val replayed = Seq.newBuilder [String]
      records.str.replay (replayed += _)
      recovery.reattachAndLaunch (("a", file))
      assertResult (Seq ("one", "two")) (replayed.result)
    }}

  it should "report an unrecognized record" in {

    var file: StubFile = null

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile()
      implicit val recovery = Disks.recover()
      implicit val disks = recovery.attachAndLaunch (("a", file, geometry))
      records.str.record ("one") .pass
    }

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile (file.data)
      implicit val recovery = Disks.recover()
      file = StubFile (file.data)
      recovery.reattachAndWait (("a", file)) .fail [InvalidTagException]
    }}

  it should "report an error from a replay function" in {

    var file: StubFile = null

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile()
      implicit val recovery = Disks.recover()
      implicit val disks = recovery.attachAndLaunch (("a", file, geometry))
      records.str.record ("one") .pass
    }

    {
      implicit val scheduler = StubScheduler.random()
      file = StubFile (file.data)
      implicit val recovery = Disks.recover()
      records.str.replay (_ => throw new DistinguishedException)
      recovery.reattachAndWait (("a", file)) .fail [DistinguishedException]
    }}

  it should "reject an oversized record" in {

    {
      implicit val scheduler = StubScheduler.random()
      val file = StubFile()
      implicit val recovery = Disks.recover()
      implicit val disks = recovery.attachAndLaunch (("a", file, geometry))
      records.stuff.record (Stuff (0, 1000)) .fail [OversizedRecordException]
    }}


  it should "run one checkpoint at a time" taggedAs (Periodic) in {
    forAllSeeds { implicit random =>

      implicit val scheduler = StubScheduler.random (random)
      val file = StubFile()
      val recovery = Disks.recover()
      val launch = recovery.attachAndWait (("a", file, geometry)) .pass
      import launch.disks

      var checkpointed = false
      var checkpointing = false
      launch.checkpoint (async [Unit] { cb =>
        assert (!checkpointing, "Expected one checkpoint at a time.")
        scheduler.execute {
          checkpointing = false
          cb.pass()
        }
        checkpointed = true
        checkpointing = true
      })
      launch.launch()
      scheduler.runTasks()

      latch (
          disks.checkpoint(),
          disks.checkpoint(),
          disks.checkpoint()) .pass
      assert (checkpointed, "Expected a checkpoint")
    }}}
