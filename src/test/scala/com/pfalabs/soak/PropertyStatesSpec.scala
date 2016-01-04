package com.pfalabs.soak

import scala.collection.JavaConversions.asJavaIterable
import scala.util.{ Failure, Success }

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.{ Root, Type }
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider
import org.junit.runner.RunWith
import org.scalatest.{ Finders, FlatSpec, Matchers }
import org.scalatest.junit.JUnitRunner

import com.pfalabs.soak.Sessions.RepoOpF

import PropertyStates.{ asB, asI, asIs, asL, asLs, asS, asSs }
import Sessions.{ RepoOpF, close, runAsAdmin }

@RunWith(classOf[JUnitRunner])
class PropertyStatesSpec extends FlatSpec with Matchers {

  "Property ops" should "get string value" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    def getString(n: String)(root: Root) = asS(root.getTree("/test/" + n), "choice")

    def ops(n: String): RepoOpF[Option[String]] = createTestContent _ andThen getString(n) _

    try {
      runAsAdmin(ops("tString")) match {
        case Success(Some(v)) => assert(v == "yes")
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // transparent Numeric to String conversion
      runAsAdmin(ops("tNumeric")) match {
        case Success(Some(v)) => assert(v == "0")
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // transparent Large Numeric to String conversion
      runAsAdmin(ops("tNumericLarge")) match {
        case Success(Some(v)) => assert(v == "2147483657")
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // transparent Boolean to String conversion
      runAsAdmin(ops("tBoolean")) match {
        case Success(Some(v)) => assert(v == "true")
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(ops("tEmpty")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(r => asS(r.getTree("/test/tEmpty"), "choice", "defaultVal")) match {
        case Success(v)  => assert(v == "defaultVal")
        case Failure(ex) => fail(ex.getMessage)
      }

      runAsAdmin(r => asSs(r.getTree("/test/tStrings"), "choice")) match {
        case Success(Some(v)) => assert(v.toList == List("a", "b", "c"))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  it should "get long value" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    def getLong(n: String)(root: Root) = asL(root.getTree("/test/" + n), "choice")

    def ops(n: String): RepoOpF[Option[Long]] = createTestContent _ andThen getLong(n) _

    try {

      // string->long conversion fails
      runAsAdmin(ops("tString")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(ops("tNumeric")) match {
        case Success(Some(v)) => assert(v == 0)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(ops("tNumericLarge")) match {
        case Success(Some(v)) => assert(v == 2147483657l)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // boolean->long conversion fails
      runAsAdmin(ops("tBoolean")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(r => asLs(r.getTree("/test/tNumerics"), "choice")) match {
        case Success(Some(v)) => assert(v.toList == List(1, 2, 3))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  it should "get int value" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    def getInt(n: String)(root: Root) = asI(root.getTree("/test/" + n), "choice")

    def ops(n: String): RepoOpF[Option[Int]] = createTestContent _ andThen getInt(n) _

    try {

      // string->int conversion fails
      runAsAdmin(ops("tString")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(ops("tNumeric")) match {
        case Success(Some(v)) => assert(v == 0)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      // integer overflow
      runAsAdmin(ops("tNumericLarge")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      // boolean->int conversion fails
      runAsAdmin(ops("tBoolean")) match {
        case Success(Some(v)) => fail(s"unexpected value $v")
        case Success(None)    => // expected
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(r => asIs(r.getTree("/test/tNumerics"), "choice")) match {
        case Success(Some(v)) => assert(v.toList == List(1, 2, 3))
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  it should "get boolean value" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    def getBoolean(n: String)(root: Root) = asB(root.getTree("/test/" + n), "choice")

    def ops(n: String): RepoOpF[Option[Boolean]] = createTestContent _ andThen getBoolean(n) _

    try {

      runAsAdmin(ops("tString")) match {
        case Success(Some(v)) => assert(!v)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(ops("tNumeric")) match {
        case Success(Some(v)) => assert(!v)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(ops("tBoolean")) match {
        case Success(Some(v)) => assert(v)
        case Success(None)    => fail("get property failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  def createTestContent(root: Root): Root = {
    val t = root.getTree("/").addChild("test")
    val t1 = t.addChild("tString")
    t1.setProperty("choice", "yes")
    val t2 = t.addChild("tNumeric")
    t2.setProperty("choice", 0)
    val t3 = t.addChild("tBoolean")
    t3.setProperty("choice", true)
    val t4 = t.addChild("tEmpty")

    val l: java.lang.Long = Int.MaxValue + 10l
    val t5 = t.addChild("tNumericLarge")
    t5.setProperty("choice", l, Type.LONG)

    val t11 = t.addChild("tStrings")
    // specifically call the conversion method, otherwise things get confusing
    t11.setProperty("choice", asJavaIterable(List("a", "b", "c")), Type.STRINGS)

    val t12 = t.addChild("tNumerics")
    // specifically call the conversion method, otherwise things get confusing
    val p12: java.lang.Iterable[java.lang.Long] = asJavaIterable(List(1, 2, 3))
    t12.setProperty("choice", p12, Type.LONGS)

    root.commit()
    root
  }

}
