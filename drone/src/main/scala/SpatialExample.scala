package dawn.flow.spatial

import spatial.dsl._
import dawn.flow.spatialf._
import dawn.flow._
import org.virtualized._

object SpatialExample extends FlowApp[Any, Any] {


  val clock1 = new Clock(0.1).stop(10)
  val clock2 = new Clock(0.1).stop(10)  

  val spatial = new SpatialBatch1[Time, Time, Double, Double](clock1) {
    def spatial(x: TSA) = {
      cos(x.v)
    }    
  }

  val spatial2 = new SpatialBatch1[Time, Time, Double, Double](clock2) {
    def spatial(x: TSA) = {
      x.v + 42
    }    
  }

/*
  val spatial3 = new SpatialBatch2[Time, Time, Time, Double, Double, Double](spatial, spatial2) {
    def spatial(x: Either[TSA, TSB]) = {
      x match {
        case Right(t) => t.v+10
        case Left(t) => t.v-10
      }
    }    
  }
 */

  spatial.merge(spatial2).debug

//  Plot(spatial3)

  drawExpandedGraph()

  run(null, null)

  System.exit(0)

}


