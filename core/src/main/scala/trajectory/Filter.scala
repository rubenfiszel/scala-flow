package dawn.flow.trajectory

import dawn.flow._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross}

//https://pdfs.semanticscholar.org/480b/7477c76a1b2b2b130923f09a66ecdb0fb910.pdf
case class OrientationComplimentaryFilter(alpha: Real, dt: Timeframe)
    extends SignalFilter[(Acceleration, BodyRates), Trajectory, Quaternion[Real]] {

  var quat = Quaternion[Real](1, 0, 0, 0)

  def filter(p: Trajectory, x: Timestamped[(Acceleration, BodyRates)]) = {
    val (acc, gyro) = x.v
    val qacc = Trajectory.getQuaternion(acc, Vec3(0, 0, -1))
    val xgyro2 = Trajectory.bodyRateToQuat(quat, gyro)
    val nqgyro = quat + xgyro2*dt
    quat = nqgyro * (1-alpha) + qacc * alpha
    quat
  }

}
