package dawn.flow.trajectory

import dawn.flow._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{
  norm,
  normalize,
  cross,
  DenseMatrix,
  DenseVector,
  eig,
  eigSym,
  argmax
}
//https://pdfs.semanticscholar.org/480b/7477c76a1b2b2b130923f09a66ecdb0fb910.pdf

case class OrientationComplementaryFilter(
    rawSource1: Source[Acceleration],
    rawSource2: Source[BodyRates],
    rawSource3: Source[(Thrust, BodyRates)],
    init: Quat,
    alpha: Real,
    dt: Timeframe)
    extends Block3[Acceleration, BodyRates, (Thrust, BodyRates), Quat] {

  def name = "OCF"
  val acc = source1
  val gyro = source2
  val thrust = source3.map(_._1)

  def attitudeAcc(atv: ((Acceleration, Thrust), Quat)) = {
    val ((a, th), q) = atv
    TQuaternion.getQuaternion(
      a - Vec3(0, 0, 1) * th, //Remove thrust from acceleration to retrieve gravity
      Vec3(0, 0, -1))

  }
  lazy val accQuat =
    acc.zip(thrust).zip(buffer).map(attitudeAcc _, "ACC2Quat")

  lazy val bodyRateInteg = Integrate(gyro, dt)
  lazy val gyroQuatLocal = bodyRateInteg
    .zip(buffer)
    .map((qv: (Vec3, Quat)) => TQuaternion.localAngleToLocalQuat(qv._2, qv._1),
         "BR2Quat")
  lazy val gyroQuat = gyroQuatLocal.zip(buffer).map(x => x._1.rotateBy(x._2))

  lazy val out =
    gyroQuat.zip(accQuat).map(ga => ga._1 * alpha + ga._2 * (1 - alpha))

  lazy val buffer: Source[Quat] =
    Buffer(out, init, source1)

}
//case class OrientationKalmanFilter(source1: Source[Acceleration, Trajectory], source2: Source[BodyRates, Trajectory], init: Quaternion[Real], alpha: Real, dt: Timeframe) extends Block2T[Quaternion[Real], Trajectory, Acceleration, BodyRates] {

//}

