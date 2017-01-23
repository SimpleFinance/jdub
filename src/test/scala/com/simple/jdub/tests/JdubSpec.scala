/**
  * © 2016 Simple Finance Technology Corp. All rights reserved.
  */
package com.simple.jdub.tests

import com.simple.jdub.Database
import com.simple.jdub.Database.Primary
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FunSuite
import org.scalatest.GivenWhenThen
import org.scalatest.Inside
import org.scalatest.MustMatchers
import org.scalatest.OptionValues
import org.scalatest.mockito.MockitoSugar

import java.util.concurrent.atomic.AtomicInteger

object Global {
  val i = new AtomicInteger
  val db = Database.connect[Primary]("jdbc:hsqldb:mem:DbTest" + Global.i.incrementAndGet(), "sa", "")
}

trait JdubSpec extends FunSuite
    with BeforeAndAfter
    with BeforeAndAfterEach
    with GivenWhenThen
    with Inside
    with MockitoSugar
    with MustMatchers
    with OptionValues {

  Class.forName("org.hsqldb.jdbcDriver")
  val db = Global.db

  before {
    db.execute(SQL("DROP TABLE people IF EXISTS"))
    db.execute(SQL("CREATE TABLE people (name varchar(100) primary key, email varchar(100), age int)"))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Coda Hale", "chale@example.com", 29)))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Kris Gale", "kgale@example.com", 30)))
    db.execute(SQL("INSERT INTO people VALUES (?, ?, ?)", Seq("Old Guy", null, 402)))
  }
}
