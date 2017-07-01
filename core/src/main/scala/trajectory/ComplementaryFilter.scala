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
    rawSource1: Source[(Acceleration, Omega)],
  rawSource2: Source[Thrust],
  alpha: Real
)(implicit val initHook: InitHook[TrajInit])
    extends Block2[(Acceleration, Omega), Thrust, Quat]
    with RequireInit[TrajInit]
{

  def name = "OCF"

  lazy val (acc, omegaG) = source1.unzip2
  def thrust = source2

  def attitudeAcc(at: (Acceleration, Thrust)) = {
    val (a, th) = at
    Quat.getQuaternion(
      a - Vec3(0, 0, 1) * th, //Remove thrust from acceleration to retrieve gravity
      Vec3(0, 0, -1))
  }

  lazy val accQuat =
    acc
      .zip(thrust, true)
      .map(attitudeAcc, "ACC2Quat")



  lazy val bodyRateInteg: Source[Vec3] =
    omegaG
      .zipT(buffer, true)
      .map(x => x._1.v*(x._1.t - x._2.t), "Integ")

  lazy val gyroQuatLocal =
    bodyRateInteg
      .map(Quat.localAngleToLocalQuat(_),
        "BR2Quat")

  lazy val gyroQuat =
    gyroQuatLocal
      .zip(buffer, true)
      .map(x => x._1.rotateBy(x._2), "Rotation")

  lazy val buffer: Source[Quat] =
    Buffer(out, init.q, source1)

  lazy val out =
    gyroQuat
      .zip(accQuat, true)
      .map(ga => ga._1 * alpha + ga._2 * (1 - alpha), "Combining")


}


