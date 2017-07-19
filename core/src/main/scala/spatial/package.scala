package dawn.flow

import spatial._
import spatial.dsl._
import argon.nodes._
import argon.core.Const
import org.virtualized._

package object spatialf {

  implicit object DoubleSpatial extends Spatialable[scala.Double] {
    type Spatial = Double
    type Internal = BigDecimal
    def from(x: BigDecimal) = x.toDouble
    def to(x: scala.Double): Double = new Double(new Const[Double](DoubleType)(BigDecimal(x)))
    def bitsI = implicitly[Bits[Double]]
    def typeI = implicitly[Type[Double]]    
  }

}
