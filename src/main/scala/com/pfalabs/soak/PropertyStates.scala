package com.pfalabs.soak

//import scala.collection.JavaConverters.{ asJavaIterable, iterableAsScalaIterable, iterableAsScalaIterableConverter }
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.JavaConverters.asJavaIterableConverter

import org.apache.jackrabbit.oak.api.{ PropertyState, Tree, Type }
import org.apache.jackrabbit.oak.api.Type.{ BOOLEAN, BOOLEANS, LONG, LONGS, STRING, STRINGS }

object PropertyStates {

  /**
   * @param t tree
   * @param name property name
   * @return returns the string representation of the value.
   */
  def asS(t: Tree, name: String): Option[String] = asS(t.getProperty(name))

  def asS(ps: PropertyState): Option[String] =
    Option(ps).flatMap { p => asType(p, STRING) }

  def asS(t: Tree, name: String, default: String): String =
    asS(t.getProperty(name), default)

  def asS(ps: PropertyState, default: String): String =
    Option(ps).flatMap { p => asType(p, STRING) }.getOrElse(default)

  def asSs(t: Tree, name: String): Option[Iterable[String]] =
    asSs(t.getProperty(name))

  def asSs(ps: PropertyState): Option[Iterable[String]] =
    Option(ps)
      .flatMap { p => asType(p, STRINGS) }
      .map { x => x.asScala }

  /**
   * @param t tree
   * @param name property name
   * @return returns the long representation of the value. Can be <code>None</code> when the value cannot be converted to long
   */
  def asL(t: Tree, name: String): Option[Long] = asL(t.getProperty(name))

  def asL(ps: PropertyState): Option[Long] =
    Option(ps).flatMap { p => asType(p, LONG) }.map { l => l }

  def asLs(t: Tree, name: String): Option[Iterable[Long]] =
    asLs(t.getProperty(name))

  def asLs(ps: PropertyState): Option[Iterable[Long]] =
    Option(ps)
      .flatMap { p => asType(p, LONGS) }
      .map { x => x.asScala.map(_.asInstanceOf[Long]) }

  /**
   * @param t tree
   * @param name property name
   * @return returns the integer representation of the value. Can be <code>None</code> when the value cannot be converted, or when the value is bigger than <code>Int.MaxValue</code>
   */
  def asI(t: Tree, name: String): Option[Int] =
    asI(t.getProperty(name))

  def asI(ps: PropertyState): Option[Int] =
    asL(ps).filter { l => l <= Int.MaxValue }.map { l => l.intValue() }

  def asIs(t: Tree, name: String): Option[Iterable[Int]] =
    asIs(t.getProperty(name))

  def asIs(ps: PropertyState): Option[Iterable[Int]] =
    asLs(ps)
      .map { x => x.filter { l => l <= Int.MaxValue }.map { l => l.intValue() } }

  /**
   * @param t tree
   * @param name property name
   * @return returns the boolean representation of the value. Delegates to underlying impl for conversion, which uses <code>Boolean.parseBoolean</code>.
   */
  def asB(t: Tree, name: String): Option[Boolean] =
    asB(t.getProperty(name))

  def asB(ps: PropertyState): Option[Boolean] =
    Option(ps).flatMap { p => asType(p, BOOLEAN) }.map { l => l }

  def asBs(t: Tree, name: String): Option[Iterable[Boolean]] =
    asBs(t.getProperty(name))

  def asBs(ps: PropertyState): Option[Iterable[Boolean]] =
    Option(ps)
      .flatMap { p => asType(p, BOOLEANS) }
      .map { x => x.asScala.map(_.booleanValue()) }

  private def asType[U](ps: PropertyState, t: Type[U]): Option[U] = {
    try {
      t match {
        case STRING => Some(ps.getValue(t))
        case STRINGS => Some(ps.getValue(t))
        case LONG => Some(ps.getValue(t))
        case LONGS => Some(ps.getValue(t))
        case BOOLEAN => Some(ps.getValue(t))
        case BOOLEANS => Some(ps.getValue(t))
        case _ => None
      }
    } catch {
      case t: IllegalArgumentException => None
    }
  }

  // ----------------------------------------------------
  // Property Type Helpers
  // ----------------------------------------------------

  object TypeOps {

    implicit def stringIterableType = STRINGS

    implicit def longIterableType = LONGS

    implicit def intIterableType = INTS

    implicit def boolIterableType = BOOLS

    private[soak] sealed trait STypeIterable[U, V] {
      def convertValue(l: Iterable[U]): V
      def convertType(): Type[V]
    }

    private[soak] case object STRINGS extends STypeIterable[String, java.lang.Iterable[String]] {
      def convertValue(l: Iterable[String]) = l.asJava
      def convertType() = Type.STRINGS
    }

    private[soak] case object LONGS extends STypeIterable[Long, java.lang.Iterable[java.lang.Long]] {
      def convertValue(l: Iterable[Long]) = l.map(_.asInstanceOf[java.lang.Long]).asJava
      def convertType() = Type.LONGS
    }

    private[soak] case object INTS extends STypeIterable[Int, java.lang.Iterable[java.lang.Long]] {
      def convertValue(l: Iterable[Int]) = l.map(java.lang.Long.valueOf(_)).asJava
      def convertType() = Type.LONGS
    }

    private[soak] case object BOOLS extends STypeIterable[Boolean, java.lang.Iterable[java.lang.Boolean]] {
      def convertValue(l: Iterable[Boolean]) = l.map(_.asInstanceOf[java.lang.Boolean]).asJava
      def convertType() = Type.BOOLEANS
    }

  }

}