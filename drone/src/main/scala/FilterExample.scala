package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._

object FilterExample extends App {
  
  implicit val mc = new ModelCallBack[Trajectory]()

  val alpha = 0.5

  val clock = Clock(0.1).stop(1)
  val ts    = clock.map(Timestamp(1000))
  val sinus = ts.mapT(x => Quaternion(1.0, 0, 0, 0), "Sinus")
  val filt =
    LowPassFilter(sinus, Timestamped(0, Quaternion(1.0, 0, 0, 0), 0), alpha)

  val plot = Plot(filt)

  val sinks = Seq(plot)

  val sim = Simulation(sinks)
  sim.run(null)

  Sourcable.drawGraph(sinks)

}
