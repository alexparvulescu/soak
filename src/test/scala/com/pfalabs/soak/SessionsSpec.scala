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

import Sessions.RepoOpF
import Sessions.close
import Sessions.runAsAdmin

@RunWith(classOf[JUnitRunner])
class SessionsSpec extends FlatSpec with Matchers {

  "Session ops" should "create content" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    val o1 = runAsAdmin({ root =>
      root.getTree("/").addChild("test")
      root.commit()
    })
    assert(o1.isSuccess)

    val o2 = runAsAdmin({ root =>
      val t = root.getTree("/").getChild("test")
      assert(t.exists())
    })
    assert(o2.isSuccess)

    val o3 = Sessions.run("none", "")({ root =>
      fail("not allowed here!")
    })
    assert(o3.isFailure)

    close(repository)
  }

  it should "compose" in {
    val oak = new Oak(new MemoryNodeStore()).`with`(new OpenSecurityProvider())
    implicit val repository = oak.createContentRepository()

    def getTest(r: Root): Tree = {
      r.getTree("/").addChild("test")
    }

    def getContent(t: Tree): Tree = {
      t.addChild("content")
    }

    val chainOpsRoot: RepoOpF[Tree] = getTest _ andThen getContent _

    val o1 = runAsAdmin({ root =>
      chainOpsRoot(root)
      assert(root.hasPendingChanges())
      root.commit()
    })
    assert(o1.isSuccess)

    val o2 = runAsAdmin({ root =>
      chainOpsRoot(root)
      assert(!root.hasPendingChanges())
    })
    assert(o2.isSuccess)

    def createContent(tIn: Tree) = {
      val t = tIn.addChild("t2")
      t.setProperty("ts", System.currentTimeMillis())
    }

    val chainOps: RepoOpF[Unit] = chainOpsRoot andThen createContent _

    val o3 = runAsAdmin({ root =>
      chainOps(root)
      assert(root.hasPendingChanges())
      root.commit()
    })
    assert(o3.isSuccess)

    // println(runAsAdmin(Trees.mkString).get)
    close(repository)
  }
}
