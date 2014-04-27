package com.treode.store

import scala.language.postfixOps

import com.treode.async.Backoff
import com.treode.async.misc.RichInt

class StoreConfig private (
    val priorValueEpoch: Epoch,
    val falsePositiveProbability: Double,
    val lockSpaceBits: Int,
    val targetPageBytes: Int,
    val rebalanceBytes: Int,
    val rebalanceEntries: Int
) {

  val scanBatchBytes = 1<<16
  val scanBatchEntries = 1000
  val scanBatchBackoff = Backoff (500, 500, 10 seconds, 7)

  val exodusThreshold = 0.2D

  val deliberatingTimeout = 2 seconds
  val preparingTimeout = 5 seconds
  val closedLifetime = 2 seconds

  val prepareBackoff = Backoff (100, 100, 1 seconds, 7)
  val readBackoff = Backoff (100, 100, 1 seconds, 7)
  val rebalanceBackoff = Backoff (500, 500, 5 minutes)
}

object StoreConfig {

  def apply (
      priorValueEpoch: Epoch,
      falsePositiveProbability: Double,
      lockSpaceBits: Int,
      targetPageBytes: Int,
      rebalanceBytes: Int,
      rebalanceEntries: Int
  ): StoreConfig = {

    require (
        0 < falsePositiveProbability && falsePositiveProbability < 1,
        "The false positive probability must be between 0 and 1 exclusive.")
    require (
        0 <= lockSpaceBits && lockSpaceBits <= 14,
        "The size of the lock space must be between 0 and 14 bits.")
    require (
        targetPageBytes > 0,
        "The target size of a page must be more than zero bytes.")
    require (
        rebalanceBytes > 0,
        "The rebalance batch size must be more than 0 bytes.")
    require (
        rebalanceEntries > 0,
        "The rebalance batch size must be more than 0 entries.")

    new StoreConfig (
        priorValueEpoch,
        falsePositiveProbability,
        lockSpaceBits,
        targetPageBytes,
        rebalanceBytes,
        rebalanceEntries)
  }

  def recommended (
      priorValueEpoch: Epoch = Epoch.StartOfYesterday,
      falsePositiveProbability: Double = 0.01,
      lockSpaceBits: Int = 10,
      targetPageBytes: Int = 1<<20,
      rebalanceBytes: Int = 1<<20,
      rebalanceEntries: Int = 10000
  ): StoreConfig =
    StoreConfig (
        priorValueEpoch,
        falsePositiveProbability,
        lockSpaceBits,
        targetPageBytes,
        rebalanceBytes,
        rebalanceEntries)
}
