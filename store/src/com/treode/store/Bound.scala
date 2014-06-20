package com.treode.store

import com.treode.pickle.Pickler

sealed abstract class Bound [A] {

  def bound: A
  def inclusive: Boolean
  def <* (v: A) (implicit ordering: Ordering [A]): Boolean
  def >* (v: A) (implicit ordering: Ordering [A]): Boolean
}

object Bound {

  case class Inclusive [A] (bound: A) extends Bound [A] {

    def inclusive = true

    def <* (other: A) (implicit ordering: Ordering [A]) =
      ordering.lteq (bound, other)

    def >* (other: A) (implicit ordering: Ordering [A]) =
      ordering.gteq (bound, other)
  }

  case class Exclusive [A] (bound: A) extends Bound [A] {

    def inclusive = false

    def <* (other: A) (implicit ordering: Ordering [A]) =
      ordering.lt (bound, other)

    def >*  (other: A) (implicit ordering: Ordering [A]) =
      ordering.gt (bound, other)
  }

  def apply [A] (bound: A, inclusive: Boolean): Bound [A] =
    if (inclusive)
      Inclusive (bound)
    else
      Exclusive (bound)

  val firstKey = Inclusive (Key.MinValue)

  def pickler [A] (pa: Pickler [A]) = {
    import StorePicklers._
    tagged [Bound [A]] (
      0x1 -> wrap (pa) .build (new Inclusive (_)) .inspect (_.bound),
      0x2 -> wrap (pa) .build (new Exclusive (_)) .inspect (_.bound))
    }}