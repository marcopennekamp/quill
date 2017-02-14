package io.getquill.context.jdbc

import java.sql.{ PreparedStatement, ResultSet }

import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy

abstract class JdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends SqlContext[Dialect, Naming] with Encoders with Decoders {
  type Exec = JdbcExecutor[Dialect, Naming]

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet

  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  def executeQuery[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => T = identity[ResultSet] _)(implicit executor: Exec): List[T] =
    executor.executeQuery(sql, prepare, extractor)

  def executeQuerySingle[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => T = identity[ResultSet] _)(implicit executor: Exec): T =
    executor.executeQuerySingle(sql, prepare, extractor)

  def executeAction(sql: String, prepare: PreparedStatement => PreparedStatement = identity)(implicit executor: Exec): Long =
    executor.executeAction(sql, prepare)

  def executeActionReturning[O](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => O, returningColumn: String)(implicit executor: Exec): O =
    executor.executeActionReturning(sql, prepare, extractor, returningColumn)

  def executeBatchAction(groups: List[BatchGroup])(implicit executor: Exec): List[Long] =
    executor.executeBatchAction(groups.asInstanceOf[List[executor.BatchGroup]]) // TODO: Temporary fix for path-dependent BatchGroup types.

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: ResultSet => T)(implicit executor: Exec): List[T] =
    executor.executeBatchActionReturning(groups.asInstanceOf[List[executor.BatchGroupReturning]], extractor) // TODO: Temporary fix for path-dependent BatchGroup types.
}
