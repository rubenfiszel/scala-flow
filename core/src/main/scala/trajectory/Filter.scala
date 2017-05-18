package dawn.flow.trajectory

import dawn.flow._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross}

//https://pdfs.semanticscholar.org/480b/7477c76a1b2b2b130923f09a66ecdb0fb910.pdf
case class OrientationComplimentaryFilter(source: SourceT[(Acceleration, BodyRates), Trajectory], alpha: Real, dt: Timeframe, init: Quaternion[Real] = Quaternion[Real](1, 0, 0, 0))
    extends Op1T[Quaternion[Real], Trajectory, (Acceleration, BodyRates)] with Resettable {

  var quat = init

  def reset() = {
    quat = init
  }

  def stream(p: Trajectory) = source.mapT(filter _).stream(p)

  def filter(x: (Acceleration, BodyRates)) = {
    val (acc, gyro) = x
    val qacc = Trajectory.getQuaternion(acc, Vec3(0, 0, -1))
    val xgyro2 = Trajectory.bodyRateToQuat(quat, gyro)
    val nqgyro = quat + xgyro2*dt
    quat = nqgyro * (1-alpha) + qacc * alpha
    quat
  }

}

case class OrientationComplimentaryFilterBuffered(source1: SourceT[(Acceleration, BodyRates), Trajectory], source2: SourceT[Quaternion[Real], Trajectory], alpha: Real, dt: Timeframe)
    extends Op2T[Quaternion[Real], Trajectory, (Acceleration, BodyRates), Quaternion[Real]] {

  def stream(p: Trajectory) = source1.stream(p).zip(source2.stream(p)).map(filter _)

  def filter(x:(Timestamped[(Acceleration, BodyRates)], Timestamped[Quaternion[Real]])) = {
    val (acc, gyro) = x._1.v
    val quat = x._2.v
    val qacc = Trajectory.getQuaternion(acc, Vec3(0, 0, -1))
    val xgyro2 = Trajectory.bodyRateToQuat(quat, gyro)
    val nqgyro = quat + xgyro2*dt
    val nquat = nqgyro * (1-alpha) + qacc * alpha
    Timestamped(x._1.t, nquat)
  }

}

//case class OComplementaryFilter(alpha: Real, dt: Timeframe) extends Block2[Quaternion[Real], Trajectory, Accelerometer, BodyRates] {}
