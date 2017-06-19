package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg._

//https://ai2-s2-pdfs.s3.amazonaws.com/0322/8afc107f925b7a0ca77d5ade2fda9050f0a3.pdf
case class ParticleFilterMag2GPSAlt(rawSource1: Source[(Acceleration, Omega)],
                                    rawSource2: Source[Attitude],
                                    rawSource3: Source[Position],
                                    rawSource4: Source[Position],
                                    rawSource5: Source[AltitudeRay],
                                    N: Int,
                                    covAcc: Real,
                                    covGyro: Real,
                                    covMag: Real,
                                    covGPS1: Real,
                                    covGPS2: Real,
                                    varAlt: Real)(implicit val initHook: InitHook[TrajInit])
    extends Block5[(Acceleration, Omega), Attitude, Position, Position, AltitudeRay, (Position, Attitude)]
    with RequireInit[TrajInit]
    with ParticleFilter {

  def imu  = source1
  def mag  = source2
  def gps1 = source3
  def gps2 = source4
  def alt  = source5

  def name = "ParticleFilterMag2GPSAlt"

  object State {
    val Hpos = {
      val m  = DenseMatrix.zeros[Real](3, 6)
      m(::, 3 to 5) := eye(3)
      m
    }

    val Halt = {
      val m  = DenseMatrix.zeros[Real](1, 6)
      m(0, 5) = 1.0
      m      
    }

    val Rgps1 = eye(3)*covGPS1
    val Rgps2 = eye(3)*covGPS1
    val Ralt = eye(1)*varAlt
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

    def updatePosGPS1(pos: Position) = {

      val (nx, ncov, zsig) = KalmanFilter.update(x, cov, pos.toDenseMatrix.t, State.Hpos, State.Rgps1)

      (copy(
         x = nx,
         cov = ncov
       ),
       zsig)
    }

    def updatePosGPS2(pos: Position) = {

      val (nx, ncov, zsig) = KalmanFilter.update(x, cov, pos.toDenseMatrix.t, State.Hpos, State.Rgps2)

      (copy(
         x = nx,
         cov = ncov
       ),
       zsig)
    }

    def updateAlt(q: Quat, alt: AltitudeRay): (State, (VectorR, MatrixR))= {

      val p = q.getPitch
//      val d = traj.getPosition(t).z * cos(p)
      
      val tAlt: Real = ???
  //    val (nx, ncov, zsig) = KalmanFilter.update(x, cov, DenseMatrix((tAlt)), State.Halt, State.Ralt)

      ???
    }
    
  }

  def kalmanUpdatePosGPS1(ps: Particles, pos: Position) = {
    ps.copy(sp = ps.sp.map(x => {
      val (ns, (z, sig)) = x.s.updatePosGPS1(pos)
      val p              = Rand.gaussianLogPdf(pos, z, sig)
      x.copy(w = x.w + p, s = ns)
    }))
  }

  def kalmanUpdatePosGPS2(ps: Particles, pos: Position) = {
    ps.copy(sp = ps.sp.map(x => {
      val (ns, (z, sig)) = x.s.updatePosGPS2(pos)
      val p              = Rand.gaussianLogPdf(pos, z, sig)
      x.copy(w = x.w + p, s = ns)
    }))
  }

  def kalmanUpdateAlt(ps: Particles, alt: AltitudeRay) = {
    ps.copy(sp = ps.sp.map(x => {
      val (ns, (z, sig)) = x.s.updateAlt(x.q, alt)
      val p              = Rand.gaussianLogPdf(alt, z(0), sig(0, 0))
      x.copy(w = x.w + p, s = ns)
    }))
  }
  


  type Combined = Either[Either[Either[Either[(Acceleration, Omega), Attitude], Position], Position], AltitudeRay]

  def update(x: (Timestamped[Combined], Timestamped[Particles])): Particles = {
    val (Timestamped(t1, e, _), Timestamped(t2, ps, _)) = x

    val dt = t1 - t2

    e match {
      case Left(Left(Left(Left((acc, om))))) =>
        updateFromIMU(ps, acc, om, dt)

      case Left(Left(Left(Right(att)))) =>
        val psQ = updateAttitude(ps, dt)
        val psK = kalmanPredict(psQ, dt)
        updateWeightAtt(psK, att, eye(3) * covMag)

      case Left(Left(Right(pos))) =>
        val psK = kalmanPredict(ps, dt)
        kalmanUpdatePosGPS1(psK, pos)


      case Left(Right(pos)) =>
        val psK = kalmanPredict(ps, dt)
        kalmanUpdatePosGPS2(psK, pos)        

      case Right(alt) =>
        val psK = kalmanPredict(ps, dt)
        kalmanUpdateAlt(psK, alt)

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
    Particle(log(1.0 / N), init.q, State(initX, initS), Vec3())

  lazy val fused =
    imu
      .merge(mag)
      .merge(gps1)
      .merge(gps2)
      .merge(alt)

}
