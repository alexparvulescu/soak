package com.pfalabs.soak

import scala.collection.JavaConversions.iterableAsScalaIterable

import org.apache.jackrabbit.oak.api.{ Root, Tree }
import org.apache.jackrabbit.oak.api.QueryEngine.{ NO_BINDINGS, NO_MAPPINGS }

object Queries {

  def xpath[T](xp: String, f: Tree => T)(root: Root): Iterable[T] =
    xpath(xp, f, Int.MaxValue, 0)(root)

  def xpath[T](xp: String, f: Tree => T, limit: Long, offset: Long)(root: Root): Iterable[T] = {
    val r = root.getQueryEngine().executeQuery(xp, "xpath", limit, offset, NO_BINDINGS, NO_MAPPINGS)
    r.getRows().map(_.getPath()).map(root.getTree).map(f)
  }

  def xpathUnique[T](xp: String)(root: Root): Option[String] = {
    val r = root.getQueryEngine().executeQuery(xp, "xpath", 1, 0, NO_BINDINGS, NO_MAPPINGS)
    val it = r.getRows().iterator()
    if (it.hasNext()) {
      val path = it.next().getPath()
      Some(path)
    } else {
      None
    }
  }

  def xpathUnique[T](xp: String, f: Tree => T)(root: Root): Option[T] =
    xpathUnique(xp)(root).map { p => f(root.getTree(p)) }

}
