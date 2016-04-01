/**
 * © 2016 Simple Finance Technology Corp. All rights reserved.
 */

package com.simple.jdub

object Interpolator {
  case class Statements private[jdub] (
    sql: String,
    values: Seq[Any]
  ) extends Statement

  case class CollectionQueries[T] private[jdub] (
    sql: String,
    values: Seq[Any],
    f: Row => T
  ) extends CollectionQuery[Seq, T] {
    def map(row: Row) = f(row)
  }

  case class FlatCollectionQueries[T] private[jdub] (
    sql: String,
    values: Seq[Any],
    f: Row => Option[T]
  ) extends FlatCollectionQuery[Seq, T] {
    def flatMap(row: Row) = f(row)
  }

  case class Queries[T] private[jdub] (
    sql: String,
    values: Seq[Any],
    initial: T,
    f: (T, Row) => T
  ) extends Query[T] {
    def reduce(rows: Iterator[Row]): T = rows.foldLeft(initial)(f)
  }

  implicit class SqlStringContext(val query: StringContext) extends AnyVal {
    def sql(params: Any*) = Statements(
      sql = expand(params, query.parts),
      values = pancake(params)
    )
  }

  implicit class SqlStatement(val statement: Statement) extends AnyVal {
    def map[T](f: Row => T) = CollectionQueries[T](
      sql = statement.sql,
      values = statement.values,
      f = f
    )

    def flatMap[T](f: Row => Option[T]) = FlatCollectionQueries[T](
      sql = statement.sql,
      values = statement.values,
      f = f
    )

    def foldLeft[T](initial: T)(f: (T, Row) => T) = Queries[T](
      sql = statement.sql,
      values = statement.values,
      initial = initial,
      f = f
    )
  }

  private def expand(params: Seq[Any], parts: Seq[String]): String = {
    val builder = new StringBuilder
    builder.append(parts.head)
    params.zip(parts.tail).map {
      case (param, part) =>
        builder.append(("?" * size(param)).mkString(", "))
        builder.append(part)
    }
    builder.toString
  }

  private def size(param: Any): Int = param match {
    case seq: Seq[_] => seq.length
    case _ => 1
  }

  private def pancake(params: Seq[Any]): Seq[Any] = params.flatMap {
    case seq: Seq[_] => seq // only support lists nested one deep
    case param => Seq(param)
  }
}
