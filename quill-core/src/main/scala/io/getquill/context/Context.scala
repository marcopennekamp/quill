package io.getquill.context

import scala.language.higherKinds
import scala.language.experimental.macros
import io.getquill.dsl.CoreDsl
import io.getquill.NamingStrategy

trait Context[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends CoreDsl with HandleSingleResult with BatchGroupTypes {
  type RunQuerySingleResult[T]
  type RunQueryResult[T]
  type RunActionResult
  type RunActionReturningResult[T]
  type RunBatchActionResult
  type RunBatchActionReturningResult[T]

  def run[T](quoted: Quoted[T]): RunQuerySingleResult[T] = macro QueryMacro.runQuerySingle[T]
  def run[T](quoted: Quoted[Query[T]]): RunQueryResult[T] = macro QueryMacro.runQuery[T]
  def run(quoted: Quoted[Action[_]]): RunActionResult = macro ActionMacro.runAction
  def run[T](quoted: Quoted[ActionReturning[_, T]]): RunActionReturningResult[T] = macro ActionMacro.runActionReturning[T]
  def run(quoted: Quoted[BatchAction[Action[_]]]): RunBatchActionResult = macro ActionMacro.runBatchAction
  def run[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): RunBatchActionReturningResult[T] = macro ActionMacro.runBatchActionReturning[T]
}
