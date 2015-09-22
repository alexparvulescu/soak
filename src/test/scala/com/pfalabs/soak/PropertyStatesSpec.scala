package com.pfalabs.soak

import scala.util.Failure
import scala.util.Success

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.Root
import org.apache.jackrabbit.oak.api.Type
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider
import org.junit.runner.RunWith
import org.scalatest.Finders
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import com.pfalabs.soak.Sessions.RepoOpF

import PropertyStates.asB
import PropertyStates.asI
import PropertyStates.asL
import PropertyStates.asS
import Sessions.RepoOpF
import Sessions.close
import Sessions.runAsAdmin

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

    root.commit()
    root
  }

}
