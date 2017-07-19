package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg._

trait ParticleFilter {

  def N: Int
  def covGyro: Real
  def source1: Source[_]

  type Weight = Real

  type State <: {
    def x: MatrixR
    def cov: MatrixR
    def p: Position

    def predict(q: Quat, a: Acceleration, dt: Time): State    
  }


  case class Particle(w: Weight, q: Attitude, s: State, lastA: Acceleration, lastQ: Quat)
  case class Particles(sp: Seq[Particle], lastO: Omega)
  
  type Combined

  def update(x: (Timestamped[Combined], Timestamped[Particles])): Particles 

  def updateAttitude(ps: Particles, dt: Time) =
    ps.copy(sp = ps.sp.map(x => x.copy(q = sampleAtt(x.q, ps.lastO, dt))))

  def updateAcceleration(ps: Particles, acc: Acceleration) =
    ps.copy(sp = ps.sp.map(x => x.copy(lastA = x.q.rotate(acc), lastQ = x.q)))

  def kalmanPredict(ps: Particles, dt: Time) =
    ps.copy(sp = ps.sp.map(x => x.copy(s = x.s.predict(x.lastQ, x.lastA, dt))))
  
  def sampleAtt(q: Quat, om: Omega, dt: Time): Quat = {
    val withNoise  = Rand.gaussian(om, eye(3)*covGyro)
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
    val effective = ps.sp.count(x => exp(x.w) >= 1.0/N)
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
  def averageQuaternion(ps: Particles): Quat = {
    Quat.averageQuaternion(ps.sp.map(x => (exp(x.w), x.q)))
  }

  
  def updateFromIMU(ps: Particles, acc: Acceleration, om: Omega, dt: Time) = {
        val psO = ps.copy(lastO = om)
        val psUA = updateAttitude(psO, dt)
        val psA  = updateAcceleration(psUA, acc)
        kalmanPredict(psA, dt)
  }

  def updateWeightPos(ps: Particles, pos: Position, cov: MatrixR) =
    ps.copy(
      sp = ps.sp.map(x =>
        x.copy(w =
          x.w + posLogLikelihood(pos, x.s.p, cov))))

  def updateWeightAtt(ps: Particles, att: Attitude, cov: MatrixR) =
    ps.copy(
      sp = ps.sp.map(x =>
        x.copy(w =
          x.w + attLogLikelihood(att, x.q, cov))))

  def posLogLikelihood(measurement: Position, state: Position, cov: MatrixR) = {
    Rand.gaussianLogPdf(measurement, state, cov)
  }

  def attLogLikelihood(measurement: Attitude, state: Attitude, cov: MatrixR) = {
    val error = measurement.rotateBy(state.reciprocal)
    val rad   = Quat.quatToAngle(error)
    Rand.gaussianLogPdf(rad, Vec3(), cov)
  }
        
        

  def fused: Source[Combined]

  lazy val process: Source[Particles] =
    fused
      .zipLastT(buffer)
      .map(update, "Update")
      .map(normWeights, "NormWeight")
      .map(resample, "Resample")



  lazy val out: Source[(Position, Quat)] =
    process
      .map(x =>
             (
               averagePosition(x),
               averageQuaternion(x)
           ),
        "Average")

  def initP: Particle

  lazy val buffer: Source[Particles] = {
    Buffer(process, Particles(Seq.fill(N)(initP), Vec3()), source1)
  }
  
  
}
