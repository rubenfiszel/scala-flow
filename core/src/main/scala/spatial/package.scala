package dawn.flow

import spatial._
import spatial.dsl._
import argon.nodes._
import argon.core.Const
import org.virtualized._

package object spatialf {

  //lifting to constant require a state :<
  implicit private val fakeState = new argon.core.State {}

  implicit object DoubleSpatial extends Spatialable[scala.Double] {
    type Spatial = Double
    type Internal = BigDecimal
    def from(x: Internal): scala.Double = x.toDouble
    def to(x: scala.Double): Spatial = x
    def bitsI = implicitly[Bits[Spatial]]
    def typeI = implicitly[Type[Spatial]]    
  }

  @struct case class SVec3(a: Double, b: Double, c: Double)

  implicit object Vec3Spatial extends Spatialable[Vec3] {
    type Spatial = SVec3
    type Internal = Seq[(java.lang.String, Const[_])]
    def from(x: Internal): Vec3 = {
      val m = x.toMap.mapValues(_.asInstanceOf[BigDecimal])
      Vec3(m("a").toDouble, m("b").toDouble, m("c").toDouble)
    }
    def to(x: Vec3) = SVec3(x.x, x.y, x.z)
    def bitsI = implicitly[Bits[SVec3]]
    def typeI = implicitly[Type[SVec3]]
  }
}
