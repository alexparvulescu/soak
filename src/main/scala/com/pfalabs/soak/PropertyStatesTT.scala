package com.pfalabs.soak

import org.apache.jackrabbit.oak.api.PropertyState

object PropertyStatesTT {

  //
  // tagged types!
  //
  // http://eed3si9n.com/learning-scalaz/Tagged+type.html
  // https://gist.github.com/milessabin/89c9b47a91017973a35f

  private[soak]type Tagged[U] = { type Tag = U }
  private[soak]type @@[T, Tag] = T with Tagged[Tag]

  private[soak] class Tagger[U] {
    def apply[T](t: T): T @@ U = t.asInstanceOf[T @@ U]
  }

  private[soak] def tag[U] = new Tagger[U]

  type StringPS = PropertyState @@ String
  type StringsPS = PropertyState @@ List[String]
  type LongPS = PropertyState @@ Long
  type LongsPS = PropertyState @@ List[Long]

  def ttS(p: PropertyState): StringPS = tag(p)
  def ttSs(p: PropertyState): StringsPS = tag(p)
  def ttL(p: PropertyState): LongPS = tag(p)
  def ttLs(p: PropertyState): LongsPS = tag(p)

}