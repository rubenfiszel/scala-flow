package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._

object FilterExample extends App {
  
  implicit val mc = new ModelCallBack[Trajectory]()
  implicit val sh = new Scheduler {}

  val alpha = 0.5

  val clock: Source[Time] = Clock(0.05, 2)
  val ts    = clock.map(Timestamp(1000))
  val ts2    = clock.map(Timestamp(1300))
  val fused = ts.fusion(ts2)
  val sinus: SourceT[Quat] = fused.mapT(x => Quaternion(sin(x), cos(x), tan(x), 1/(x+0.000001)), "Sinus")
  val filt =
    LowPassFilter(sinus, Timestamped(0, Quaternion(1.0, 0, 0, 0)), alpha)

  val plot = Plot(filt)

  sh.run()

  Sourcable.drawGraph(Seq(plot))

}
