package dawn.flow.trajectory

import dawn.flow._

import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg._

case class UKFVicon(rawSource1: Source[(Acceleration, Omega)],
                    rawSource2: Source[(Position, Attitude)],
                    covAcc: Real,
                    covGyro: Real,
                    covViconP: Real,
                    covViconQ: Real)(implicit val initHook: InitHook[TrajInit])
    extends Block2[(Acceleration, Omega), (Position, Attitude), (Position, Attitude)]
    with RequireInit[TrajInit] {



  val N = 10

  val ALPHA = 0.1 ** 3
  val KAPPA = 0
  val BETA  = 2

  val LAMBDA = (ALPHA ** 2) * (N + KAPPA) - N
  val w0m    = LAMBDA / (N + LAMBDA)
  val w0c    = LAMBDA / (N + LAMBDA) + (1 - ALPHA ** 2 + BETA)
  val wi     = 1 / (2 * (N + LAMBDA))

  val wms = Seq(w0m) ++ Seq.fill(2*N)(wi)
  val wcs = Seq(w0c) ++ Seq.fill(2*N)(wi)

  def imu   = source1
  def vicon = source2

  def name = "UKFVicon"

  case class State(x: MatrixR, cov: MatrixR, lastA: Acceleration, lastO: Omega) {

    def v = x(0 to 2, 0).toDenseVector
    def p = x(3 to 5, 0).toDenseVector
    def q = x(6 to 9, 0).toDenseVector.toQuaternion

    def predict(dt: Time): State = {
      val nx = x.copy
      nx(0 to 2, 0) += lastA * dt
      nx(3 to 5, 0) += v * dt
      nx(6 to 9, 0) := (q*Quat.localAngleToLocalQuat(lastO * dt)).toDenseVector
      copy(x = nx)
    }
  }


    def Q(q: Quat, dt: Time) = {
      val m             = DenseMatrix.zeros[Real](10, 10)
      val covAccFixedDt = q.rotationMatrix * q.rotationMatrix.t * (covAcc * (dt ** 2))
      m(0 to 2, 0 to 2) := covAccFixedDt
      //https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5108752/ should work but doesn't
      val gyroNoise = q.riemannMatrix*(eye(3)*covGyro)*q.riemannMatrix.t*((dt/2.0)**2)
      m(6 to 9, 6 to 9) := gyroNoise
      m
//      DenseMatrix.eye[Real](10)*0.001            
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
        m(3 to 6, 3 to 6) +=  ((lq - q.toDenseVector)*((lq - q.toDenseVector).t))*(1.0/6)
        
      }
//      println(m(3 to 6, 3 to 6))
      m
//      DenseMatrix.eye[Real](7)*0.001
    }
  


  type Combined = Either[(Acceleration, Omega), (Position, Attitude)]

  case class SigmaPoints(s: State, points: Seq[MatrixR]) {
    def predict(dt: Time): SigmaPoints              = {
      val nxs = points
        .map(p => s.copy(x = p))
        .map(_.predict(dt).x)

      val nx = sum(nxs.zip(wms).map(x => x._1*x._2))
      var ns = s.copy(x = nx)
      val ncov = sum(nxs.zip(wcs).map(x => ((x._1 - nx)*((x._1 - nx).t))*(x._2))) + Q(ns.q, dt)
      ns = ns.copy(cov = ncov)
      copy(points = nxs, s = ns)
    }

    def h(x: MatrixR) = x(3 to 9, ::)
    def update(pos: Position, att: Attitude): State = {
      val y = DenseVector.vertcat(pos, att.toDenseVector).toDenseMatrix.t
      val yks = points.map(h)
      val ym = sum(yks.zip(wms).map(x => x._1*x._2))
      val scov = sum(yks.zip(wcs).map(x => ((x._1 - ym)*((x._1 - ym).t))*(x._2))) + R(s.q)
      val xm = s.x
      val ccov = sum(points.zip(yks).zip(wcs).map(x => ((x._1._1 - xm)*((x._1._2 - ym).t))*(x._2)))
      val k = ccov*inv(scov)
      val nx = xm + k*(y - ym)
      val ncov = s.cov - k*scov*(k.t)
      s.copy(x = nx, cov = ncov)
    }


  }

  def toSigmaPoints(s: State): SigmaPoints = {
//    println(".....")
    val ch =
      if (s.cov == DenseMatrix.zeros[Real](N, N))
        DenseMatrix.zeros[Real](N, N)
      else {
        cholesky(s.cov)*sqrt((N + LAMBDA))
      }

    val m = s.x
    val cols = (0 until ch.cols).map(i => ch(::, i).toDenseMatrix.t)
    val xm = (0 until N).map(i => m - cols(i))
    val xp = (0 until N).map(i => m + cols(i))
    val xs = Seq(m) ++ xm ++ xp

    SigmaPoints(s, xs)
  }

  def update(x: (Timestamped[Combined], Timestamped[State])): State = {
    val (Timestamped(t1, e, _), Timestamped(t2, s, _)) = x

    val dt = t1 - t2

    e match {
      case Left((acc, om)) =>
        toSigmaPoints(
          s.copy(lastA = s.q.rotate(acc), lastO = om)
        )
          .predict(dt)
          .s

      case Right((pos, att)) =>
        toSigmaPoints(s)
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
    def initX: MatrixR = {
      val m = DenseMatrix.zeros[Real](N, 1)
      m(0 to 2, ::) := init.v.toDenseMatrix.t
      m(3 to 5, ::) := init.p.toDenseMatrix.t
      m(6 to 9, ::) := init.q.toDenseVector.toDenseMatrix.t
      m
    }

    def initS =
//      DenseMatrix.zeros[Real](N, N)
      DenseMatrix.eye[Real](N) * 0.001

    Buffer(process, State(initX, initS, init.a, Vec3()), source1)
  }

}

