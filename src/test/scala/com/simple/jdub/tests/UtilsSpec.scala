package com.simple.jdub.tests

import com.simple.jdub._

import org.mockito.Mockito.verify

import java.sql
import scala.math.BigDecimal

class UtilsSpec extends JdubSpec {
  test("joda - DateTime is converted properly") {
    val converted = Utils.convert(org.joda.time.DateTime.now())
    converted.isInstanceOf[java.sql.Timestamp].must(be(true))
  }

  test("joda - LocalDate is converted properly") {
    val converted = Utils.convert(org.joda.time.LocalDate.now())
    converted.isInstanceOf[java.sql.Date].must(be(true))
  }

  test("jdk - LocalDateTime is converted properly") {
    val jdkDateTime = java.time.LocalDateTime.now()
    val converted = Utils.convert(jdkDateTime)

    converted.isInstanceOf[java.sql.Timestamp].must(be(true))
    converted.asInstanceOf[java.sql.Timestamp].toLocalDateTime.mustBe(jdkDateTime)
  }

  test("jdk - LocalDate is converted properly") {
    val jdkDate = java.time.LocalDate.now()
    val converted = Utils.convert(jdkDate)

    converted.isInstanceOf[java.sql.Date].must(be(true))
    converted.asInstanceOf[java.sql.Date].toLocalDate.mustBe(jdkDate)
  }

  test("BigDecimal is converted property") {
    val converted = Utils.convert(BigDecimal("1.20"))
    converted.isInstanceOf[java.math.BigDecimal].must(be(true))
  }

  test("BigInt is converted property") {
    val converted = Utils.convert(BigInt(120))
    converted.isInstanceOf[java.math.BigDecimal].must(be(true))
  }

  test("Null is set as null") {
    val s = mock[sql.PreparedStatement]
    Utils.prepare(s, Seq(null))

    verify(s).setNull(1, sql.Types.NULL)
  }

  test("None is set as null") {
    val s = mock[sql.PreparedStatement]
    Utils.prepare(s, Seq(None))
    verify(s).setNull(1, sql.Types.NULL)
  }

  test("Non-empty Options are set as their value") {
    val s = mock[sql.PreparedStatement]
    Utils.prepare(s, Seq(Some("Ahab")))
    verify(s).setObject(1, "Ahab")
  }

  test("Empty Options are set as null") {
    val s = mock[sql.PreparedStatement]
    Utils.prepare(s, Seq(Option[String](null)))
    verify(s).setNull(1, sql.Types.NULL)
  }

}
