package com.simple.jdub

import org.joda.time.{DateTime, DateTimeZone}

import java.io.{InputStream, Reader}
import java.net.URL
import java.sql.{Blob, Clob, Date, NClob, Ref, ResultSet, SQLXML, Time, Timestamp}
import java.time.{Instant, LocalDateTime}
import java.util.UUID

/**
 * A row in a result set.
 *
 * (N.B.: All offsets are zero-based, unlike ResultSet.)
 */
class Row(rs: ResultSet) {
  /**
   * Extract the value at the given offset as an Option[String].
   */
  def string(index: Int): Option[String] = extract(rs.getString(index + 1))

  /**
   * Extract the value with the given name as an Option[String].
   */
  def string(name: String): Option[String] = extract(rs.getString(name))

  /**
   * Extract the value at the given offset as an Option[UUID].
   */
  def uuid(index: Int): Option[UUID] = string(index).map { UUID.fromString }

  /**
   * Extract the value with the given name as an Option[UUID].
   */
  def uuid(name: String): Option[UUID] = string(name).map { UUID.fromString }

  /**
   * Extract the value at the given offset as an Option[Boolean].
   */
  def boolean(index: Int): Option[Boolean] = extract(rs.getBoolean(index + 1))

  /**
   * Extract the value with the given name as an Option[Boolean].
   */
  def boolean(name: String): Option[Boolean] = extract(rs.getBoolean(name))

  /**
   * Extract the value at the given offset as an Option[Byte].
   */
  def byte(index: Int): Option[Byte] = extract(rs.getByte(index + 1))

  /**
   * Extract the value with the given name as an Option[Byte].
   */
  def byte(name: String): Option[Byte] = extract(rs.getByte(name))

  /**
   * Extract the value at the given offset as an Option[Short].
   */
  def short(index: Int): Option[Short] = extract(rs.getShort(index + 1))

  /**
   * Extract the value with the given name as an Option[Short].
   */
  def short(name: String): Option[Short] = extract(rs.getShort(name))


  /**
   * Extract the value at the given offset as an Option[Int].
   */
  def int(index: Int): Option[Int] = extract(rs.getInt(index + 1))

  /**
   * Extract the value with the given name as an Option[Int].
   */
  def int(name: String): Option[Int] = extract(rs.getInt(name))

  /**
   * Extract the value at the given offset as an Option[Long].
   */
  def long(index: Int): Option[Long] = extract(rs.getLong(index + 1))

  /**
   * Extract the value with the given name as an Option[Long].
   */
  def long(name: String): Option[Long] = extract(rs.getLong(name))

  /**
   * Extract the value at the given offset as an Option[Float].
   */
  def float(index: Int): Option[Float] = extract(rs.getFloat(index + 1))

  /**
   * Extract the value with the given name as an Option[Float].
   */
  def float(name: String): Option[Float] = extract(rs.getFloat(name))

  /**
   * Extract the value at the given offset as an Option[Double].
   */
  def double(index: Int): Option[Double] = extract(rs.getDouble(index + 1))

  /**
   * Extract the value with the given name as an Option[Double].
   */
  def double(name: String): Option[Double] = extract(rs.getDouble(name))

  /**
   * Extract the value at the given offset as an Option[BigDecimal].
   */
  def bigDecimal(index: Int): Option[BigDecimal] = extract(rs.getBigDecimal(index + 1)).map { scala.math.BigDecimal(_) }

  /**
   * Extract the value with the given name as an Option[BigDecimal].
   */
  def bigDecimal(name: String): Option[BigDecimal] = extract(rs.getBigDecimal(name)).map { scala.math.BigDecimal(_) }

  /**
   * Extract the value at the given offset as an Option[Array[Byte]].
   */
  def bytes(index: Int): Option[Array[Byte]] = extract(rs.getBytes(index + 1))

  /**
   * Extract the value with the given name as an Option[Array[Byte]].
   */
  def bytes(name: String): Option[Array[Byte]] = extract(rs.getBytes(name))

  /**
   * Extract the value at the given offset as an Option[Date].
   */
  def date(index: Int): Option[Date] = extract(rs.getDate(index + 1))

  /**
   * Extract the value with the given name as an Option[Date].
   */
  def date(name: String): Option[Date] = extract(rs.getDate(name))

  /**
   * Extract the value at the given offset as an Option[Time].
   */
  def time(index: Int): Option[Time] = extract(rs.getTime(index + 1))

  /**
   * Extract the value with the given name as an Option[Time].
   */
  def time(name: String) = extract(rs.getTime(name))

  /**
   * Extract the value at the given offset as an Option[Timestamp].
   */
  def timestamp(index: Int): Option[Timestamp] = extract(rs.getTimestamp(index + 1))

  /**
   * Extract the value with the given name as an Option[Timestamp].
   */
  def timestamp(name: String): Option[Timestamp] = extract(rs.getTimestamp(name))

  /**
    * Extract the value at the given offset as an Option[Instant].
    */
  def instant(index: Int): Option[Instant] = timestamp(index).map(_.toInstant)

  /**
    * Extract the value with the given name as an Option[Instant].
    */
  def instant(name: String): Option[Instant] = timestamp(name).map(_.toInstant)

  /**
    * Extract the value at the given offset as an Option[LocalDateTime].
    */
  def localDateTime(index: Int): Option[LocalDateTime] = timestamp(index).map(_.toLocalDateTime)

  /**
    * Extract the value with the given name as an Option[LocalDateTime].
    */
  def localDateTime(name: String): Option[LocalDateTime] = timestamp(name).map(_.toLocalDateTime)

  /**
   * Extract the value with the given name as an Option[DateTime].
   */
  def datetime(index: Int): Option[DateTime] = {
    extract(rs.getTimestamp(index + 1)).map { new DateTime(_, DateTimeZone.UTC) }
  }

  /**
   * Extract the value with the given name as an Option[DateTime].
   */
  def datetime(name: String): Option[DateTime] = {
    extract(rs.getTimestamp(name)).map { new DateTime(_, DateTimeZone.UTC) }
  }

  /**
   * Extract the value at the given offset as an Option[InputStream].
   */
  def asciiStream(index: Int): Option[InputStream] = extract(rs.getAsciiStream(index + 1))

  /**
   * Extract the value with the given name as an Option[InputStream].
   */
  def asciiStream(name: String): Option[InputStream] = extract(rs.getAsciiStream(name))

  /**
   * Extract the value at the given offset as an Option[Reader].
   */
  def characterStream(index: Int): Option[Reader] = extract(rs.getCharacterStream(index + 1))

  /**
   * Extract the value with the given name as an Option[Reader].
   */
  def characterStream(name: String): Option[Reader] = extract(rs.getCharacterStream(name))

  /**
   * Extract the value at the given offset as an Option[InputStream].
   */
  def binaryStream(index: Int): Option[InputStream] = extract(rs.getBinaryStream(index + 1))

  /**
   * Extract the value with the given name as an Option[InputStream].
   */
  def binaryStream(name: String): Option[InputStream] = extract(rs.getBinaryStream(name))

  /**
   * Extract the value at the given offset as an Option[Any].
   */
  def any(index: Int): Option[Any] = extract(rs.getObject(index + 1))

  /**
   * Extract the value with the given name as an Option[Any].
   */
  def any(name: String): Option[Any] = extract(rs.getObject(name))

  /**
   * Extract the value at the given offset as an Option[Array].
   */
  def sqlArray(index: Int): Option[java.sql.Array] = extract(rs.getArray(index + 1))

  /**
   * Extract the value with the given name as an Option[Array].
   */
  def sqlArray(name: String): Option[java.sql.Array] = extract(rs.getArray(name))

  /**
   * Extract the value at the given offset as an Option[Blob].
   */
  def blob(index: Int): Option[Blob] = extract(rs.getBlob(index + 1))

  /**
   * Extract the value with the given name as an Option[Blob].
   */
  def blob(name: String): Option[Blob] = extract(rs.getBlob(name))

  /**
   * Extract the value at the given offset as an Option[Clob].
   */
  def clob(index: Int): Option[Clob] = extract(rs.getClob(index + 1))

  /**
   * Extract the value with the given name as an Option[Clob].
   */
  def clob(name: String): Option[Clob] = extract(rs.getClob(name))

  /**
   * Extract the value at the given offset as an Option[Ref].
   */
  def ref(index: Int): Option[Ref] = extract(rs.getRef(index + 1))

  /**
   * Extract the value with the given name as an Option[Ref].
   */
  def ref(name: String): Option[Ref] = extract(rs.getRef(name))

  /**
   * Extract the value at the given offset as an Option[String].
   */
  def nString(index: Int): Option[String] = extract(rs.getNString(index + 1))

  /**
   * Extract the value with the given name as an Option[String].
   */
  def nString(name: String): Option[String] = extract(rs.getNString(name))

  /**
   * Extract the value at the given offset as an Option[Reader].
   */
  def nCharacterStream(index: Int): Option[Reader] = extract(rs.getNCharacterStream(index + 1))

  /**
   * Extract the value with the given name as an Option[Reader].
   */
  def nCharacterStream(name: String): Option[Reader] = extract(rs.getNCharacterStream(name))

  /**
   * Extract the value at the given offset as an Option[NClob].
   */
  def nClob(index: Int): Option[NClob] = extract(rs.getNClob(index + 1))

  /**
   * Extract the value with the given name as an Option[NClob].
   */
  def nClob(name: String): Option[NClob] = extract(rs.getNClob(name))

  /**
   * Extract the value at the given offset as an Option[SQLXML].
   */
  def sqlXML(index: Int): Option[SQLXML] = extract(rs.getSQLXML(index + 1))

  /**
   * Extract the value with the given name as an Option[SQLXML].
   */
  def sqlXML(name: String): Option[SQLXML] = extract(rs.getSQLXML(name))

  /**
   * Extract the value at the given offset as an Option[URL].
   */
  def url(index: Int): Option[URL] = extract(rs.getURL(index + 1))

  /**
   * Extract the value with the given name as an Option[URL].
   */
  def url(name: String): Option[URL] = extract(rs.getURL(name))

  /**
   * Transform the row into a Map[String, Any].
   */
  def toMap(): Map[String, Any] = {
    val md = rs.getMetaData()
    val colRange = 1 until (1 + md.getColumnCount)
    val colNames =  colRange.map(md.getColumnName)
    val colValues = colRange.map(rs.getObject)
    colNames.zip(colValues).toMap
  }

  private[this] def extract[A](f: A): Option[A] = if (rs.wasNull()) None else Some(f)

  def array[T: reflect.ClassTag](index: Int): Option[Array[T]] = extractArray[T](rs.getArray(index + 1))

  def array[T: reflect.ClassTag](name: String): Option[Array[T]] = extractArray[T](rs.getArray(name))

  private[this] def extractArray[T: reflect.ClassTag](sqlArray: java.sql.Array): Option[Array[T]] = {
    if (rs.wasNull()) {
      None
    } else {
      Option(sqlArray.getArray
                     .asInstanceOf[Array[Object]]
                     .map(_.asInstanceOf[T]))
    }
  }
}
