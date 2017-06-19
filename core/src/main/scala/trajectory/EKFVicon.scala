package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg._

case class EKFVicon(rawSource1: Source[(Acceleration, Omega)],
                    rawSource2: Source[(Position, Attitude)],
                    N: Int,
                    covAcc: Real,
                    covGyro: Real,
                    covViconP: Real,
                    covViconQ: Real)(implicit val initHook: InitHook[TrajInit])
    extends Block2[(Acceleration, Omega), (Position, Attitude), (Position, Attitude)]
    with RequireInit[TrajInit] {

  def imu   = source1
  def vicon = source2

  def name = "EKFVicon"

  object State {
    val H = {
      val m = DenseMatrix.zeros[Real](7, 10)
      m(::, 3 to 9) := eye(7)
      m
    }

    def R(q: Quat) = {
      val m = eye(7)
      m(0 to 2, ::) *= covViconP
      m(3 to 6, 3 to 6) := eye(4)*0.0001 //I add a small noise. Could not find the correct form.
      m
    }
  }

  case class State(x: MatrixR, cov: MatrixR, lastA: Acceleration, lastO: Omega) {

    def v = x(0 to 2, 0).toDenseVector
    def p = x(3 to 5, 0).toDenseVector
    def q = x(6 to 9, 0).toDenseVector.toQuaternion

    def Q(q: Quat, dt: Time) = {
      val m             = DenseMatrix.zeros[Real](10, 10)
      val covAccFixedDt = q.rotationMatrix * q.rotationMatrix.t * (covAcc * (dt ** 2))
      m(0 to 2, 0 to 2) := covAccFixedDt
      //https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5108752/ should work but doesn't
      val gyroNoise = q.riemannMatrix*(eye(3)*covGyro)*q.riemannMatrix.t*((dt/2.0)**2)
      m(6 to 9, 6 to 9) := gyroNoise
      m
    }

    def F(dt: Time) = {
      val m = DenseMatrix.eye[Real](10)
      m(3, 0) = dt
      m(4, 1) = dt
      m(5, 2) = dt
      m
    }

    def predict(dt: Time) = {

      val nq = q*Quat.localAngleToLocalQuat(lastO * dt)
      val nv = v + nq.rotate(lastA)*dt
      val np = p + v*dt

      val nx = x.copy
      nx(0 to 2, ::) := nv.toDenseMatrix.t
      nx(3 to 5, ::) := np.toDenseMatrix.t
      nx(6 to 9, ::) := nq.toDenseVector.toDenseMatrix.t

      val f = F(dt)
      val ncov = f*cov*f.t + Q(nq, dt)

      copy(x = nx, cov = ncov)

    }

    def update(pos: Position, att: Attitude) = {

      val hx =  DenseMatrix.zeros[Real](7, 1)
      hx(0 to 2, ::) := p.toDenseMatrix.t
      hx(3 to 6, ::) := q.toDenseVector.toDenseMatrix.t

      val z = DenseMatrix.zeros[Real](7, 1)
      z(0 to 2, ::) := pos.toDenseMatrix.t
      z(3 to 6, ::) := att.toDenseVector.toDenseMatrix.t      

      val (nx, ncov, zsig) = KalmanFilter.extendedUpdate(x, cov, z, hx, State.H, State.R(q))

      copy(
         x = nx,
         cov = ncov
       )
    }
  }

  type Combined = Either[(Acceleration, Omega), (Position, Attitude)]

  def update(x: (Timestamped[Combined], Timestamped[State])): State = {
    val (Timestamped(t1, e, _), Timestamped(t2, s, _)) = x

    val dt = t1 - t2

    e match {
      case Left((acc, om)) =>
        s
          .copy(lastA = acc, lastO = om)
          .predict(dt)

      case Right((pos, att)) =>
        s
          .predict(dt)
          .update(pos, att)
    }
  }

  lazy val fused =
    imu
      .merge(vicon)

  lazy val out: Source[(Position, Quat)] =
    process
      .map(x => (x.p, x.q))

  lazy val process: Source[State] =
    fused
      .zipLastT(buffer)
      .map(update)

  lazy val buffer: Source[State] = {
    def initX: MatrixR =  {
      val m = DenseMatrix.zeros[Real](10, 1)
      m(0 to 2, ::) := init.v.toDenseMatrix.t
      m(3 to 5, ::) := init.p.toDenseMatrix.t
      m(6 to 9, ::) := init.q.toDenseVector.toDenseMatrix.t
      m
    }

    def initS =
      DenseMatrix.eye[Real](10) * (0.1**24)
      
    Buffer(process, State(initX, initS, init.a, Vec3()), source1)
  }

}
