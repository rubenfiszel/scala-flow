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

case class OrientationComplementaryFilter(source1: SourceT[Acceleration],
                                          source2: SourceT[BodyRates],
                                          source3: SourceT[(Thrust, BodyRates)],
                                          init: Quat,
                                          alpha: Real,
                                          dt: Timeframe)
    extends Op3T[Acceleration, BodyRates, (Thrust, BodyRates), Quat] {

  val acc    = source1
  val gyro   = source2
  val thrust = source3.mapT(_._1)

  def attitudeAcc(atv: ((Acceleration, Thrust), Quat)) = {
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
    .mapT(
      (qv: (Vec3, Quat)) => TQuaternion.localAngleToLocalQuat(qv._2, qv._1),
      "BR2Quat")
  lazy val gyroQuat = gyroQuatLocal.zipT(buffer).mapT(x => x._1.rotateBy(x._2))

  lazy val cf =
    gyroQuat.zip(accQuat).map(ga => ga._1 * alpha + ga._2 * (1 - alpha))
  lazy val buffer: SourceT[Quat] =
    Buffer(cf, Timestamped(init))

  def genStream() = cf.stream()
}
//case class OrientationKalmanFilter(source1: SourceT[Acceleration, Trajectory], source2: SourceT[BodyRates, Trajectory], init: Quaternion[Real], alpha: Real, dt: Timeframe) extends Block2T[Quaternion[Real], Trajectory, Acceleration, BodyRates] {

//}

//https://ai2-s2-pdfs.s3.amazonaws.com/0322/8afc107f925b7a0ca77d5ade2fda9050f0a3.pdf
case class ParticleFilter(source1: SourceT[Acceleration],
                          source2: SourceT[BodyRates],
                          source3: SourceT[(Thrust, BodyRates)],
                          source4: SourceT[(Position, Quat)],
                          init: Quat,
                          dt: Timeframe,
                          N: Int,
                          bodyRateStd: DenseMatrix[Real])
    extends Op4T[Acceleration, BodyRates, (Thrust, BodyRates), (Position, Quat), Quat]
{

  type Weight = Real
  case class State(w: Weight, q: Quat, br: BodyRates, p: Position, v: Velocity, a: Acceleration)

  def sampleBR(q: Quat, br: BodyRates): Quat = {
    val withNoise  = Rand.gaussian(br, bodyRateStd)
    val integrated = withNoise * dt
    val lq         = TQuaternion.localAngleToLocalQuat(q, integrated)
    lq.rotateBy(q)
  }

  //http://users.isy.liu.se/rt/schon/Publications/HolSG2006.pdf
  //See systematic resampling
  def resample(sb: Seq[State]) = {
    val u  = Rand.uniform()
    val us = Array.tabulate(N)(i => (i + u) / N)
    val ws = sb.map(_.w).scanLeft(0.0)(_ + _)
    val ns = Array.fill(N)(0)
    var cu = 0
    for (w <- ws) {
      while (cu < N && us(cu) <= w) {
        ns(cu) += 1
        cu += 1
      }
    }
    sb.zip(ns).flatMap(x => List.fill(x._2)(x._1)).map(_.copy(w = 1.0 / N))
  }

  //https://stackoverflow.com/questions/12374087/average-of-multiple-quaternions
  def averageQuaternions(sq: Seq[(Weight, Quat)]): Quat = {

    //Should be the right way but doesn't seem to work as good as the second technique
    /*
    val sqdv = sq.map(_.toDenseVector)
    val qr = DenseMatrix.zeros[Real](4, 4)
    for (q <- sqdv) {
      qr += (q*q.t)/N.toDouble
    }
    val eg = eig(qr)
    val pvector = eg.eigenvectors(::, 0)
    Quaternion(pvector(0), pvector(1), pvector(2), pvector(3)).normalized
     */

    //Use this instead http://wiki.unity3d.com/index.php/Averaging_Quaternions_and_Vectors
    val first = sq(0)._2.toDenseVector
    def inverseIfNotClose(x: Quat) =
      if (x.toDenseVector.dot(first) > 0.0)
        x
      else
        -x

    (sq
      .map(x => (x._1, inverseIfNotClose(x._2)))
      .foldLeft(Quaternion(0.0, 0.0, 0.0, 0.0))((acc, qw) =>
        acc + qw._2 / qw._1))
      .normalized
  }

  //TODO HANDLE DIFFERENT INITIAL POSSIBLE POSITIONS + resample + accelerometer for attitude + vicon + integrate acceleration
  lazy val particlesGyro: SourceT[Seq[State]] =
    source2
      .zipLastT(buffer)
      .mapT(x => x._2.map(b => State(b.w, sampleBR(b.q, x._1), b.br, b.p, b.v, b.a)))

  lazy val particlesAcc =
    source1
      .zipLastT(buffer)
      .mapT(_._2)

  lazy val fused =
    particlesGyro
      .fusion(particlesAcc)
      .mapT(resample)

  lazy val buffer: SourceT[Seq[State]] =
    Buffer(fused,
      Timestamped(Seq.fill(N)(State(1.0 / N, init, Vec3(), Vec3(), Vec3(), Vec3()))))

  lazy val filter: SourceT[Quat] =
    fused
      .mapT(_.map(x => (x.w, x.q)))
      .mapT(averageQuaternions)

  def genStream = filter.stream()

}
