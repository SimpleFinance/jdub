package com.codahale.jdub

import java.sql.ResultSet

class Cell(rs: ResultSet, i: Int) {
  override def toString = rs.getString(i)

  def toByte = rs.getByte(i)

  def toShort = rs.getShort(i)

  def toInt = rs.getInt(i)

  def toLong = rs.getLong(i)

  def toFloat = rs.getFloat(i)

  def toDouble = rs.getDouble(i)

  def toBigDecimal = rs.getBigDecimal(i)

  def toByteArray = rs.getBytes(i)

  def toDate = rs.getDate(i)

  def toTime = rs.getTime(i)

  def toTimestamp = rs.getTimestamp(i)

  def toAsciiStream = rs.getAsciiStream(i)

  def toCharacterStream = rs.getCharacterStream(i)

  def toBinaryStream = rs.getBinaryStream(i)

  def toObject = rs.getObject(i)

  def toSqlArray = rs.getArray(i)

  def toBlob = rs.getBlob(i)

  def toClob = rs.getClob(i)

  def toRef = rs.getRef(i)

  def toNString = rs.getNString(i)

  def toNCharacterStream = rs.getCharacterStream(i)

  def toNClob = rs.getNClob(i)

  def toSQLXML = rs.getSQLXML(i)

  def toURL = rs.getURL(i)
}
