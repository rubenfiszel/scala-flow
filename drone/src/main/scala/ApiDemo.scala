package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg.norm
import spire.math.Quaternion

object ApiDemo extends FlowApp[Null, Null] {

//   */  

  val clock = new Clock(0.1)
    .stop(4)

  val clock2 = new Clock(0.3)
    .stop(4)

  val clock3 = new Clock(0.25)
    .stop(1.2)
  

  clock
    .muted
    .filter(_ < 0.2, "< 2.0")//
    .map(_*2.0, "*2")
    .zip(clock)
    .zipLast(clock2)
    .merge(clock2)

  clock
    .fusion(clock.latency(0.001))
    .takeWhile(_ < 0.8, "< 0.8")
    .accumulate(clock3)
    .debug    
//    .reduce(0.0, (x: Time, y: Time) => x + y)
//    .groupBy(_.ceil)
//    .foreach(println)


  drawExpandedGraph()
  run(null, null)
}



