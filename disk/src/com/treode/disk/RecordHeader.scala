package com.treode.disk

private sealed abstract class RecordHeader

private object RecordHeader {

  case object End extends RecordHeader
  case class Continue (seg: Int) extends RecordHeader
  case class Entry (time: Long, id: TypeId) extends RecordHeader

  val pickler = {
    import DiskPicklers._
    tagged [RecordHeader] (
        0x1 -> const (End),
        0x2 -> wrap (int) .build (Continue.apply _) .inspect (_.seg),
        0x3 -> wrap (fixedLong, typeId)
            .build ((Entry.apply _).tupled)
            .inspect (v => (v.time, v.id)))
  }}