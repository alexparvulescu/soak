package com.pfalabs.soak

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.util.Properties

import org.apache.jackrabbit.oak.api.Root
import org.apache.jackrabbit.oak.api.Tree

object Trees {

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
}