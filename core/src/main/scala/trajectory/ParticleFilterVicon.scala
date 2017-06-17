package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg._

//https://ai2-s2-pdfs.s3.amazonaws.com/0322/8afc107f925b7a0ca77d5ade2fda9050f0a3.pdf
case class ParticleFilterVicon(rawSource1: Source[(Acceleration, Omega)],
                               rawSource2: Source[(Position, Attitude)],
                               init: Quat,
                               N: Int,
                               covAcc: MatrixR,
                               covGyro: MatrixR,
                               covViconP: MatrixR,
                               covViconQ: MatrixR)
    extends Block2[(Acceleration, Omega), (Position, Attitude), (Position, Attitude)]
    with ParticleFilter {

  def imu   = source1
  def vicon = source2

  def name = "ParticleFilterVicon"

  object State {
    val Hv = {
      val id = DenseMatrix.eye[Real](3)
      val m  = DenseMatrix.zeros[Real](3, 6)
      m(::, 3 to 5) := id
      m
    }
    val Rv = covViconP
  }

  case class State(x: MatrixR, cov: MatrixR) {

    def v = x(0 to 2, 0).toDenseVector
    def p = x(3 to 5, 0).toDenseVector

    def Q(dt: Time) = {
      val m = DenseMatrix.zeros[Real](6, 6)
      m(0 to 2, 0 to 2) := covViconP * (dt ** 2)
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
      u(0 to 2, 0) := a * dt

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

  def kalmanUpdate(ps: Particles, pos: Position) =
    ps.copy(sp = ps.sp.map(x => x.copy(s = x.s.update(pos))))


  type Combined = Either[(Acceleration, Omega), (Position, Attitude)]

  def update(x: (Timestamped[Combined], Timestamped[Particles])): Particles = {
    val (Timestamped(t1, e, _), Timestamped(t2, ps, _)) = x

    val dt = t1 - t2

    e match {
      case Left((acc, om)) =>
        updateFromIMU(ps, acc, om, dt)

      case Right((pos, att)) =>
        val psQ  = updateAttitude(ps, dt)
        val psS  = kalmanPredict(psQ, dt)
        val psSP = kalmanUpdate(psS, pos)
        val psWP = updateWeightPos(psSP, pos, covViconP)
        updateWeightAtt(psWP, att, covViconQ)        

    }
  }


  lazy val buffer: Source[Particles] = {
    val initP =
      Particle(log(1.0 / N), init, State(DenseMatrix.zeros[Real](6, 1), DenseMatrix.eye[Real](6) * 0.001), Vec3())
    Buffer(process, Particles(Seq.fill(N)(initP), Vec3()), source1)
  }

  lazy val fused =
    imu
      .merge(vicon)

}
