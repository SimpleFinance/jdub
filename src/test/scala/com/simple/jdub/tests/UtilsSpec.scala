package com.simple.jdub.tests

import com.simple.simplespec.Spec
import org.junit.Test
import com.simple.jdub._
import org.joda.time.{LocalDate, DateTime}
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
}
