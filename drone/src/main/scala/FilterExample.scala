package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._

object FilterExample extends App {

  implicit val mc = ModelHook[Trajectory]

  val alpha = 0.5

  val clock: Source[Time] = new Clock(0.05).stop(2)
  val ts = clock.map(Timestamp(1000))
  val ts2 = clock.map(Timestamp(1300))
  val fused = ts.fusion(ts2)
  val sinus: Source[Quat] = fused.map(
    x => Quaternion(sin(x), cos(x), tan(x), 1 / (x + 0.000001)),
    "Sinus")
  val filt =
    LowPassFilter(sinus, Quaternion(1.0, 0, 0, 0), alpha)

  val plot = Plot(filt)

//  PrimarySchedulerHook.run()
//  PrimaryNodeHook.drawGraph()

}
