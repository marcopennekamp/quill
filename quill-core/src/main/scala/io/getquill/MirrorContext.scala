package io.getquill

import io.getquill.MirrorContext._
import io.getquill.context.Context
import io.getquill.context.mirror.{ MirrorDecoders, MirrorEncoders, Row }
import io.getquill.idiom.{ Idiom => BaseIdiom }

class MirrorContext[Idiom <: BaseIdiom, Naming <: NamingStrategy]
  extends Context[Idiom, Naming]
  with MirrorEncoders
  with MirrorDecoders {

  type Exec = MirrorExecutor[Idiom, Naming]

  override type PrepareRow = Row
  override type ResultRow = Row
  override type RunQueryResult[T] = QueryMirror[T]
  override type RunQuerySingleResult[T] = QueryMirror[T]
  override type RunActionResult = ActionMirror
  override type RunActionReturningResult[T] = ActionReturningMirror[T]
  override type RunBatchActionResult = BatchActionMirror
  override type RunBatchActionReturningResult[T] = BatchActionReturningMirror[T]

  def executeQuery[T](sql: String, prepare: Row => Row = identity, extractor: Row => T = identity[Row] _)(implicit executor: Exec) =
    executor.executeQuery(sql, prepare, extractor)

  def executeQuerySingle[T](sql: String, prepare: Row => Row = identity, extractor: Row => T = identity[Row] _)(implicit executor: Exec) =
    executor.executeQuerySingle(sql, prepare, extractor)

  def executeAction(sql: String, prepare: Row => Row = identity)(implicit executor: Exec) =
    executor.executeAction(sql, prepare)

  def executeActionReturning[O](sql: String, prepare: Row => Row = identity, extractor: Row => O, returningColumn: String)(implicit executor: Exec) =
    executor.executeActionReturning(sql, prepare, extractor, returningColumn)

  def executeBatchAction(groups: List[BatchGroup])(implicit executor: Exec) =
    executor.executeBatchAction(groups.asInstanceOf[List[executor.BatchGroup]]) // TODO: Temporary fix for path-dependent BatchGroup types.

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Row => T)(implicit executor: Exec) =
    executor.executeBatchActionReturning(groups.asInstanceOf[List[executor.BatchGroupReturning]], extractor) // TODO: Temporary fix for path-dependent BatchGroup types.
}

object MirrorContext {
  case class ActionMirror(string: String, prepareRow: Row)
  case class ActionReturningMirror[T](string: String, prepareRow: Row, extractor: Row => T, returningColumn: String)
  case class BatchActionMirror(groups: List[(String, List[Row])])
  case class BatchActionReturningMirror[T](groups: List[(String, String, List[Row])], extractor: Row => T)
  case class QueryMirror[T](string: String, prepareRow: Row, extractor: Row => T)
}