package io.getquill.context

import java.io.Closeable

import io.getquill.NamingStrategy

import scala.util.Try

trait Executor[C <: Context[Idiom, Naming], Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends Closeable with HandleSingleResult with BatchGroupTypes {
  override type PrepareRow = C#PrepareRow
  type Query

  def probe(statement: String): Try[_]

  def executeQuery[T](query: Query, prepare: C#PrepareRow => C#PrepareRow = identity, extractor: C#ResultRow => T = identity[C#ResultRow] _): C#RunQueryResult[T]
  def executeQuerySingle[T](query: Query, prepare: C#PrepareRow => C#PrepareRow = identity, extractor: C#ResultRow => T = identity[C#ResultRow] _): C#RunQuerySingleResult[T]
  def executeAction(query: Query, prepare: C#PrepareRow => C#PrepareRow = identity): C#RunActionResult
  def executeActionReturning[O](query: Query, prepare: C#PrepareRow => C#PrepareRow = identity, extractor: C#ResultRow => O, returningColumn: Query): C#RunActionReturningResult[O]
  def executeBatchAction(groups: List[BatchGroup]): C#RunBatchActionResult
  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: C#ResultRow => T): C#RunBatchActionReturningResult[T]
}
