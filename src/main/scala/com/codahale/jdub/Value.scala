package com.codahale.jdub

import java.sql.ResultSet

/**
  * A value in a result set which cannot be `NULL`. Convertible to other types.
  * Will throw an exception if a `NULL` value is coerced into a type. Use
  * `nullable` for that.
  */
class Value(rs: ResultSet, index: Int) {
  private val twin = new NullableValue(rs, index)

  /**
    * Returns a nullable version of this value.
    *
    * @see com.codahale.jdub.NullableValue
    */
  def nullable = twin

  def toUtf8String = asValue(rs.getString(index))
  def toBoolean = asValue(rs.getBoolean(index))
  def toByte = asValue(rs.getByte(index))
  def toShort = asValue(rs.getShort(index))
  def toInt = asValue(rs.getInt(index))
  def toLong = asValue(rs.getLong(index))
  def toFloat = asValue(rs.getFloat(index))
  def toDouble = asValue(rs.getDouble(index))
  def toBigDecimal = asValue(rs.getBigDecimal(index))
  def toByteArray = asValue(rs.getBytes(index))
  def toDate = asValue(rs.getDate(index))
  def toTime = asValue(rs.getTime(index))
  def toTimestamp = asValue(rs.getTimestamp(index))
  def toAsciiStream = asValue(rs.getAsciiStream(index))
  def toCharacterStream = asValue(rs.getCharacterStream(index))
  def toBinaryStream = asValue(rs.getBinaryStream(index))
  def toObject = asValue(rs.getObject(index))
  def toSqlArray = asValue(rs.getArray(index))
  def toBlob = asValue(rs.getBlob(index))
  def toClob = asValue(rs.getClob(index))
  def toRef = asValue(rs.getRef(index))
  def toNString = asValue(rs.getNString(index))
  def toNCharacterStream = asValue(rs.getCharacterStream(index))
  def toNClob = asValue(rs.getNClob(index))
  def toSQLXML = asValue(rs.getSQLXML(index))
  def toURL = asValue(rs.getURL(index))

  private def asValue[T](v: T)(implicit mf: Manifest[T]) = if (rs.wasNull()) {
    throw new NoSuchElementException("Cannot coerce NULL directly to " + mf.erasure.getSimpleName)
  } else v
}

/**
  * A value in a result set which may or may not be `NULL`. Convertible to
  * options of other types.
  */
class NullableValue(rs: ResultSet, index: Int) {
  def toUtf8String = asOption(rs.getString(index))
  def toBoolean = asOption(rs.getBoolean(index))
  def toByte = asOption(rs.getByte(index))
  def toShort = asOption(rs.getShort(index))
  def toInt = asOption(rs.getInt(index))
  def toLong = asOption(rs.getLong(index))
  def toFloat = asOption(rs.getFloat(index))
  def toDouble = asOption(rs.getDouble(index))
  def toBigDecimal = asOption(rs.getBigDecimal(index))
  def toByteArray = asOption(rs.getBytes(index))
  def toDate = asOption(rs.getDate(index))
  def toTime = asOption(rs.getTime(index))
  def toTimestamp = asOption(rs.getTimestamp(index))
  def toAsciiStream = asOption(rs.getAsciiStream(index))
  def toCharacterStream = asOption(rs.getCharacterStream(index))
  def toBinaryStream = asOption(rs.getBinaryStream(index))
  def toObject = asOption(rs.getObject(index))
  def toSqlArray = asOption(rs.getArray(index))
  def toBlob = asOption(rs.getBlob(index))
  def toClob = asOption(rs.getClob(index))
  def toRef = asOption(rs.getRef(index))
  def toNString = asOption(rs.getNString(index))
  def toNCharacterStream = asOption(rs.getCharacterStream(index))
  def toNClob = asOption(rs.getNClob(index))
  def toSQLXML = asOption(rs.getSQLXML(index))
  def toURL = asOption(rs.getURL(index))
  
  private def asOption[T](v: T) = if (rs.wasNull()) None else Some(v)
}
