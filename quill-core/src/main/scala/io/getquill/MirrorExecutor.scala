package io.getquill

import io.getquill.MirrorContext._
import io.getquill.context.Executor
import io.getquill.context.mirror.Row

import scala.util.{ Failure, Success, Try }

class MirrorExecutorWithQueryProbing[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends MirrorExecutor[Idiom, Naming] with QueryProbing

class MirrorExecutor[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends Executor[MirrorContext[Idiom, Naming], Idiom, Naming] {
  override type Query = String

  override def probe(statement: String): Try[_] =
    if (statement.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  override def close(): Unit = ()

  def transaction[T](f: => T) = f

  override def executeQuery[T](string: String, prepare: Row => Row = identity, extractor: Row => T = identity[Row] _) =
    QueryMirror(string, prepare(Row()), extractor)

  override def executeQuerySingle[T](string: String, prepare: Row => Row = identity, extractor: Row => T = identity[Row] _) =
    QueryMirror(string, prepare(Row()), extractor)

  def executeAction(string: String, prepare: Row => Row = identity) =
    ActionMirror(string, prepare(Row()))

  override def executeActionReturning[O](string: String, prepare: Row => Row = identity, extractor: Row => O,
                                         returningColumn: String) =
    ActionReturningMirror[O](string, prepare(Row()), extractor, returningColumn)

  override def executeBatchAction(groups: List[BatchGroup]) =
    BatchActionMirror {
      groups.map {
        case BatchGroup(string, prepare) =>
          (string, prepare.map(_(Row())))
      }
    }

  override def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Row => T) =
    BatchActionReturningMirror[T](
      groups.map {
        case BatchGroupReturning(string, column, prepare) =>
          (string, column, prepare.map(_(Row())))
      }, extractor
    )
}
