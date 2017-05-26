package dawn.flow.trajectory

import dawn.flow._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross, DenseMatrix, eig, eigSym, argmax}
//https://pdfs.semanticscholar.org/480b/7477c76a1b2b2b130923f09a66ecdb0fb910.pdf

case class OrientationComplementaryFilter(source1: SourceT[Acceleration],
                                          source2: SourceT[BodyRates],
                                          source3: SourceT[Thrust],
                                          init: Quat,
                                          alpha: Real,
                                          dt: Timeframe)
    extends Op3T[Acceleration, BodyRates, Thrust, Quat] {

  val acc    = source1
  val gyro   = source2
  val thrust = source3

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
                          source3: SourceT[Thrust],
                          source4: SourceT[(Position, Quat)],
                          init: Quat,
                          dt: Timeframe,
                          N: Int,
                          bodyRateStd: DenseMatrix[Real])
    extends Op4T[Acceleration, BodyRates, Thrust, (Position, Quat), Quat] {

  def sampleBR(q: Quat, br: BodyRates): Quat = {
    val withNoise  = Rand.gaussian(br, bodyRateStd)
    val integrated = withNoise * dt
    val lq         = TQuaternion.localAngleToLocalQuat(q, integrated)
    lq.rotateBy(q)
  }

  //https://stackoverflow.com/questions/12374087/average-of-multiple-quaternions
  def averageQuaternions(sq: Seq[Quat]): Quat = {

    //Should be the right way but doesn't seem to work.
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
    val first = sq(0).toDenseVector
    def inverseIfNotClose(x: Quat) =
      if (x.toDenseVector.dot(first) > 0.0)
        x
      else
        -x

    (sq.map(inverseIfNotClose).reduce(_ + _) / N).normalized
  }

  //TODO HANDLE DIFFERENT INITIAL POSSIBLE POSITIONS
  lazy val particles: SourceT[Seq[Quat]] =
    source2.zipT(buffer).mapT(x => x._2.map(sampleBR(_, x._1)))
  lazy val buffer: SourceT[Seq[Quat]] =
    Buffer(particles, Timestamped(Seq.fill(N)(init)))
  lazy val filter: SourceT[Quat] = particles.mapT(averageQuaternions)

  def genStream = filter.stream()

}
