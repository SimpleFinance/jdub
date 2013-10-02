package com.simple.jdub

import scala.annotation.tailrec
import java.sql.{Timestamp, Types, PreparedStatement}
import org.joda.time.{LocalDate, DateTime, ReadableInstant}

object Utils {
  private[jdub] def prependComment(obj: Object, sql: String) =
    "/* %s */ %s".format(obj.getClass.getSimpleName.replace("$", ""), sql)

  @tailrec
  private[jdub] def prepare(stmt: PreparedStatement, values: Seq[Any], index: Int = 1) {
    if (!values.isEmpty) {
      val v = values.head
      if (v == null || v.isInstanceOf[None$]) {
        stmt.setNull(index, Types.NULL)
      } else {
        stmt.setObject(index, convert(v.asInstanceOf[AnyRef]))
      }
      prepare(stmt, values.tail, index + 1)
    }
  }

  /**
   * Convert to JDBC-compatible types
   */
  private[jdub] def convert(x: AnyRef): AnyRef = x match {
    case num: BigDecimal => num.underlying()
    case num: BigInt => BigDecimal(num).underlying()

    // Convert Joda times to UTC.
    case ts: ReadableInstant => new Timestamp(new DateTime(ts.getMillis, ts.getZone).toDateTimeISO.getMillis)
    case d: LocalDate => new java.sql.Date(d.toDate.getTime)
    // Pass everything else through.
    case _ => x
  }
}
