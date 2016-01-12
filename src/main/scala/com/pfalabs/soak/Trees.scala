package com.pfalabs.soak

import scala.collection.convert.wrapAll.iterableAsScalaIterable
import scala.util.Properties.lineSeparator

import org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import org.apache.jackrabbit.oak.api.{ PropertyState, Root, Tree, Type }
import org.apache.jackrabbit.oak.api.Type.NAME
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants.NT_OAK_UNSTRUCTURED

import com.pfalabs.soak.PropertyStates.TypeOps.STypeIterable

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
        node.append(lineSeparator)
        node.append(mkString(tc, level + 1, prepend + prepend,
          tc.getName()));
      }
    }
    node.toString()
  }

  // ----------------------------------------------------
  // Trees
  // ----------------------------------------------------

  class TreeOps(val t: Tree) extends AnyVal {

    def get: Tree = t

    def name = t.getName()
    def ?? = t.exists()

    def /(n: String): Tree = t.getChild(n)
    def /!(n: String): Tree = getOrCreate(t, n)
    def /!(n: String, init: Tree => Unit): Tree = getOrCreate(t, n, init)

    def /?(n: String): Boolean = t.hasChild(n)
    def +(n: String): Tree = t.addChild(n)

    def /:/ : Iterable[Tree] = t.getChildren
    def /:/[U](m: Tree => U): Iterable[U] = getChildren(t, m)
    def /:/[U](f: Tree => Boolean, m: Tree => U): Iterable[U] = getChildren(t, f, m)

    def |(p: String): PropertyState = t.getProperty(p)
    def |?(p: String): Boolean = t.hasProperty(p)
    def |-(p: String) = t.removeProperty(p)

    def |:| : Iterable[PropertyState] = t.getProperties

    def |+[U](p: String, v: U): TreeOps = {
      t.setProperty(p, v)
      this
    }

    def |+[U](p: String, v: U, ty: Type[U]): TreeOps = {
      t.setProperty(p, v, ty)
      this
    }

    def |+[U, V](p: String, l: Iterable[U])(implicit ty: STypeIterable[U, V]): TreeOps =
      {
        t.setProperty(p, ty.convertValue(l), ty.convertType())
        this
      }
  }

  object TreeOps {

    def apply(t: Tree) = new TreeOps(t)

    implicit def toTreeOps(t: Tree) = TreeOps(t)

  }

  def getChildren[U](t: Tree, m: Tree => U): Iterable[U] =
    t.getChildren()
      .map(m)

  def getChildren[U](t: Tree, f: Tree => Boolean, m: Tree => U): Iterable[U] =
    t.getChildren()
      .filter(f)
      .map(m)

  def getOrCreate(t: Tree, name: String) =
    if (t.hasChild(name)) {
      t.getChild(name)
    } else {
      t.addChild(name)
    }

  def getOrCreate(t: Tree, name: String, init: Tree => Unit) =
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

  // ----------------------------------------------------
  // Roots
  // ----------------------------------------------------

  class RootOps(val r: Root) extends AnyVal {
    def >(p: String): Tree = r.getTree(p)
    def / = >("/")

    def ?* = r.hasPendingChanges()
    def |+> = r.commit()
  }

  object RootOps {
    def apply(r: Root) = new RootOps(r)
    implicit def toRootOps(r: Root) = RootOps(r)
  }

}
