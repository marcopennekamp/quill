package io.getquill.context.sql.norm

import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.ast.Property
import io.getquill.ast.StatefulTransformer
import io.getquill.context.sql.FlattenSqlQuery
import io.getquill.context.sql.FromContext
import io.getquill.context.sql.InfixContext
import io.getquill.context.sql.JoinContext
import io.getquill.context.sql.QueryContext
import io.getquill.context.sql.SelectValue
import io.getquill.context.sql.SetOperationSqlQuery
import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.TableContext
import io.getquill.context.sql.UnaryOperationSqlQuery
import io.getquill.context.sql.FlatJoinContext

object ExpandNestedQueries {

  private def matchName(x: FromContext): String = x match {
    case QueryContext(_, alias)   => alias
    case TableContext(_, alias)   => alias
    case InfixContext(_, alias)   => alias
    case JoinContext(_, _, _, _)  => ???
    case FlatJoinContext(_, _, _) => ???
  }

  def apply(q: SqlQuery, references: collection.Set[Property]): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        var select = q.from.flatMap {
          case JoinContext(t, a, b, on) =>
            val aName = matchName(a)
            val bName = matchName(b)
            List(SelectValue(Ident(aName)), SelectValue(Ident(bName)))
          case _ => List.empty
        }

        if (select.isEmpty) {
          select = expandSelect(q.select, references)
        }

        println("flatten sql query: " + q)
        expandNested(q.copy(select = select))
      case SetOperationSqlQuery(a, op, b) =>
        SetOperationSqlQuery(apply(a, references), op, apply(b, references))
      case UnaryOperationSqlQuery(op, q) =>
        UnaryOperationSqlQuery(op, apply(q, references))
    }

  private def expandNested(q: FlattenSqlQuery): SqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        val asts = Nil ++ where ++ groupBy ++ orderBy.map(_.ast) ++ limit ++ offset ++ select.map(_.ast)
        println("expand nested asts: " + asts)
        println("expand nested q from: " + q.from)
        val from = q.from.map(expandContext(_, asts))
        q.copy(from = from)
    }

  private def expandContext(s: FromContext, asts: List[Ast]): FromContext =
    s match {
      case QueryContext(q, alias) =>
        println("Query Context q: " + q)
        println("Query Context Alias: " + alias)
        println("Query Context Asts: " + asts)
        QueryContext(apply(q, references(alias, asts)), alias)
      case q @ JoinContext(t, a, b, on) =>
        println("EXPAND JOIN CONTEXT: " + q)

        val aName = matchName(a)
        val bName = matchName(b)
        println("A-Name: " + aName)
        println("B-Name: " + bName)

        JoinContext(t, expandContext(a, asts :+ on), expandContext(b, asts :+ on), on)
      case FlatJoinContext(t, a, on) =>
        println("EXPAND FLAT JOIN CONTEXT")
        FlatJoinContext(t, expandContext(a, asts :+ on), on)
      case _: TableContext | _: InfixContext => s
    }

  /*FlattenSqlQuery(
      List(
        JoinContext(
          InnerJoin,
          TableContext(querySchema("TestEntity"),x01),
          TableContext(querySchema("TestEntity2"),x11),
          x01.i == x11.i
        )
      ),
      Some(x1._1.o.isDefined),
      List(),
      List(),
      None,
      None,
      List(SelectValue(x1,None)),
      false
    )*/

  private def expandSelect(select: List[SelectValue], references: collection.Set[Property]) =
    references.toList match {
      case Nil => select
      case refs =>
        println("current: " + select)
        println("refs: " + refs)
        refs.map {
          case a @ Property(Property(l, tupleElem), prop) if (tupleElem.matches("_[0-9]*")) =>
            /*println("case 1")
            println("term: " + a)
            println("_x right: " + tupleElem)
            println("_x left: " + l)
            println("rest prop: " + prop)*/
            val index = tupleElem.drop(1).toInt - 1
            if (index < select.length) {
              val p = Property(select(tupleElem.drop(1).toInt - 1).ast, prop)
              SelectValue(p, Some(prop))
            } else {
              //Property(select.head.ast, prop) // TODO: This is temporary nonsense.
              //Property(select(tupleElem.drop(1).toInt - 1).ast, prop)
              //SelectValue(Ident("xmyass"), Some(prop))
              ???
            }
          case a @ Property(l, tupleElem) if (tupleElem.matches("_[0-9]*")) =>
            /*println("case 2")
            println("term: " + a)
            println("_x right: " + tupleElem)
            println("_x left: " + l)*/
            SelectValue(select(tupleElem.drop(1).toInt - 1).ast, Some(tupleElem))
          case a @ Property(l, name) =>
            /*println("case 3")
            println("term: " + a)
            println("_x right: " + name)
            println("_x left: " + l)*/
            select match {
              case List(SelectValue(i: Ident, _)) =>
                SelectValue(Property(i, name))
              case other =>
                SelectValue(Ident(name))
            }
        }
    }

  private def references(alias: String, asts: List[Ast]) = {
    println("References: " + asts)
    References(State(Ident(alias), Nil))(asts)(_.apply)._2.state.references.toSet
  }

}

case class State(ident: Ident, references: List[Property])

case class References(val state: State)
  extends StatefulTransformer[State] {

  import state._

  override def apply(a: Ast) =
    a match {
      case `reference`(p) => (p, References(State(ident, references :+ p)))
      case other          => super.apply(a)
    }

  object reference {
    def unapply(p: Property): Option[Property] =
      p match {
        case Property(`ident`, name)      => Some(p)
        case Property(reference(_), name) => Some(p)
        case other                        => None
      }
  }
}
