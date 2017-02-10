package io.getquill.context

trait BatchGroupTypes {
  type PrepareRow
  case class BatchGroup(string: String, prepare: List[PrepareRow => PrepareRow])
  case class BatchGroupReturning(string: String, column: String, prepare: List[PrepareRow => PrepareRow])
}
