package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg._

//TODO find why getting NAN sometimes if first IMU before  first vicon
case class EKFVicon(rawSource1: Source[(Acceleration, Omega)],
                    rawSource2: Source[(Position, Attitude)],
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

  val sqrtViconQ = sqrt(covViconQ*3)    
    def R(q: Quat) = {
      val m = DenseMatrix.zeros[Real](7, 7)
      m(0 to 2, 0 to 2) := DenseMatrix.eye[Real](3) * covViconP
      
      for (i <- (0 to 5)) {
        val dv = DenseVector.zeros[Real](3)
        if (i >= 3)
          dv(i-3) = -sqrtViconQ
        else
          dv(i) = sqrtViconQ
        
        val lq = (q*Quat.localAngleToLocalQuat(dv)).toDenseVector
        m(3 to 6, 3 to 6) +=  ((lq - q.toDenseVector)*(lq - q.toDenseVector).t)*(1.0/6)
      }
//      println(m(3 to 6, 3 to 6))
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
      val wx = lastO(0)
      val wy = lastO(1)
      val wz = lastO(2)            
      //auto generated from sage
      val n = sqrt(wx**2 + wy**2 + wz**2)
      val sinn =
        if (n < 0.000001)
          1.0
        else
          sin(1.0/2*n*dt)/n

      m(6, 6) = cos(1.0/2**dt)      
      m(7, 6) = wx*sinn
      m(8, 6) = wy*sinn
      m(9, 6) = wz*sinn
      m(6, 7) = -wx*sinn
      m(7, 7) = cos(1.0/2*n*dt)
      m(8, 7) = -wz*sinn
      m(9, 7) = wy*sinn
      m(6, 8) = -wy*sinn
      m(7, 8) = wz*sinn
      m(8, 8) = cos(1.0/2*n*dt)
      m(9, 8) = -wx*sinn
      m(6, 9) = -wz*sinn
      m(7, 9) = -wy*sinn
      m(8, 9) = wx*sinn
      m(9, 9) = cos(1.0/2*n*dt)
      //
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


      //      println(nx(0, 0))
//      println(f.toString(1000, 1000))
//      println((f*cov*f.t).toString(1000, 1000))
      

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

//    println(t1, t2, e.toString.take(10))
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
      .map(x => (x.p, x.q), "P & Q")

  lazy val process: Source[State] =
    fused
      .zipLastT(buffer)
      .map(update, "Update")

  lazy val buffer: Source[State] = {
    def initX: MatrixR =  {
      val m = DenseMatrix.zeros[Real](10, 1)
      m(0 to 2, ::) := init.v.toDenseMatrix.t
      m(3 to 5, ::) := init.p.toDenseMatrix.t
      m(6 to 9, ::) := init.q.toDenseVector.toDenseMatrix.t
      m
    }

    def initS =
      DenseMatrix.eye[Real](10) * (0.1**3)
      
    Buffer(process, State(initX, initS, init.a, Vec3()), source1)
  }

}

