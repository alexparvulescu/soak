package com.pfalabs.soak

import scala.collection.JavaConverters.asJavaIterable

import scala.util.{ Failure, Success }

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.{ ContentRepository, Root, Type }
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider
import org.junit.runner.RunWith
import org.scalatest.{ Finders, FlatSpec, Matchers }
import org.scalatest.junit.JUnitRunner

import PropertyStates.{ asB, asBs, asI, asIs, asL, asLs, asS, asSs }
import Sessions.{ close, runAsAdmin }
import Trees.RootOps.toRootOps
import Trees.TreeOps.toTreeOps

@RunWith(classOf[JUnitRunner])
class PropertyStatesSpec extends FlatSpec with Matchers {

  "Property ops" should "get string value" in {
    implicit val repository = newTestRepository

    def getString(n: String)(root: Root): Option[String] =
      asS(root > "/test/" + n | "choice")

    def getStrings(n: String)(root: Root): Option[Iterable[String]] =
      asSs(root > "/test/" + n | "choice")

    try {
      runAsAdmin(getString("tString")) match {
        case Success(Some(v)) => assert(v == "yes")
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // transparent Numeric to String conversion
      runAsAdmin(getString("tNumeric")) match {
        case Success(Some(v)) => assert(v == "0")
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // transparent Large Numeric to String conversion
      runAsAdmin(getString("tNumericLarge")) match {
        case Success(Some(v)) => assert(v == "2147483657")
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // transparent Boolean to String conversion
      runAsAdmin(getString("tBoolean")) match {
        case Success(Some(v)) => assert(v == "true")
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getString("tEmpty")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(r => asS(r > "/test/tEmpty" | "choice", "defaultVal")) match {
        case Success(v)  => assert(v == "defaultVal")
        case Failure(ex) => fail(ex.getMessage)
      }

      runAsAdmin(getStrings("tStrings")) match {
        case Success(Some(v)) => assert(v.toList == List("a", "b", "c"))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getStrings("tStringsImpl")) match {
        case Success(Some(v)) => assert(v.toList == List("a", "b", "c"))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  it should "get long value" in {
    implicit val repository = newTestRepository

    def getLong(n: String)(root: Root): Option[Long] =
      asL(root > "/test/" + n | "choice")

    def getLongs(n: String)(root: Root): Option[Iterable[Long]] =
      asLs(root > "/test/" + n | "choice")

    try {

      // string->long conversion fails
      runAsAdmin(getLong("tString")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getLong("tNumeric")) match {
        case Success(Some(v)) => assert(v == 0)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getLong("tNumericLarge")) match {
        case Success(Some(v)) => assert(v == 2147483657l)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // boolean->long conversion fails
      runAsAdmin(getLong("tBoolean")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getLongs("tNumerics")) match {
        case Success(Some(v)) => assert(v.toList == List(1, 2, 3))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getLongs("tNumericsImpl")) match {
        case Success(Some(v)) => assert(v.toList == List(1, 2, 3))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  it should "get int value" in {
    implicit val repository = newTestRepository

    def getInt(n: String)(root: Root): Option[Int] =
      asI(root > "/test/" + n | "choice")

    def getInts(n: String)(root: Root): Option[Iterable[Int]] =
      asIs(root > "/test/" + n | "choice")

    try {

      // string->int conversion fails
      runAsAdmin(getInt("tString")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getInt("tNumeric")) match {
        case Success(Some(v)) => assert(v == 0)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // integer overflow
      runAsAdmin(getInt("tNumericLarge")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      // boolean->int conversion fails
      runAsAdmin(getInt("tBoolean")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getInts("tNumerics")) match {
        case Success(Some(v)) => assert(v.toList == List(1, 2, 3))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getInts("tNumericsImpl")) match {
        case Success(Some(v)) => assert(v.toList == List(1, 2, 3))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getInts("tNumericsIntImpl")) match {
        case Success(Some(v)) => assert(v.toList == List(1, 2, 3))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  it should "get boolean value" in {
    implicit val repository = newTestRepository

    def getBoolean(n: String)(root: Root): Option[Boolean] =
      asB(root > "/test/" + n | "choice")

    def getBooleans(n: String)(root: Root): Option[Iterable[Boolean]] =
      asBs(root > "/test/" + n | "choice")

    try {

      runAsAdmin(getBoolean("tString")) match {
        case Success(Some(v)) => assert(!v)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getBoolean("tNumeric")) match {
        case Success(Some(v)) => assert(!v)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getBoolean("tBoolean")) match {
        case Success(Some(v)) => assert(v)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getBooleans("tBooleans")) match {
        case Success(Some(v)) => assert(v.toList == List(true, false))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(getBooleans("tBooleansImpl")) match {
        case Success(Some(v)) => assert(v.toList == List(true, false))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  def createTestContent(root: Root): Root = {
    val t = (root /) + "test"
    val t1 = t + "tString"
    t1 |+ ("choice", "yes")
    val t2 = t + "tNumeric"
    t2 |+ ("choice", 0)
    val t3 = t + "tBoolean"
    t3 |+ ("choice", true)
    val t4 = t + "tEmpty"

    val l: java.lang.Long = Int.MaxValue + 10l
    val t5 = t + "tNumericLarge"
    t5 |+ ("choice", l, Type.LONG)

    // lists no implicits
    val t11 = t.addChild("tStrings")
    // specifically call the conversion method, otherwise things get confusing
    t11.setProperty("choice", asJavaIterable(List("a", "b", "c")), Type.STRINGS)

    val t12 = t.addChild("tNumerics")
    // specifically call the conversion method, otherwise things get confusing
    val p12: java.lang.Iterable[java.lang.Long] = asJavaIterable(List(1, 2, 3))
    t12.setProperty("choice", p12, Type.LONGS)

    val t13 = t.addChild("tBooleans")
    // specifically call the conversion method, otherwise things get confusing
    val p13: java.lang.Iterable[java.lang.Boolean] = asJavaIterable(List(true, false))
    t13.setProperty("choice", p13, Type.BOOLEANS)

    // lists with implicits
    (t + "tStringsImpl") |+ ("choice", List("a", "b", "c"))
    (t + "tNumericsImpl") |+ ("choice", List(1l, 2, 3))
    (t + "tNumericsIntImpl") |+ ("choice", List(1, 2, 3))
    (t + "tBooleansImpl") |+ ("choice", List(true, false))

    root.|+>
    root
  }

  def newTestRepository(): ContentRepository = {
    val repo = new Oak(new MemoryNodeStore())
      .`with`(new OpenSecurityProvider())
      .createContentRepository()
    runAsAdmin(createTestContent _)(repo)
    repo
  }
}
