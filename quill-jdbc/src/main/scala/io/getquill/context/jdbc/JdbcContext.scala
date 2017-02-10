package io.getquill.context.jdbc

import java.io.Closeable
import java.sql.{ Connection, PreparedStatement, ResultSet }
import javax.sql.DataSource

import com.typesafe.scalalogging.Logger
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.{ DynamicVariable, Try }
import scala.util.control.NonFatal

abstract class JdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy](dataSource: DataSource with Closeable)
  extends SqlContext[Dialect, Naming]
  with Encoders
  with Decoders {

  type Exec = JdbcExecutor[Dialect, Naming]

  private val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[JdbcContext[_, _]]))

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet

  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  protected val currentConnection = new DynamicVariable[Option[Connection]](None)

  protected def withConnection[T](f: Connection => T) =
    currentConnection.value.map(f).getOrElse {
      val conn = dataSource.getConnection
      try f(conn)
      finally conn.close()
    }

  def close() = dataSource.close()

  def probe(sql: String) =
    Try {
      withConnection(_.createStatement.execute(sql))
    }

  def transaction[T](f: => T) =
    currentConnection.value match {
      case Some(_) => f // already in transaction
      case None =>
        withConnection { conn =>
          currentConnection.withValue(Some(conn)) {
            val wasAutoCommit = conn.getAutoCommit
            conn.setAutoCommit(false)
            try {
              val res = f
              conn.commit()
              res
            } catch {
              case NonFatal(e) =>
                conn.rollback()
                throw e
            } finally
              conn.setAutoCommit(wasAutoCommit)
          }
        }
    }

  def executeQuery[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => T = identity[ResultSet] _)(implicit executor: Exec): List[T] =
    executor.executeQuery(sql, prepare, extractor)

  def executeQuerySingle[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => T = identity[ResultSet] _)(implicit executor: Exec): T =
    executor.executeQuerySingle(sql, prepare, extractor)

  def executeAction[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity)(implicit executor: Exec): Long =
    executor.executeAction(sql, prepare)

  def executeActionReturning[O](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => O, returningColumn: String)(implicit executor: Exec): O =
    executor.executeActionReturning(sql, prepare, extractor, returningColumn)

  def executeBatchAction(groups: List[BatchGroup])(implicit executor: Exec): List[Long] =
    executor.executeBatchAction(groups.asInstanceOf[List[executor.BatchGroup]]) // TODO: Temporary fix for path-dependent type.

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: ResultSet => T)(implicit executor: Exec): List[T] =
    executor.executeBatchActionReturning(groups.asInstanceOf[List[executor.BatchGroupReturning]], extractor) // TODO: Temporary fix for path-dependent type.

  @tailrec
  private def extractResult[T](rs: ResultSet, extractor: ResultSet => T, acc: List[T] = List()): List[T] =
    if (rs.next)
      extractResult(rs, extractor, extractor(rs) :: acc)
    else
      acc.reverse
}
