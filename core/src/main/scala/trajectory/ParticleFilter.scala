package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg._

trait ParticleFilter {

  def N: Int
  def covGyro: MatrixR

  type Weight = Real

  type State <: {
    def x: MatrixR
    def cov: MatrixR

    def p: Position

    def Q(dt: Time): MatrixR
    def F(dt: Time): MatrixR

    def predict(a: Acceleration, dt: Time): State    
  }

  case class Particle(w: Weight, q: Attitude, s: State, lastA: Acceleration)
  case class Particles(sp: Seq[Particle], lastO: Omega)
  
  type Combined

  def update(x: (Timestamped[Combined], Timestamped[Particles])): Particles 

  def updateAttitude(ps: Particles, dt: Time) =
    ps.copy(sp = ps.sp.map(x => x.copy(q = sampleAtt(x.q, ps.lastO, dt))))

  def updateAcceleration(ps: Particles, acc: Acceleration) =
    ps.copy(sp = ps.sp.map(x => x.copy(lastA = x.q.rotate(acc))))

  def kalmanPredict(ps: Particles, dt: Time) =
    ps.copy(sp = ps.sp.map(x => x.copy(s = x.s.predict(x.lastA, dt))))
  
  def sampleAtt(q: Quat, om: Omega, dt: Time): Quat = {
    val withNoise  = Rand.gaussian(om, covGyro)
    val integrated = withNoise * dt
    val lq         = Quat.localAngleToLocalQuat(integrated)
    lq.rotateBy(q)
  }
  
  //http://timvieira.github.io/blog/post/2014/02/11/exp-normalize-trick/
  def normWeights(ps: Particles) = {
    val ws  = ps.sp.map(_.w)
    val b   = ws.max
    val sum = b + log(ws.map(x => exp(x - b)).sum)
    ps.copy(
      sp = ps.sp.map(p => p.copy(w = p.w - sum))
    )
  }

  //http://users.isy.liu.se/rt/schon/Publications/HolSG2006.pdf
  //See systematic resampling
  def resample(ps: Particles) = {
    if (tooLowEffective(ps)) {
      val u  = Rand.uniform()
      val us = Array.tabulate(N)(i => (i + u) / N)
      val ws = ps.sp.map(x => exp(x.w)).scanLeft(0.0)(_ + _).drop(1)
      val ns = Array.fill(N)(0)
      var cu = 0
      var wu = 0
      for (w <- ws) {
        while (cu < N && us(cu) <= w) {
          ns(wu) += 1
          cu += 1
        }
        wu += 1
      }
      ps.copy(sp = ps.sp.zip(ns).flatMap(x => List.fill(x._2)(x._1)).map(_.copy(w = log(1.0 / N))))
    } else
      ps
  }
  

  def tooLowEffective(ps: Particles) = {
    val effective = ps.sp.count(_.w > log(1.0 / N))
    val ratio     = (effective / N.toDouble)
    ratio <= 0.1
  }
  
  def averagePosition(ps: Particles): Position = {
    var r = Vec3()
    ps.sp
      .map(x => x.s.p * exp(x.w))
      .foreach(x => r += x)
    r
  }

  //https://stackoverflow.com/questions/12374087/average-of-multiple-quaternions
  def averageQuaternions(ps: Particles): Quat = {

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
    val first = ps.sp(0).q.toDenseVector
    def inverseIfNotClose(x: Quat) =
      if (x.toDenseVector.dot(first) > 0.0)
        x
      else
        -x

    (ps.sp
      .map(x => (exp(x.w), inverseIfNotClose(x.q)))
      .foldLeft(Quaternion(0.0, 0.0, 0.0, 0.0))((acc, qw) => acc + qw._2 / qw._1))
      .normalized
  }

  def updateFromIMU(ps: Particles, acc: Acceleration, om: Omega, dt: Time) = {
        val psO = ps.copy(
          lastO = om
        )
        val psUA = updateAttitude(psO, dt)
        val psA  = updateAcceleration(psUA, acc)
        kalmanPredict(psA, dt)
  }

  def updateWeightPos(ps: Particles, pos: Position, cov: MatrixR) =
    ps.copy(
      sp = ps.sp.map(x =>
        x.copy(w =
          x.w + posLogLikelihood(x.s.p, pos, cov))))

  def updateWeightAtt(ps: Particles, att: Attitude, cov: MatrixR) =
    ps.copy(
      sp = ps.sp.map(x =>
        x.copy(w =
          x.w + attLogLikelihood(x.q, att, cov))))

  def posLogLikelihood(state: Position, measurement: Position, cov: MatrixR) = {
    Rand.gaussianLogPdf(measurement, state, cov)
  }

  def attLogLikelihood(state: Attitude, measurement: Attitude, cov: MatrixR) = {
    val error = measurement.rotateBy(state.reciprocal)
    val rad   = Quat.quatToAngle(error)
    Rand.gaussianLogPdf(rad, Vec3(), cov)
  }
        
        

  def fused: Source[Combined]

  lazy val process: Source[Particles] =
    fused
      .zipLastT(buffer)
      .map(update)
      .map(normWeights)
      .map(resample, "Resample")

  def buffer: Source[Particles]

  lazy val out: Source[(Position, Quat)] =
    process
      .map(x =>
             (
               averagePosition(x),
               averageQuaternions(x)
           ),
           "average")
  
}
