package dawn.flow.trajectory

import dawn.flow._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross}

//https://pdfs.semanticscholar.org/480b/7477c76a1b2b2b130923f09a66ecdb0fb910.pdf
case class OrientationComplementaryFilter(source: SourceT[(Acceleration, BodyRates), Trajectory], alpha: Real, dt: Timeframe, init: Quaternion[Real] = Quaternion[Real](1, 0, 0, 0))
    extends Op1T[Quaternion[Real], Trajectory, (Acceleration, BodyRates)]  {

  var quat = init

  override def reset() = {
    super.reset()
    quat = init
  }

  def genStream(p: Trajectory) = source.mapT(filter _).stream(p)

  def filter(x: (Acceleration, BodyRates)) = {
    val (acc, gyro) = x
    val qacc = TQuaternion.getQuaternion(acc, Vec3(0, 0, -1))
    val xgyro2 = TQuaternion.bodyRateToQuat(quat, gyro)
    val nqgyro = quat + xgyro2*dt
    quat = nqgyro * (1-alpha) + qacc * alpha
    quat
  }

}

case class OrientationComplementaryFilterBuffered(source1: SourceT[(Acceleration, BodyRates), Trajectory], source2: SourceT[Quaternion[Real], Trajectory], alpha: Real, dt: Timeframe)
    extends Op2T[Quaternion[Real], Trajectory, (Acceleration, BodyRates), Quaternion[Real]] {

  def genStream(p: Trajectory) = source1.zip(source2).map(filter _).stream(p)

  def filter(x:(Timestamped[(Acceleration, BodyRates)], Timestamped[Quaternion[Real]])) = {
    val (acc, gyro) = x._1.v
    val quat = x._2.v
    val qacc = TQuaternion.getQuaternion(acc, Vec3(0, 0, -1))
    val xgyro2 = TQuaternion.bodyRateToQuat(quat, gyro)
    val nqgyro = quat + xgyro2*dt
    val nquat = nqgyro * (1-alpha) + qacc * alpha
    Timestamped(x._1.t, nquat)
  }

}

case class OrientationComplementaryFilterBlock(source1: SourceT[Acceleration, Trajectory], source2: SourceT[BodyRates, Trajectory], init: Quaternion[Real], alpha: Real, dt: Timeframe) extends Block2T[Quaternion[Real], Trajectory, Acceleration, BodyRates] {

  val timestamp = source1
  val acc = source1.map(_.v)
  val gyro = source2.map(_.v)  
  lazy val accQuat = acc.map((x: Acceleration) => TQuaternion.getQuaternion(x, Vec3(0, 0, -1)), "ACC2Quat")
  lazy val gyroQuatDt = buffer.zip(gyro).map((qv: (Quaternion[Real], Vec3)) => TQuaternion.bodyRateToQuat(qv._1, qv._2), "BR2Quat")
  lazy val gyroQuat = buffer.zip(Integrate(gyroQuatDt, dt)).map(x => x._1 + x._2)
  lazy val cf = ComplementaryFilter[Quaternion[Real], Trajectory](accQuat, gyroQuat, init, alpha)
  lazy val buffer: Source[Quaternion[Real], Trajectory] = Buffer(cf, init)
  def genStream(p: Trajectory) = cf.zip(timestamp).map(cft => Timestamped(cft._2.t, cft._1, cft._2.dt)).stream(p)
}
