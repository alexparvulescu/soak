package com.pfalabs.soak

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.util.Properties

import org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import org.apache.jackrabbit.oak.api.Root
import org.apache.jackrabbit.oak.api.Tree
import org.apache.jackrabbit.oak.api.Type.NAME
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants.NT_OAK_UNSTRUCTURED

object Trees {

  // ----------------------------------------------------
  // ToString Helpers
  // ----------------------------------------------------

  def mkString(r: Root): String = mkString(r.getTree("/"))

  def mkString(t: Tree): String =
    if (t == null || !t.exists()) {
      "[]"
    } else {
      mkString(t, 1, "  ", "/")
    }

  private def mkString(t: Tree, level: Int, prepend: String,
                       name: String): String = {
    val node = new StringBuilder()
    node.append(prepend).append(name)
    if (t.getPropertyCount > 0) {
      node.append(t.getProperties().mkString("{", ", ", "}"))
    }
    t.getChildren.foreach { tc =>
      {
        node.append(Properties.lineSeparator)
        node.append(mkString(tc, level + 1, prepend + prepend,
          tc.getName()));
      }
    }
    node.toString()
  }

  // ----------------------------------------------------
  // Children Ops
  // ----------------------------------------------------

  type TreeOps[U] = (Tree) => U

  type TreeOpsFilter = TreeOps[Boolean]

  private def includeAll: TreeOpsFilter = (x: Tree) => true

  def getChildren[U](t: Tree, m: TreeOps[U], f: TreeOpsFilter = includeAll): List[U] = {
    t.getChildren()
      .filter(f)
      .map(m)
      .toList
  }

  def getOrCreate(t: Tree, name: String, init: TreeOps[Unit]) =
    if (t.hasChild(name)) {
      t.getChild(name)
    } else {
      val c = t.addChild(name)
      init(c)
      c
    }

  // ----------------------------------------------------
  // Node Types
  // ----------------------------------------------------

  def setPrimaryType(t: Tree, pt: String): Tree = {
    t.setProperty(JCR_PRIMARYTYPE, pt, NAME)
    t
  }

  def typeOakUnstructured(t: Tree): Tree = setPrimaryType(t, NT_OAK_UNSTRUCTURED)

}
