package com.treode.store.tier

import com.treode.cluster.concurrent.Callback

private class TierIterator (cache: BlockCache) {

  private var stack = List.empty [(IndexBlock, Int)]
  private var block: ValueBlock = null
  private var index = 0

  private def find (pos: Long, cb: Callback [Unit]) {
    cache.get (pos, new Callback [Block] {

      def apply (b: Block) {
        b match {
          case b: IndexBlock =>
            val e = b.get (0)
            stack ::= (b, 0)
            find (e.pos, cb)
          case b: ValueBlock =>
            block = b
            index = 0
            cb()
        }}

      def fail (t: Throwable) = cb.fail (t)
    })
  }

  def hasNext: Boolean =
    index < block.size

  def next (cb: Callback [ValueEntry]) {
    val entry = block.get (index)
    index += 1
    if (index == block.size && !stack.isEmpty) {
      var b = stack.head._1
      var i = stack.head._2 + 1
      stack = stack.tail
      while (i == b.size && !stack.isEmpty) {
        b = stack.head._1
        i = stack.head._2 + 1
        stack = stack.tail
      }
      if (i < b.size) {
        stack ::= (b, i)
        find (b.get (i) .pos, new Callback [Unit] {
          def apply (v: Unit): Unit = cb (entry)
          def fail (t: Throwable) = cb.fail (t)
        })
      } else {
        cb (entry)
      }
    } else {
      cb (entry)
    }}}

private object TierIterator {

  def apply (cache: BlockCache, pos: Long, cb: Callback [TierIterator]) {
    val iter = new TierIterator (cache)
    iter.find (pos, new Callback [Unit] {
      def apply (v: Unit): Unit = cb (iter)
      def fail (t: Throwable) = cb.fail (t)
    })
  }}