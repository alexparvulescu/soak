package com.pfalabs.soak

import scala.util.{ Failure, Success }

import org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.{ ContentRepository, Root, Tree }
import org.apache.jackrabbit.oak.api.Type.NAME
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants.NT_OAK_UNSTRUCTURED
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider
import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.junit.JUnitRunner

import PropertyStates.{ asI, asS }
import Queries.{ xpath, xpathUnique }
import Sessions.{ close, runAsAdmin }
import Trees.RootOps.toRootOps
import Trees.TreeOps.toTreeOps

@RunWith(classOf[JUnitRunner])
class QueriesSpec extends FlatSpec with Matchers {

  case class Item(name: String, id: Int, lang: Option[String])

  "Query ops" should "query by property" in {
    implicit val repository = newTestRepository

    def treeToItem(t: Tree): Item = Item(t.name, asI(t | "id").get, asS(t | "lang"))

    val xpathEn = "/jcr:root/testQ/element(*, oak:Unstructured)[@lang = 'en']"

    val xpathEn1 = "/jcr:root/testQ/element(*, oak:Unstructured)[@id = 1]"

    try {
      val o1 = runAsAdmin(xpath(xpathEn, treeToItem))
      assert(o1.isSuccess)
      val l1 = o1.get.toList
      assert(l1.length == 2)
      assert(l1 contains Item("t1en", 1, Some("en")))
      assert(l1 contains Item("t2en", 3, Some("en")))

      runAsAdmin(xpathUnique(xpathEn1)) match {
        case Success(Some(v)) => assert(v == "/testQ/t1en")
        case Success(None)    => fail("query failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

      runAsAdmin(xpathUnique(xpathEn1, treeToItem)) match {
        case Success(Some(v)) => assert(v == Item("t1en", 1, Some("en")))
        case Success(None)    => fail("query failed")
        case Failure(ex)      => fail(ex.getMessage)
      }

    } finally {
      close(repository)
    }
  }

  def newTestRepository(): ContentRepository = {
    val repo = new Oak(new MemoryNodeStore())
      .`with`(new OpenSecurityProvider())
      .`with`(new InitialContent())
      .createContentRepository()
    runAsAdmin(createTestContent _)(repo)
    repo
  }

  def createTestContent(root: Root): Root = {
    val t = (root /) + "testQ"
    (t + "t1en") |+ ("id", 1) |+ ("lang", "en") |+ (JCR_PRIMARYTYPE, NT_OAK_UNSTRUCTURED, NAME)
    (t + "t1fr") |+ ("id", 2) |+ ("lang", "fr") |+ (JCR_PRIMARYTYPE, NT_OAK_UNSTRUCTURED, NAME)
    (t + "t2en") |+ ("id", 3) |+ ("lang", "en") |+ (JCR_PRIMARYTYPE, NT_OAK_UNSTRUCTURED, NAME)
    (t + "t2fr") |+ ("id", 4) |+ ("lang", "fr") |+ (JCR_PRIMARYTYPE, NT_OAK_UNSTRUCTURED, NAME)
    root.|+>
    root
  }

}
