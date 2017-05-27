package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._

object FilterExample extends App {
  
  implicit val mc = new ModelCallBack[Trajectory]()

  val alpha = 0.5

  val clock = Clock(0.05).stop(2)
  val ts    = clock.map(Timestamp(1000))
  val ts2    = clock.map(Timestamp(1300))
  val fused = ts.fusion(ts2)
  val sinus = fused.mapT(x => Quaternion(sin(x), cos(x), tan(x), 1/x), "Sinus")
  val filt =
    LowPassFilter(sinus, Timestamped(0, Quaternion(1.0, 0, 0, 0), 0), alpha)

  val plot = Plot(filt)

  val sinks = Seq(plot)

  val sim = Simulation(sinks)
  sim.run(null)

  Sourcable.drawGraph(sinks)

}
