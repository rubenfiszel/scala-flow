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

//https://ai2-s2-pdfs.s3.amazonaws.com/0322/8afc107f925b7a0ca77d5ade2fda9050f0a3.pdf
case class ParticleFilter(rawSource1: Source[Acceleration],
                          rawSource2: Source[BodyRates],
                          rawSource3: Source[(Thrust, BodyRates)],
                          rawSource4: Source[(Position, Quat)],
                          init: Quat,
                          dt: Timeframe,
                          N: Int,
                          bodyRateStd: DenseMatrix[Real])
    extends Block4[Acceleration,
                   BodyRates,
                   (Thrust, BodyRates),
                   (Position, Quat),
                   Quat] {

  def name = "ParticleFilter"
  type Weight = Real
  case class State(w: Weight,
                   q: Quat,
                   br: BodyRates,
                   p: Position,
                   v: Velocity,
                   a: Acceleration)

  def sampleBR(q: Quat, br: BodyRates): Quat = {
    val withNoise = Rand.gaussian(br, bodyRateStd)
    val integrated = withNoise * dt
    val lq = TQuaternion.localAngleToLocalQuat(q, integrated)
    lq.rotateBy(q)
  }

  //http://users.isy.liu.se/rt/schon/Publications/HolSG2006.pdf
  //See systematic resampling
  def resample(sb: Seq[State]) = {
    val u = Rand.uniform()
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
  lazy val particlesGyro: Source[Seq[State]] =
    source2
      .zipLast(buffer)
      .map(x =>
        x._2.map(b => State(b.w, sampleBR(b.q, x._1), b.br, b.p, b.v, b.a)), "Sample")

  lazy val particlesAcc =
    source1
      .latency(0.0001)
      .zipLast(buffer)
      .map(_._2, "2nd")

  lazy val fused =
    particlesGyro
      .fusion(particlesAcc)
      .map(resample, "Resample")

  lazy val buffer: Source[Seq[State]] =
    Buffer(fused,
           Seq.fill(N)(State(1.0 / N, init, Vec3(), Vec3(), Vec3(), Vec3())),
           source1)

  lazy val out: Source[Quat] =
    fused
      .map(_.map(x => (x.w, x.q)), "toW-Q")
      .map(averageQuaternions, "average")

}
