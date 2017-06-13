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

  type State = (Velocity, Position)

  def acc     = source1
  def omegaG  = source2
  def vicon   = source3
  def thrustC = source4
  def omegaC  = source5

  lazy val (posV, attV) = vicon.unzip2

  def update(x: (Timestamped[Either[Acceleration, Position]], Timestamped[State])): State = {
    val (Timestamped(t1, e, _), Timestamped(t2, s, _)) = x

    val dt = t1 - t2
    //if dt is too small (< epsilon) then dividing by dt to get velocity make no sense
    val epsilon = 0.0001
    e match {
      case Left(acc) => (s._1 + (acc * dt), s._2 + (s._1 * dt))
      case Right(pos) => (s._1, pos)
//      case Right(pos) if dt < epsilon => (s._1, pos)
//      case Right(pos) => ((pos - s._2) / dt, pos)
    }
  }

  def toFixed(x: (Acceleration, Quat)) =
    x._2.rotate(x._1)

  lazy val accF =
    acc
      .zipLast(cf)
      .map(toFixed)


  lazy val upd =
    accF
      .merge(posV.filter(_ => false))
      .zipT(state)
      .map(update)

  lazy val state: Source[State] = Buffer(upd, (Vec3(), Vec3()), source1)
  lazy val cf                   = OrientationComplementaryFilter(acc, omegaG, thrustC, initQ, alpha)
  lazy val out                  = upd.map(_._2).zipLast(cf)

}
