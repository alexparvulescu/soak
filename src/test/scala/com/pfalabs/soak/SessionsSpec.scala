package com.pfalabs.soak

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

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
  }

}