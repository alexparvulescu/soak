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
import Sessions.{ close, runAsAdmin }
import Trees.RootOps.toRootOps
import Trees.TreeOps.toTreeOps
import PropertyStatesTT._
import PropertyStates._
import org.apache.jackrabbit.oak.api.PropertyState

@RunWith(classOf[JUnitRunner])
class PropertyStatesTTSpec extends FlatSpec with Matchers {

  "Property ops" should "get string value" in {
    implicit val repository = newTestRepository

    def getString(n: String)(root: Root): StringPS =
      ttS(root > "/test/" + n | "choice")

    def extract(v: StringPS): String = asS(v).getOrElse("")

    try {
      runAsAdmin(getString("tString")) match {
        case Success(v)  => assert(extract(v) == "yes")
        case Failure(ex) => fail(ex.getMessage)
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
