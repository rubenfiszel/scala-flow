package dawn.flow.trajectory

import dawn.flow._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross, DenseMatrix, DenseVector, eig, eigSym, argmax}

case class AugmentedComplementaryFilter(rawSource1: Source[Acceleration],
                                        rawSource2: Source[Omega],
                                        rawSource3: Source[(Position, Attitude)],
                                        rawSource4: Source[Thrust],
                                        rawSource5: Source[Omega],
                                        initQ: Quat,
                                        alpha: Real)
    extends Block5[Acceleration, Omega, (Position, Attitude), Thrust, Omega, (Position, Attitude)] {

  def name = "ACF"

  type State = (Velocity, Position, Attitude, (Position, Time))

  def acc     = source1
  def omegaG  = source2
  def vicon   = source3
  def thrustC = source4
  def omegaC  = source5


  def update(x: (Timestamped[Either[((Acceleration, Thrust), Omega), (Position, Attitude)]], Timestamped[State])): State = {
    val (Timestamped(t1, e, _), Timestamped(t2, s, _)) = x

    val dt = t1 - t2
    //if dt is too small (< epsilon) then dividing by dt to get velocity make no sense
    val epsilon = 0.0001
    e match {
//      case Left(Left(acc, t)) => (s._1 + (acc * dt), s._2 + (s._1 * dt))
      case Left(((a, th), om)) =>
        val localQuat = Quat.localAngleToLocalQuat(om * dt)
        val nAttGyro = localQuat.rotateBy(s._3)
        val nAttAcc =
          Quat.getQuaternion(
            a - Vec3(0, 0, 1) * th, //Remove thrust from acceleration to retrieve gravity
            Vec3(0, 0, -1))
        val cf = nAttGyro * alpha + nAttAcc * (1-alpha)
        val af = toFixed(a, cf)
        (s._1 + (af * dt), s._2 + (s._1 * dt), cf, s._4)
      case Right((pos, att)) =>
        val v = 
          if (dt < epsilon)
            s._1
          else {
//            println((pos - s._2) / dt, pos - s._2, dt)
            (pos - s._4._1) / (t1 - s._4._2)
          }
        (v, pos, att, (pos, t1))
      case _ => s
    }
  }

  def toFixed(x: (Acceleration, Quat)) =
    x._2.rotate(x._1)


  lazy val omega =
    omegaG
      .zip(omegaC)
      .map(x => (x._1 + x._2)/2.0)
      .bufferWithTime(Vec3())

  lazy val upd =
    acc
      .zip(thrustC)
      .zip(omega)
      .merge(vicon)
      .zipT(state)
      .map(update)

  lazy val state: Source[State] = Buffer(upd, (Vec3(), Vec3(), initQ, (Vec3(), 0.0)), source1)
  lazy val out                  = upd.map(x => (x._2, x._3))

}
