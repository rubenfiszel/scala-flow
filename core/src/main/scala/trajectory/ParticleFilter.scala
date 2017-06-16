package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross, DenseMatrix, DenseVector, eig, eigSym, argmax, inv}

//https://ai2-s2-pdfs.s3.amazonaws.com/0322/8afc107f925b7a0ca77d5ade2fda9050f0a3.pdf
case class ParticleFilter(rawSource1: Source[(Acceleration, Omega)],
                          rawSource2: Source[(Position, Attitude)],
                          init: Quat,
                          N: Int,
                          covAcc: MatrixR,
                          covGyro: MatrixR,
                          covViconP: MatrixR,
                          covViconQ: MatrixR)
    extends Block2[(Acceleration, Omega), (Position, Attitude), (Position, Attitude)] {

  def imu   = source1
  def vicon = source2

  def name = "ParticleFilter"

  type Weight = Real

  object State {
    val Hv = {
      val id = DenseMatrix.eye[Real](3)
      val m = DenseMatrix.zeros[Real](3, 6)
      m(::,3 to 5) := id
      m
    }
    val Rv = covViconP

  }

  case class State(x: MatrixR, cov: MatrixR) {

    def v = x(0 to 2, 0).toDenseVector
    def p = x(3 to 5, 0).toDenseVector

    def Q(dt: Time) = {
      val m = DenseMatrix.zeros[Real](6, 6)
      m(0 to 2, 0 to 2) := covViconP*(dt**2)
      m
    }

    def F(dt: Time) = {
      val m = DenseMatrix.eye[Real](6)
      m(3, 0) = dt
      m(4, 1) = dt
      m(5, 2) = dt
      m
    }

    def predict(a: Acceleration, dt: Time) = {

      val u = DenseMatrix.zeros[Real](6, 1)
      u(0 to 2, 0) := a*dt

      val (nx, ncov) = KalmanFilter.predict(x, cov, F(dt), u, Q(dt))

      copy(
        x = nx,
        cov = ncov
      )
    }

    def update(pos: Position) = {

      val (nx, ncov) = KalmanFilter.update(x, cov, pos.toDenseMatrix.t, State.Hv, State.Rv)

      copy(
        x = nx,
        cov = ncov
      )
    }
  }
  case class Particle(w: Weight, q: Attitude, s: State, lastA: Acceleration)
  case class Particles(sp: Seq[Particle], lastO: Omega)

  def sampleAtt(q: Quat, br: Omega, dt: Time): Quat = {
    val withNoise  = Rand.gaussian(br, covGyro)
    val integrated = withNoise * dt
    val lq         = Quat.localAngleToLocalQuat(integrated)
    lq.rotateBy(q)
  }

  def sampleAcc(acc: Acceleration): Acceleration = {
    Rand.gaussian(acc, covAcc)
  }

  def updateAttitude(ps: Particles, dt: Time) =
    ps.copy(sp = ps.sp.map(x =>
      x.copy(q = sampleAtt(x.q, ps.lastO, dt))
    ))

  def updateAcceleration(ps: Particles, acc: Acceleration) = 
    ps.copy(sp = ps.sp.map(x => x.copy(lastA = 
      x.q.rotate(acc)
    )))

  def kalmanPredict(ps: Particles, dt: Time) =
    ps.copy(sp = ps.sp.map(x => x.copy(s = 
      x.s.predict(x.lastA, dt)
    )))

  def kalmanUpdate(ps: Particles, pos: Position) =
    ps.copy(sp = ps.sp.map(x => x.copy(s = 
      x.s.update(pos)
    )))
  
  def updateWeightPosAtt(ps: Particles, pos: Position, att: Attitude) =
    ps.copy(
      sp = ps.sp.map(x => x.copy(w =
        x.w + posLogLikelihood(x.s.p, pos) + attLogLikelihood(x.q, att)
      )))


  def update(
      x: (Timestamped[Either[(Acceleration, Omega), (Position, Attitude)]], Timestamped[Particles])): Particles = {
    val (Timestamped(t1, e, _), Timestamped(t2, ps, _)) = x

    val dt = t1 - t2

    e match {
      case Left((acc, om)) =>

        val psO = ps.copy(
          lastO = om
        )
        val psUA = updateAttitude(psO, dt)
        val psA = updateAcceleration(psUA, acc)
        kalmanPredict(psA, dt)
       

      case Right((pos, att)) =>
        val psQ = updateAttitude(ps, dt)        
        val psS = kalmanPredict(psQ, dt)
        val psSP = kalmanUpdate(psS, pos)
        updateWeightPosAtt(psSP, pos, att)


    }
  }

  def posLogLikelihood(state: Position, measurement: Position) = {
    Rand.gaussianLogPdf(measurement, state, covViconP)
  }

  def attLogLikelihood(state: Attitude, measurement: Attitude) = {
    val error = measurement.rotateBy(state.reciprocal)
    val rad   = Quat.quatToAngle(error)
    Rand.gaussianLogPdf(rad, Vec3(), covViconQ)
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

  def tooLowEffective(ps: Particles) = {
    val effective = ps.sp.count(_.w > log(1.0 / N))
    val ratio     = (effective / N.toDouble)
    ratio <= 0.1
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

  def averagePosition(ps: Particles): Position = {
    var r = Vec3()
    ps.sp
      .map(x => x.s.p * exp(x.w))
      .foreach(x => r += x)
    r
  }

  lazy val fused =
    imu
      .merge(vicon)

  lazy val process: Source[Particles] =
    fused
      .zipLastT(buffer)
      .map(update)
      .map(normWeights)
      .map(resample, "Resample")

  lazy val buffer: Source[Particles] = {
    val initP = Particle(log(1.0 / N), init, State(DenseMatrix.zeros[Real](6, 1), DenseMatrix.eye[Real](6) * 0.001), Vec3())
    Buffer(process, Particles(Seq.fill(N)(initP), Vec3()), source1)
  }

  lazy val out: Source[(Position, Quat)] =
    process
      .map(x =>
             (
               averagePosition(x),
               averageQuaternions(x)
           ),
           "average")

}
