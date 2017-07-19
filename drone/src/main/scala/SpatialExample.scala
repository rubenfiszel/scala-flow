package dawn.flow.spatial

import argon.core.Const
import spatial._
import spatial.dsl._

import dawn.flow.spatial._
import dawn.flow._
import org.virtualized._

object SpatialExample extends FlowApp[Any, Any] {

  val alpha = 0.5

  val clock: Source[Time] = new Clock(0.05).stop(2)
  val ts = clock.map(Timestamp(1000))
//  val ts2 = clock.map(Timestamp(1300))

/*  val spatial = new SpatialBatch2[Time, Time, scala.Double] {
    def rawSource1 = ts
    def rawSource2 = ts2

    type SpatialA = Float 
    type SpatialB = Float           
    type SpatialR = Float


    @virtualize def spatial() = {
      cos(in1) + cos(in2)
    }
  }
 */

  val spatial: Source[scala.Double] = new SpatialBatch1[Time, scala.Double, Float, Float](ts) {
    def convertA(x: Timestamped[Time]): Float = x.v
    def convertOutput(x: scala.Any): Timestamped[scala.Double] = {
      val v =  x.asInstanceOf[BigDecimal].toDouble
      Timestamped(0.1, v)
    }
    def spatial() = {
      cos(in1)
    }
    
  }

  val spatial2: Source[scala.Double] = new SpatialBatch1[scala.Double, scala.Double, Float, Float](spatial) {
    def convertA(x: Timestamped[scala.Double]): Float = x.v
    def convertOutput(x: scala.Any): Timestamped[scala.Double] = {
      val v =  x.asInstanceOf[BigDecimal].toDouble
      Timestamped(0.1, v)
    }
    def spatial() = {
      in1.value + 1f
    }
    
  }
  spatial2.debug
  val plot = Plot(spatial2)

  drawExpandedGraph()

  run(null, null)

  System.exit(0)
}


