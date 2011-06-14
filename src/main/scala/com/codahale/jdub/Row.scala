package com.codahale.jdub

import java.sql.{SQLException, ResultSet}

class Row(rs: ResultSet) {
  def string(index: Int)   = extract { rs.getString(index + 1) }
  def string(name: String) = extract { rs.getString(name) }

  def boolean(index: Int)   = extract { rs.getBoolean(index + 1) }
  def boolean(name: String) = extract { rs.getBoolean(name) }

  def byte(index: Int)   = extract { rs.getByte(index + 1) }
  def byte(name: String) = extract { rs.getByte(name) }

  def short(index: Int)   = extract { rs.getShort(index + 1) }
  def short(name: String) = extract { rs.getShort(name) }

  def int(index: Int)   = extract { rs.getInt(index + 1) }
  def int(name: String) = extract { rs.getInt(name) }

  def long(index: Int)   = extract { rs.getLong(index + 1) }
  def long(name: String) = extract { rs.getLong(name) }

  def float(index: Int)   = extract { rs.getFloat(index + 1) }
  def float(name: String) = extract { rs.getFloat(name) }

  def double(index: Int)   = extract { rs.getDouble(index + 1) }
  def double(name: String) = extract { rs.getDouble(name) }

  def bigDecimal(index: Int)   = extract { rs.getBigDecimal(index + 1) }
  def bigDecimal(name: String) = extract { rs.getBigDecimal(name) }

  def bytes(index: Int)   = extract { rs.getBytes(index + 1) }
  def bytes(name: String) = extract { rs.getBytes(name) }

  def date(index: Int)   = extract { rs.getDate(index + 1) }
  def date(name: String) = extract { rs.getDate(name) }

  def time(index: Int)   = extract { rs.getTime(index + 1) }
  def time(name: String) = extract { rs.getTime(name) }

  def timestamp(index: Int)   = extract { rs.getTimestamp(index + 1) }
  def timestamp(name: String) = extract { rs.getTimestamp(name) }

  def asciiStream(index: Int)   = extract { rs.getAsciiStream(index + 1) }
  def asciiStream(name: String) = extract { rs.getAsciiStream(name) }

  def characterStream(index: Int)   = extract { rs.getCharacterStream(index + 1) }
  def characterStream(name: String) = extract { rs.getCharacterStream(name) }

  def binaryStream(index: Int)   = extract { rs.getBinaryStream(index + 1) }
  def binaryStream(name: String) = extract { rs.getBinaryStream(name) }

  def any(index: Int): Option[Any]   = extract { rs.getObject(index + 1) }
  def any(name: String): Option[Any] = extract { rs.getObject(name) }

  def sqlArray(index: Int)   = extract { rs.getArray(index + 1) }
  def sqlArray(name: String) = extract { rs.getArray(name) }

  def blob(index: Int)   = extract { rs.getBlob(index + 1) }
  def blob(name: String) = extract { rs.getBlob(name) }

  def clob(index: Int)   = extract { rs.getClob(index + 1) }
  def clob(name: String) = extract { rs.getClob(name) }

  def ref(index: Int)   = extract { rs.getRef(index + 1) }
  def ref(name: String) = extract { rs.getRef(name) }

  def nString(index: Int)   = extract { rs.getNString(index + 1) }
  def nString(name: String) = extract { rs.getNString(name) }

  def nCharacterStream(index: Int)   = extract { rs.getNCharacterStream(index + 1) }
  def nCharacterStream(name: String) = extract { rs.getNCharacterStream(name) }

  def nClob(index: Int)   = extract { rs.getNClob(index + 1) }
  def nClob(name: String) = extract { rs.getBigDecimal(name) }

  def sqlXML(index: Int)   = extract { rs.getSQLXML(index + 1) }
  def sqlXML(name: String) = extract { rs.getSQLXML(name) }

  def url(index: Int)   = extract { rs.getURL(index + 1) }
  def url(name: String) = extract { rs.getURL(name) }

  @inline
  private def extract[A](f: => A): Option[A] = try {
    val v = f
    if (rs.wasNull()) None else Some(v)
  } catch {
    case e: SQLException => {
      e.printStackTrace()
      None
    }
  }
}
