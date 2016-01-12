package com.pfalabs.soak

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.Root
import org.apache.jackrabbit.oak.api.Tree
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import PropertyStates.asL
import PropertyStates.asS
import Trees.RootOps._
import Trees.TreeOps._
import Trees.typeOakUnstructured
import Sessions.RepoOpF
import Sessions.runAsAdmin
import Sessions.close
import org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants.NT_OAK_UNSTRUCTURED
import org.apache.jackrabbit.oak.api.ContentRepository

@RunWith(classOf[JUnitRunner])
class TreesSpec extends FlatSpec with Matchers {

  case class Item(name: String, choice: Option[String])

  "Tree ops" should "get children" in {
    implicit val repository = newTestRepository

    def getAllAsItem(root: Root): Iterable[Item] = (root > "/test") /:/ treeToItem

    def treeToItem(t: Tree): Item = Item(t.getName, asS(t, "choice"))

    try {
      val o = runAsAdmin(getAllAsItem)
      assert(o.isSuccess)
      val l = o.get.toList
      assert(l.length == 3)
      assert(l contains Item("t1", Some("yes")))
      assert(l contains Item("t2", Some("no")))
      assert(l contains Item("t3", None))
    } finally {
      close(repository)
    }
  }

  it should "get filtered children" in {
    implicit val repository = newTestRepository

    def getFilteredAsItem(root: Root): Iterable[Item] = (root > "/test") /:/ (filterByChoice, treeToItem)

    def filterByChoice(t: Tree) = asS(t, "choice").getOrElse("").equals("yes")

    def treeToItem(t: Tree): Item = Item(t.getName, asS(t, "choice"))

    try {
      val o = runAsAdmin(getFilteredAsItem)
      assert(o.isSuccess)
      val l = o.get.toList
      assert(l.length == 1)
      assert(l contains Item("t1", Some("yes")))
    } finally {
      close(repository)
    }
  }

  it should "get or create child" in {
    implicit val repository = newTestRepository

    def init(t: Tree) = {
      typeOakUnstructured(t)
      t |+ ("count", 0)
    }

    try {
      val oI = runAsAdmin(root => {
        (root /) /! ("testGOCC", init)
        assert(root.?*)
        root.|+>
        (root /) /! ("testGOCC", init)
        assert(!root.?*)
      })
      assert(oI.isSuccess)

      runAsAdmin(root => {
        val t = root > "/testGOCC"
        assert(asS(t, JCR_PRIMARYTYPE).getOrElse("") == NT_OAK_UNSTRUCTURED)
        assert(asL(t, "count").getOrElse(-1) == 0)
      })

    } finally {
      close(repository)
    }
  }

  def newTestRepository(): ContentRepository = {
    val repo = new Oak(new MemoryNodeStore())
      .`with`(new OpenSecurityProvider())
      .createContentRepository()
    runAsAdmin(createTestContent _)(repo)
    repo
  }

  def createTestContent(root: Root): Root = {
    val t = (root /) + "test"
    val t1 = t + "t1"
    t1 |+ ("choice", "yes")
    val t2 = t + "t2"
    t2 |+ ("choice", "no")
    t + "t3"
    root.|+>
    root
  }

}
