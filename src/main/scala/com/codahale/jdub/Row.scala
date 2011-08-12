package com.codahale.jdub

import java.sql.ResultSet

/**
 * A row in a result set.
 *
 * (N.B.: All offsets are zero-based, unlike ResultSet.)
 */
class Row(rs: ResultSet) {
  /**
   * Extract the value at the given offset as an Option[String].
   */
  def string(index: Int) = extract(rs.getString(index + 1))

  /**
   * Extract the value with the given name as an Option[String].
   */
  def string(name: String) = extract(rs.getString(name))

  /**
   * Extract the value at the given offset as an Option[Boolean].
   */
  def boolean(index: Int) = extract(rs.getBoolean(index + 1))

  /**
   * Extract the value with the given name as an Option[Boolean].
   */
  def boolean(name: String) = extract(rs.getBoolean(name))

  /**
   * Extract the value at the given offset as an Option[Byte].
   */
  def byte(index: Int) = extract(rs.getByte(index + 1))

  /**
   * Extract the value with the given name as an Option[Byte].
   */
  def byte(name: String) = extract(rs.getByte(name))

  /**
   * Extract the value at the given offset as an Option[Short].
   */
  def short(index: Int) = extract(rs.getShort(index + 1))

  /**
   * Extract the value with the given name as an Option[Short].
   */
  def short(name: String) = extract(rs.getShort(name))


  /**
   * Extract the value at the given offset as an Option[Int].
   */
  def int(index: Int) = extract(rs.getInt(index + 1))

  /**
   * Extract the value with the given name as an Option[Int].
   */
  def int(name: String) = extract(rs.getInt(name))

  /**
   * Extract the value at the given offset as an Option[Long].
   */
  def long(index: Int) = extract(rs.getLong(index + 1))

  /**
   * Extract the value with the given name as an Option[Long].
   */
  def long(name: String) = extract(rs.getLong(name))

  /**
   * Extract the value at the given offset as an Option[Float].
   */
  def float(index: Int) = extract(rs.getFloat(index + 1))

  /**
   * Extract the value with the given name as an Option[Float].
   */
  def float(name: String) = extract(rs.getFloat(name))

  /**
   * Extract the value at the given offset as an Option[Double].
   */
  def double(index: Int) = extract(rs.getDouble(index + 1))

  /**
   * Extract the value with the given name as an Option[Double].
   */
  def double(name: String) = extract(rs.getDouble(name))

  /**
   * Extract the value at the given offset as an Option[BigDecimal].
   */
  def bigDecimal(index: Int) = extract(rs.getBigDecimal(index + 1))

  /**
   * Extract the value with the given name as an Option[BigDecimal].
   */
  def bigDecimal(name: String) = extract(rs.getBigDecimal(name))

  /**
   * Extract the value at the given offset as an Option[Array[Byte]].
   */
  def bytes(index: Int) = extract(rs.getBytes(index + 1))

  /**
   * Extract the value with the given name as an Option[Array[Byte]].
   */
  def bytes(name: String) = extract(rs.getBytes(name))

  /**
   * Extract the value at the given offset as an Option[Date].
   */
  def date(index: Int) = extract(rs.getDate(index + 1))

  /**
   * Extract the value with the given name as an Option[Date].
   */
  def date(name: String) = extract(rs.getDate(name))

  /**
   * Extract the value at the given offset as an Option[Time].
   */
  def time(index: Int) = extract(rs.getTime(index + 1))

  /**
   * Extract the value with the given name as an Option[Time].
   */
  def time(name: String) = extract(rs.getTime(name))

  /**
   * Extract the value at the given offset as an Option[Timestamp].
   */
  def timestamp(index: Int) = extract(rs.getTimestamp(index + 1))

  /**
   * Extract the value with the given name as an Option[Timestamp].
   */
  def timestamp(name: String) = extract(rs.getTimestamp(name))

  /**
   * Extract the value at the given offset as an Option[InputStream].
   */
  def asciiStream(index: Int) = extract(rs.getAsciiStream(index + 1))

  /**
   * Extract the value with the given name as an Option[InputStream].
   */
  def asciiStream(name: String) = extract(rs.getAsciiStream(name))

  /**
   * Extract the value at the given offset as an Option[InputStream].
   */
  def characterStream(index: Int) = extract(rs.getCharacterStream(index + 1))

  /**
   * Extract the value with the given name as an Option[InputStream].
   */
  def characterStream(name: String) = extract(rs.getCharacterStream(name))

  /**
   * Extract the value at the given offset as an Option[InputStream].
   */
  def binaryStream(index: Int) = extract(rs.getBinaryStream(index + 1))

  /**
   * Extract the value with the given name as an Option[InputStream].
   */
  def binaryStream(name: String) = extract(rs.getBinaryStream(name))

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
  def sqlArray(index: Int) = extract(rs.getArray(index + 1))

  /**
   * Extract the value with the given name as an Option[Array].
   */
  def sqlArray(name: String) = extract(rs.getArray(name))

  /**
   * Extract the value at the given offset as an Option[Blob].
   */
  def blob(index: Int) = extract(rs.getBlob(index + 1))

  /**
   * Extract the value with the given name as an Option[Blob].
   */
  def blob(name: String) = extract(rs.getBlob(name))

  /**
   * Extract the value at the given offset as an Option[Clob].
   */
  def clob(index: Int) = extract(rs.getClob(index + 1))

  /**
   * Extract the value with the given name as an Option[Clob].
   */
  def clob(name: String) = extract(rs.getClob(name))

  /**
   * Extract the value at the given offset as an Option[Ref].
   */
  def ref(index: Int) = extract(rs.getRef(index + 1))

  /**
   * Extract the value with the given name as an Option[Ref].
   */
  def ref(name: String) = extract(rs.getRef(name))

  /**
   * Extract the value at the given offset as an Option[String].
   */
  def nString(index: Int) = extract(rs.getNString(index + 1))

  /**
   * Extract the value with the given name as an Option[String].
   */
  def nString(name: String) = extract(rs.getNString(name))

  /**
   * Extract the value at the given offset as an Option[Reader].
   */
  def nCharacterStream(index: Int) = extract(rs.getNCharacterStream(index + 1))

  /**
   * Extract the value with the given name as an Option[Reader].
   */
  def nCharacterStream(name: String) = extract(rs.getNCharacterStream(name))

  /**
   * Extract the value at the given offset as an Option[NClob].
   */
  def nClob(index: Int) = extract(rs.getNClob(index + 1))

  /**
   * Extract the value with the given name as an Option[NClob].
   */
  def nClob(name: String) = extract(rs.getNClob(name))

  /**
   * Extract the value at the given offset as an Option[SQLXML].
   */
  def sqlXML(index: Int) = extract(rs.getSQLXML(index + 1))

  /**
   * Extract the value with the given name as an Option[SQLXML].
   */
  def sqlXML(name: String) = extract(rs.getSQLXML(name))

  /**
   * Extract the value at the given offset as an Option[URL].
   */
  def url(index: Int) = extract(rs.getURL(index + 1))

  /**
   * Extract the value with the given name as an Option[URL].
   */
  def url(name: String) = extract(rs.getURL(name))

  private def extract[A](f: A): Option[A] = if (rs.wasNull()) None else Some(f)
}
