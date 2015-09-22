package com.pfalabs.soak

import scala.collection.JavaConversions.iterableAsScalaIterable

import org.apache.jackrabbit.oak.api.Root

import com.pfalabs.soak.Trees.TreeOps

import Trees.TreeOps

object Queries {

  def xpath[T](xp: String, root: Root, f: TreeOps[T]): Iterable[T] =
    xpath(xp, root, f, Int.MaxValue, 0)

  def xpath[T](xp: String, root: Root, f: TreeOps[T], limit: Long, offset: Long): Iterable[T] = {
    val r = root.getQueryEngine().executeQuery(xp, "xpath", limit, offset, null, null)
    r.getRows().map(_.getPath()).map(root.getTree).map(f)
  }

  def xpathUnique[T](xp: String, root: Root): Option[String] = {
    val r = root.getQueryEngine().executeQuery(xp, "xpath", 1, 0, null, null)
    val it = r.getRows().iterator()
    if (it.hasNext()) {
      val path = it.next().getPath()
      Some(path)
    } else {
      None
    }
  }

  def xpathUnique[T](xp: String, root: Root, f: TreeOps[T]): Option[T] = {
    val r = root.getQueryEngine().executeQuery(xp, "xpath", 1, 0, null, null)
    val it = r.getRows().iterator()
    if (it.hasNext()) {
      val path = it.next().getPath()
      Some(f(root.getTree(path)))
    } else {
      None
    }
  }

}
