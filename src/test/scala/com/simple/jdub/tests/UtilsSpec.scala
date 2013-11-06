package com.simple.jdub.tests

import com.simple.simplespec.Spec
import org.junit.Test
import org.junit.Ignore
import com.simple.jdub._
import org.joda.time.{LocalDate, DateTime}
import java.sql
import scala.math.BigDecimal

class UtilsSpec extends Spec {
  class `Conversions tests` {
    @Test def `DateTime is converted properly`() {
      val converted = Utils.convert(DateTime.now())
      converted.isInstanceOf[java.sql.Timestamp].must(be(true))
    }

    @Test def `LocalDate is converted properly`() {
      val converted = Utils.convert(LocalDate.now())
      converted.isInstanceOf[java.sql.Date].must(be(true))
    }

    @Test def `BigDecimal is converted property`() {
      val converted = Utils.convert(BigDecimal("1.20"))
      converted.isInstanceOf[java.math.BigDecimal].must(be(true))
    }

    @Test def `BigInt is converted property`() {
      val converted = Utils.convert(BigInt(120))
      converted.isInstanceOf[java.math.BigDecimal].must(be(true))
    }
  }

  class `Prepare tests` {
    @Test def `Null is set as null` {
      val s = mock[sql.PreparedStatement]
      Utils.prepare(s, Seq(null))
      verify.one(s).setNull(1, sql.Types.NULL)
    }

    @Test def `None is set as null` {
      val s = mock[sql.PreparedStatement]
      Utils.prepare(s, Seq(None))
      verify.one(s).setNull(1, sql.Types.NULL)
    }

    @Test def `Non-empty Options are set as their value` {
      val s = mock[sql.PreparedStatement]
      Utils.prepare(s, Seq(Some("Ahab")))
      verify.one(s).setObject(1, "Ahab")
    }

    @Test def `Empty Options are set as null` {
      val s = mock[sql.PreparedStatement]
      Utils.prepare(s, Seq(Option[String](null)))
      verify.one(s).setNull(1, sql.Types.NULL)
    }
  }
}
