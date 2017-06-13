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
    rawSource2: Source[Omega],
  rawSource3: Source[Thrust],
    init: Quat,
  alpha: Real
)
    extends Block3[Acceleration, Omega, Thrust, Quat] {

  def name = "OCF"

  def acc = source1
  def omegaG = source2
  def thrust = source3

  def attitudeAcc(atv: ((Acceleration, Thrust), Quat)) = {
    val ((a, th), q) = atv
    Quat.getQuaternion(
      a - Vec3(0, 0, 1) * th, //Remove thrust from acceleration to retrieve gravity
      Vec3(0, 0, -1))
  }

  lazy val accQuat =
    acc
      .zip(thrust)
      .zip(buffer)
      .map(attitudeAcc, "ACC2Quat")


  lazy val omegaBuffered =
    omegaG
      .bufferWithTime(Vec3())

  lazy val bodyRateInteg: Source[Vec3] =
    omegaBuffered
      .zipT(buffer)
      .map(x => x._1.v*(x._1.t - x._2.t), "Integ")

  lazy val gyroQuatLocal =
    bodyRateInteg
      .map(Quat.localAngleToLocalQuat(_),
        "BR2Quat")

  lazy val gyroQuat =
    gyroQuatLocal
      .zip(buffer)
      .map(x => x._1.rotateBy(x._2), "Rotation")

  lazy val buffer: Source[Quat] =
    Buffer(out, init, source1)

  lazy val out =
    gyroQuat
      .zip(accQuat)
      .map(ga => ga._1 * alpha + ga._2 * (1 - alpha))


}


