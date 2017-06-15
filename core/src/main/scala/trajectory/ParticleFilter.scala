package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross, DenseMatrix, DenseVector, eig, eigSym, argmax}

//https://ai2-s2-pdfs.s3.amazonaws.com/0322/8afc107f925b7a0ca77d5ade2fda9050f0a3.pdf
case class ParticleFilter(rawSource1: Source[Acceleration],
                          rawSource2: Source[Omega],
                          rawSource3: Source[(Position, Attitude)],
                          rawSource4: Source[Thrust],
                          rawSource5: Source[Omega],
                          init: Quat,
                          N: Int,
                          covAcc: MatrixR,
                          covGyro: MatrixR,
                          covViconP: MatrixR,
                          covViconQ: MatrixR,
                          stdCIThrust: Real,
                          covCIOmega: MatrixR)
    extends Block5[Acceleration, Omega, (Position, Attitude), Thrust, Omega, (Position, Attitude)] {

  def acceleration = source1
  def bodyRate     = source2
  def vicon        = source3
  def thrustC      = source4
  def omegaC       = source5

  def name = "ParticleFilter"

  type Weight = Real

  case class State(p: Position, v: Velocity, a: Acceleration)
  case class Particle(w: Weight, q: Attitude, s: State)

  def sampleBR(q: Quat, br: Omega, dt: Time): Quat = {
    val withNoise  = Rand.gaussian(br, covCIOmega)
    val integrated = withNoise * dt
    val lq         = Quat.localAngleToLocalQuat(integrated)
    lq.rotateBy(q)
  }

  //http://users.isy.liu.se/rt/schon/Publications/HolSG2006.pdf
  //See systematic resampling
  def resample(sb: Seq[Particle]) = {
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
      .foldLeft(Quaternion(0.0, 0.0, 0.0, 0.0))((acc, qw) => acc + qw._2 / qw._1))
      .normalized
  }

  //TODO HANDLE DIFFERENT INITIAL POSSIBLE POSITIONS + resample + accelerometer for attitude + vicon + integrate acceleration
  lazy val particlesGyro: Source[Seq[Particle]] =
    bodyRate
      .zipLastT(buffer)
      .map(x => x._2.v.map(b => b.copy(q = sampleBR(b.q, x._1.v, x._1.t - x._2.t ))), "Sample")

  lazy val particlesAcc =
    acceleration
//      .zip(controlInput)
      .divider(100)
      .latency(0.0001)
      .zipLast(buffer)
      .map(_._2, "2nd")

  lazy val fused =
    particlesGyro
      .fusion(particlesAcc)
      .map(resample, "Resample")

  lazy val buffer: Source[Seq[Particle]] =
    Buffer(fused, Seq.fill(N)(Particle(1.0 / N, init, State(Vec3(), Vec3(), Vec3()))), source1)

  lazy val out: Source[(Position, Quat)] =
    vicon.map(_._1).zip(
    fused
      .map(_.map(x => (x.w, x.q)), "toW-Q")
      .map(averageQuaternions, "average"))

}
