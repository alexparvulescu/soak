package com.pfalabs.soak

import scala.collection.JavaConversions.iterableAsScalaIterable

import org.apache.jackrabbit.oak.api.{ PropertyState, Tree, Type }
import org.apache.jackrabbit.oak.api.Type.{ BOOLEAN, LONG, LONGS, STRING, STRINGS }

object PropertyStates {

  /**
   * @param t tree
   * @param name property name
   * @return returns the string representation of the value.
   */
  def asS(t: Tree, name: String): Option[String] =
    Option(t.getProperty(name)).flatMap { p => asType(p, STRING) }

  def asS(t: Tree, name: String, default: String): String =
    Option(t.getProperty(name)).flatMap { p => asType(p, STRING) }.getOrElse(default)

  def asSs(t: Tree, name: String): Option[Iterable[String]] =
    Option(t.getProperty(name))
      .flatMap { p => asType(p, STRINGS) }
      .map { x => iterableAsScalaIterable(x) }

  /**
   * @param t tree
   * @param name property name
   * @return returns the long representation of the value. Can be <code>None</code> when the value cannot be converted to long
   */
  def asL(t: Tree, name: String): Option[Long] =
    Option(t.getProperty(name)).flatMap { p => asType(p, LONG) }.map { l => l }

  def asLs(t: Tree, name: String): Option[Iterable[Long]] =
    Option(t.getProperty(name))
      .flatMap { p => asType(p, LONGS) }
      .map { x => x.map(_.asInstanceOf[Long]) }

  /**
   * @param t tree
   * @param name property name
   * @return returns the integer representation of the value. Can be <code>None</code> when the value cannot be converted, or when the value is bigger than <code>Int.MaxValue</code>
   */
  def asI(t: Tree, name: String): Option[Int] =
    asL(t, name).filter { l => l <= Int.MaxValue }.map { l => l.intValue() }

  def asIs(t: Tree, name: String): Option[Iterable[Int]] =
    asLs(t, name)
      .map { x => x.filter { l => l <= Int.MaxValue }.map { l => l.intValue() } }

  /**
   * @param t tree
   * @param name property name
   * @return returns the boolean representation of the value. Delegates to underlying impl for conversion, which uses <code>Boolean.parseBoolean</code>.
   */
  def asB(t: Tree, name: String): Option[Boolean] =
    Option(t.getProperty(name)).flatMap { p => asType(p, BOOLEAN) }.map { l => l }

  private def asType[U](ps: PropertyState, t: Type[U]): Option[U] = {
    try {
      t match {
        case STRING  => Some(ps.getValue(t))
        case STRINGS => Some(ps.getValue(t))
        case LONG    => Some(ps.getValue(t))
        case LONGS   => Some(ps.getValue(t))
        case BOOLEAN => Some(ps.getValue(t))
        case _       => None
      }
    } catch {
      case t: IllegalArgumentException => None
    }
  }
}