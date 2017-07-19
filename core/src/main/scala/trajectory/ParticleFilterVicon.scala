package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg._

//https://ai2-s2-pdfs.s3.amazonaws.com/0322/8afc107f925b7a0ca77d5ade2fda9050f0a3.pdf
case class ParticleFilterVicon(rawSource1: Source[(Acceleration, Omega)],
                               rawSource2: Source[(Position, Attitude)],
                               N: Int,
                               covAcc: Real,
                               covGyro: Real,
                               covViconP: Real,
                               covViconQ: Real)(implicit val initHook: InitHook[TrajInit])
    extends Block2[(Acceleration, Omega), (Position, Attitude), (Position, Attitude)]
    with RequireInit[TrajInit]
    with ParticleFilter {

  def imu   = source1
  def vicon = source2

  def name = "ParticleFilterVicon"

  object State {
    val Hv = {
      val m = DenseMatrix.zeros[Real](3, 6)
      m(::, 3 to 5) := eye(3)
      m
    }
    val Rv = eye(3) * covViconP
  }

  case class State(x: MatrixR, cov: MatrixR) {

    def v = x(0 to 2, 0).toDenseVector
    def p = x(3 to 5, 0).toDenseVector

    def Q(q: Quat, dt: Time) = {
      val m             = DenseMatrix.zeros[Real](6, 6)
      val covAccFixedDt = q.rotationMatrix * q.rotationMatrix.t * (covAcc * (dt ** 2))
      m(0 to 2, 0 to 2) := covAccFixedDt
      m
    }

    def F(dt: Time) = {
      val m = DenseMatrix.eye[Real](6)
      m(3, 0) = dt
      m(4, 1) = dt
      m(5, 2) = dt
      m
    }

    def predict(q: Quat, a: Acceleration, dt: Time) = {

      val u = DenseMatrix.zeros[Real](6, 1)
      u(0 to 2, 0) := a * dt

      val (nx, ncov) = KalmanFilter.predict(x, cov, F(dt), u, Q(q, dt))

      copy(
        x = nx,
        cov = ncov
      )
    }

    def updatePos(pos: Position) = {

      val (nx, ncov, zsig) = KalmanFilter.update(x, cov, pos.toDenseMatrix.t, State.Hv, State.Rv)

      (copy(
         x = nx,
         cov = ncov
       ),
       zsig)
    }
  }


  def kalmanUpdatePos(ps: Particles, pos: Position) = {
    ps.copy(sp = ps.sp.map(x => {
      val (ns, (z, sig)) = x.s.updatePos(pos)
      val p              = posLogLikelihood(pos, z, sig)
      x.copy(w = x.w + p, s = ns)
    }))
  }

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
        val psUP = kalmanUpdatePos(psS, pos)
        updateWeightAtt(psUP, att, eye(3) * covViconQ)

    }
  }

  def initS =
    DenseMatrix.eye[Real](6) * (0.1 ** 24)

  def initX = {
    val m = DenseMatrix.zeros[Real](6, 1)
    m(0 to 2, ::) := init.v.toDenseMatrix.t
    m(3 to 5, ::) := init.p.toDenseMatrix.t
    m
  }
  def initP =
    Particle(log(1.0 / N), init.q, State(initX, initS), Vec3(), init.q)

  lazy val fused =
    imu
      .merge(vicon)

}
