package dawn.flow

import spire.math.Quaternion
import spatial._
import spatial.dsl._
import argon.nodes._
import argon.core.Const
import org.virtualized._

package object spatialf {

  //lifting to constant require a state :<
  implicit private val fakeState = new argon.core.State {}

  type SReal = Double

  implicit object DoubleSpatial extends Spatialable[scala.Double] {
    type Spatial = Double
    type Internal = BigDecimal
    def from(x: Internal): scala.Double = x.toDouble
    def to(x: scala.Double): Spatial = x
    def bitsI = implicitly[Bits[Spatial]]
    def typeI = implicitly[Type[Spatial]]    
  }

  @struct case class SVec3(a: SReal, b: SReal, c: SReal)
  @struct case class SQuat(r: SReal, i: SReal, j: SReal, k: SReal)
  @struct case class SIMU(a: SVec3, o: SVec3)
  @struct case class SVicon(p: SVec3, q: SQuat)
  type SPOSE = SVicon

  implicit object Vec3Spatial extends Spatialable[Vec3] {
    type Spatial = SVec3
    type Internal = Seq[(java.lang.String, Const[_])]
    def from(x: Internal): Vec3 = {
      val m = x.toMap.mapValues(_.asInstanceOf[Const[_]].c.asInstanceOf[BigDecimal])
      Vec3(m("a").toDouble, m("b").toDouble, m("c").toDouble)
    }
    def to(x: Vec3) = SVec3(x.x, x.y, x.z)
    def bitsI = implicitly[Bits[SVec3]]
    def typeI = implicitly[Type[SVec3]]
  }

  implicit object ViconSpatial extends Spatialable[(Position, Attitude)] {
    type Spatial = SVicon
    type Internal = Seq[(java.lang.String, Const[_])]
    def from(x: Internal): (Position, Attitude) = {
      val m = x.toMap.mapValues(_.asInstanceOf[Const[_]].c.asInstanceOf[Seq[(java.lang.String, Const[_])]])
      (Vec3Spatial.from(m("p")), QuatSpatial.from(m("q")))
    }
    def to(x: (Position, Attitude)) = SVicon(Vec3Spatial.to(x._1), QuatSpatial.to(x._2))
    def bitsI = implicitly[Bits[SVicon]]
    def typeI = implicitly[Type[SVicon]]
  }

  implicit object IMUSpatial extends Spatialable[(Acceleration, Omega)] {
    type Spatial = SIMU
    type Internal = Seq[(java.lang.String, Const[_])]
    def from(x: Internal): (Acceleration, Omega) = {
      val m = x.toMap.mapValues(_.asInstanceOf[Const[_]].c.asInstanceOf[Seq[(java.lang.String, Const[_])]])      
      (Vec3Spatial.from(m("a")), Vec3Spatial.from(m("o")))
    }
    def to(x: (Acceleration, Omega)) = SIMU(Vec3Spatial.to(x._1), Vec3Spatial.to(x._2))
    def bitsI = implicitly[Bits[SIMU]]
    def typeI = implicitly[Type[SIMU]]    
  }

  implicit object QuatSpatial extends Spatialable[Quat] {
    type Spatial = SQuat
    type Internal = Seq[(java.lang.String, Const[_])]
    def from(x: Internal): Quat = {
      val m = x.toMap.mapValues(_.asInstanceOf[Const[_]].c.asInstanceOf[BigDecimal])
      Quaternion(m("r").toDouble, m("i").toDouble, m("j").toDouble, m("k").toDouble)
    }
    def to(x: Quat) = SQuat(x.r, x.i, x.j, x.k)
    def bitsI = implicitly[Bits[SQuat]]
    def typeI = implicitly[Type[SQuat]]
  }
  
}
