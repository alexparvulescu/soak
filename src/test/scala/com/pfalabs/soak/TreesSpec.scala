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

import Trees.getChildren
import Trees.typeOakUnstructured
import Trees.getOrCreate
import Trees.TreeOpsFilter

import Sessions.RepoOpF
import Sessions.runAsAdmin
import Sessions.close

import org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants.NT_OAK_UNSTRUCTURED

@RunWith(classOf[JUnitRunner])
class TreesSpec extends FlatSpec with Matchers {

  case class Item(name: String, choice: Option[String])

  "Tree ops" should "get children" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    def getAllChildren(root: Root) = getChildren(root.getTree("/test"), treeToItem)

    val ops: RepoOpF[List[Item]] = createTestContent _ andThen getAllChildren _

    try {
      val o = runAsAdmin(ops)
      assert(o.isSuccess)
      val l = o.get
      assert(l.length == 3)
      assert(l contains Item("t1", Some("yes")))
      assert(l contains Item("t2", Some("no")))
      assert(l contains Item("t3", None))
    } finally {
      close(repository)
    }
  }

  it should "get filtered children" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    def getFilteredChildren(root: Root) = getChildren(root.getTree("/test"), treeToItem, filterByChoice)

    def filterByChoice(t: Tree) = asS(t, "choice").getOrElse("").equals("yes")

    def getFilteredChildrenTest(root: Root) = {
      val t = root.getTree("/test")
      getChildren(t, treeToItem, filterByChoice)
    }

    val ops: RepoOpF[List[Item]] = createTestContent _ andThen getFilteredChildren _
    try {
      val o = runAsAdmin(ops)
      assert(o.isSuccess)
      val l = o.get
      assert(l.length == 1)
      assert(l contains Item("t1", Some("yes")))
    } finally {
      close(repository)
    }
  }

  it should "get or create child" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    def init(t: Tree) = {
      typeOakUnstructured(t)
      t.setProperty("count", 0)
    }

    try {

      val oI = runAsAdmin(root => {
        getOrCreate(root.getTree("/"), "testGOCC", init)
        assert(root.hasPendingChanges())
        root.commit()
      })
      assert(oI.isSuccess)

      runAsAdmin(root => {
        val t = getOrCreate(root.getTree("/"), "testGOCC", init)
        assert(!root.hasPendingChanges())
        assert(asS(t, JCR_PRIMARYTYPE).getOrElse("") == NT_OAK_UNSTRUCTURED)
        assert(asL(t, "count").getOrElse(-1) == 0)
      })

    } finally {
      close(repository)
    }
  }

  def createTestContent(root: Root): Root = {
    val t = root.getTree("/").addChild("test")
    val t1 = t.addChild("t1")
    t1.setProperty("choice", "yes")
    val t2 = t.addChild("t2")
    t2.setProperty("choice", "no")
    val t3 = t.addChild("t3")
    root.commit()
    root
  }

  def treeToItem(t: Tree): Item = Item(t.getName, asS(t, "choice"))

}
