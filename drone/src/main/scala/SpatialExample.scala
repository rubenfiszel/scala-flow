package dawn.flow.spatial

import spatial.dsl._
import dawn.flow.spatialf._
import dawn.flow._
import org.virtualized._

object SpatialExample extends FlowApp[Any, Any] {


  val clock1 = new Clock(0.1).stop(20)
  val clock2 = new Clock(0.2).stop(20)  

  val spatial = new SpatialBatch1(clock1) {
    def spatial(x: TSA) = {
      cos(x.v)
    }    
  }

  val spatial2 = new SpatialBatch1(spatial) {
    def spatial(x: TSA) = {
      x.v + 42
    }    
  }

  val spatial3 = new SpatialBatch2(clock1, clock2) {
    def spatial(x: Either[TSA, TSB]) = {
      x match {
        case Right(t) => 0.5
        case Left(t) => 1.0
      }
    }    
  }
  
  spatial3.debug

  Plot(spatial3)

  drawExpandedGraph()

  run(null, null)

  System.exit(0)

}


