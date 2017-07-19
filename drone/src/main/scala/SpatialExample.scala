package dawn.flow.spatial

import spatial.dsl._
import dawn.flow.spatialf._
import dawn.flow._
import org.virtualized._

object SpatialExample extends FlowApp[Any, Any] {


  val clock: Source[Time] = new Clock(0.05).stop(2)

  val spatial: Source[scala.Double] = new SpatialBatch1(clock) {
    def spatial(x: TSA) = {
      cos(x.v)
    }    
  }

  val spatial2: Source[scala.Double] = new SpatialBatch1(spatial) {
    def spatial(x: TSA) = {
      x.v + 42
    }    
  }
  
  spatial2.debug

  Plot(spatial2)

  drawExpandedGraph()

  run(null, null)

  System.exit(0)
  
}


