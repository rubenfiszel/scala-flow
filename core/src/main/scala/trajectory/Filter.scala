package dawn.flow.trajectory

import dawn.flow._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross}

//https://pdfs.semanticscholar.org/480b/7477c76a1b2b2b130923f09a66ecdb0fb910.pdf

case class OrientationComplementaryFilter(
    source1: SourceT[Acceleration, Trajectory],
    source2: SourceT[BodyRates, Trajectory],
    source3: SourceT[Thrust, Trajectory],
    init: Quaternion[Real],
    alpha: Real,
    dt: Timeframe)
    extends Op3T[Quaternion[Real], Trajectory, Acceleration, BodyRates, Thrust] {

  val acc    = source1
  val gyro   = source2
  val thrust = source3

  def attitudeAcc(atv: ((Acceleration, Thrust), Quaternion[Real])) = {
    val ((a, th), q) = atv
    TQuaternion.getQuaternion(
      a - Vec3(0, 0, 1) * th, //Remove thrust from acceleration to retrieve gravity
      Vec3(0, 0, -1))

  }
  lazy val accQuat =
    acc.zipT(thrust).zipT(buffer).mapT(attitudeAcc _, "ACC2Quat")
  lazy val bodyRateInteg = Integrate(gyro, dt)
  lazy val gyroQuatLocal = bodyRateInteg
    .zipT(buffer)
    .mapT((qv: (Vec3, Quaternion[Real])) =>
            TQuaternion.localAngleToLocalQuat(qv._2, qv._1),
          "BR2Quat")
  lazy val gyroQuat = gyroQuatLocal.zipT(buffer).mapT(x => x._1.rotateBy(x._2))

  lazy val cf =
    gyroQuat.zip(accQuat).map(ga => ga._1 * alpha + ga._2 * (1 - alpha))
  lazy val buffer: SourceT[Quaternion[Real], Trajectory] =
    Buffer(cf, Timestamped(init))

  def genStream(p: Trajectory) = cf.stream(p)
}
//case class OrientationKalmanFilter(source1: SourceT[Acceleration, Trajectory], source2: SourceT[BodyRates, Trajectory], init: Quaternion[Real], alpha: Real, dt: Timeframe) extends Block2T[Quaternion[Real], Trajectory, Acceleration, BodyRates] {

//}
