package io.getquill.context.jdbc

import java.io.Closeable
import java.sql.{ Connection, PreparedStatement, ResultSet }
import javax.sql.DataSource

import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ JdbcContextConfig, NamingStrategy }
import io.getquill.context.Executor
import io.getquill.util.LoadConfig
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.{ DynamicVariable, Try }
import scala.util.control.NonFatal

class JdbcExecutor[Dialect <: SqlIdiom, Naming <: NamingStrategy](dataSource: DataSource with Closeable) extends Executor[JdbcContext[Dialect, Naming], Dialect, Naming] {
  override type Query = String

  def this(config: JdbcContextConfig) = this(config.dataSource)
  def this(config: Config) = this(JdbcContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  private val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[JdbcContext[_, _]]))

  protected val currentConnection = new DynamicVariable[Option[Connection]](None)

  protected def withConnection[T](f: Connection => T) =
    currentConnection.value.map(f).getOrElse {
      val conn = dataSource.getConnection
      try f(conn)
      finally conn.close()
    }

  override def close() = dataSource.close()

  override def probe(sql: String) =
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

  override def executeQuery[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => T = identity[ResultSet] _): List[T] =
    withConnection { conn =>
      logger.info(sql)
      val ps = prepare(conn.prepareStatement(sql))
      val rs = ps.executeQuery()
      extractResult(rs, extractor)
    }

  override def executeQuerySingle[T](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => T = identity[ResultSet] _): T =
    handleSingleResult(executeQuery(sql, prepare, extractor))

  override def executeAction(sql: String, prepare: PreparedStatement => PreparedStatement = identity): Long =
    withConnection { conn =>
      logger.info(sql)
      prepare(conn.prepareStatement(sql)).executeUpdate().toLong
    }

  override def executeActionReturning[O](sql: String, prepare: PreparedStatement => PreparedStatement = identity, extractor: ResultSet => O, returningColumn: String): O =
    withConnection { conn =>
      logger.info(sql)
      val ps = prepare(conn.prepareStatement(sql, Array(returningColumn)))
      ps.executeUpdate()
      handleSingleResult(extractResult(ps.getGeneratedKeys, extractor))
    }

  override def executeBatchAction(groups: List[BatchGroup]): List[Long] =
    withConnection { conn =>
      groups.flatMap {
        case BatchGroup(sql, prepare) =>
          logger.info(sql)
          val ps = conn.prepareStatement(sql)
          prepare.foreach { f =>
            f(ps)
            ps.addBatch()
          }
          ps.executeBatch().map(_.toLong)
      }
    }

  override def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: ResultSet => T): List[T] =
    withConnection { conn =>
      groups.flatMap {
        case BatchGroupReturning(sql, column, prepare) =>
          logger.info(sql)
          val ps = conn.prepareStatement(sql, Array(column))
          prepare.foreach { f =>
            f(ps)
            ps.addBatch()
          }
          ps.executeBatch()
          extractResult(ps.getGeneratedKeys, extractor)
      }
    }

  @tailrec
  private def extractResult[T](rs: ResultSet, extractor: ResultSet => T, acc: List[T] = List()): List[T] =
    if (rs.next)
      extractResult(rs, extractor, extractor(rs) :: acc)
    else
      acc.reverse
}
