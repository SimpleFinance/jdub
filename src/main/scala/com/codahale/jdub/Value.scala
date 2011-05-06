package com.codahale.jdub

import java.sql.ResultSet

/**
 * A value in a result set. Convertible to other types.
 */
class Value(rs: ResultSet, index: Int) {
  override def toString = rs.getString(index)
  def toBoolean = rs.getBoolean(index)
  def toByte = rs.getByte(index)
  def toShort = rs.getShort(index)
  def toInt = rs.getInt(index)
  def toLong = rs.getLong(index)
  def toFloat = rs.getFloat(index)
  def toDouble = rs.getDouble(index)
  def toBigDecimal = rs.getBigDecimal(index)
  def toByteArray = rs.getBytes(index)
  def toDate = rs.getDate(index)
  def toTime = rs.getTime(index)
  def toTimestamp = rs.getTimestamp(index)
  def toAsciiStream = rs.getAsciiStream(index)
  def toCharacterStream = rs.getCharacterStream(index)
  def toBinaryStream = rs.getBinaryStream(index)
  def toObject = rs.getObject(index)
  def toSqlArray = rs.getArray(index)
  def toBlob = rs.getBlob(index)
  def toClob = rs.getClob(index)
  def toRef = rs.getRef(index)
  def toNString = rs.getNString(index)
  def toNCharacterStream = rs.getCharacterStream(index)
  def toNClob = rs.getNClob(index)
  def toSQLXML = rs.getSQLXML(index)
  def toURL = rs.getURL(index)
}
