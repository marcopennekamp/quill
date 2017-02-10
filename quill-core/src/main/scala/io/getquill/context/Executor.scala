package io.getquill.context

import io.getquill.NamingStrategy

trait Executor[C <: Context[Idiom, Naming], Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends BatchGroupTypes with HandleSingleResult {
  override type PrepareRow = C#PrepareRow
  type Query

  def executeQuery[T](query: Query, prepare: C#PrepareRow => C#PrepareRow = identity, extractor: C#ResultRow => T = identity[C#ResultRow] _): C#RunQueryResult[T]

  def executeQuerySingle[T](query: Query, prepare: C#PrepareRow => C#PrepareRow = identity, extractor: C#ResultRow => T = identity[C#ResultRow] _): C#RunQuerySingleResult[T]

  def executeAction[T](query: Query, prepare: C#PrepareRow => C#PrepareRow = identity): C#RunActionResult

  def executeActionReturning[O](query: Query, prepare: C#PrepareRow => C#PrepareRow = identity, extractor: C#ResultRow => O, returningColumn: Query): C#RunActionReturningResult[O]

  def executeBatchAction(groups: List[BatchGroup]): C#RunBatchActionResult

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: C#ResultRow => T): C#RunBatchActionReturningResult[T]
}
