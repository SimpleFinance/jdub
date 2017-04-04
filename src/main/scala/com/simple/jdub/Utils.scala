package com.simple.jdub

import java.sql.PreparedStatement
import java.sql.Types
import scala.annotation.tailrec

object Utils {
  private[jdub] def prependComment(obj: Object, sql: String) =
    "/* %s */ %s".format(obj.getClass.getSimpleName.replace("$", ""), sql)

  @tailrec
  private[jdub] def prepare(stmt: PreparedStatement, values: Seq[Any], index: Int = 1) {
    if (values.nonEmpty) {
      values.head match {
        case v if v == null =>
          stmt.setNull(index, Types.NULL)

        case ov: Option[_] if ov.isDefined =>
          stmt.setObject(index, convert(ov.get.asInstanceOf[AnyRef]))

        case ov: Option[_] if ov.isEmpty => stmt.setNull(index, Types.NULL)

        case v => stmt.setObject(index, convert(v.asInstanceOf[AnyRef]))
      }
      prepare(stmt, values.tail, index + 1)
    }
  }

  /**
    * Convert to JDBC-compatible types
    */
  private[jdub] def convert(x: AnyRef): AnyRef = {
    x match {
      case num: BigDecimal => num.underlying()
      case num: BigInt => BigDecimal(num).underlying()

      // Convert Joda times to UTC.
      case ts: org.joda.time.ReadableInstant =>
        new java.sql.Timestamp(new org.joda.time.DateTime(ts.getMillis, ts.getZone).toDateTimeISO.getMillis)
      case d: org.joda.time.LocalDate => new java.sql.Date(d.toDate.getTime)

      // Convert JDK8 date/times
      case d: java.time.LocalDate => java.sql.Date.valueOf(d)
      case dt: java.time.LocalDateTime => java.sql.Timestamp.valueOf(dt)

      // Pass everything else through.
      case _ => x
    }
  }
}
