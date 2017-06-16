package dawn.flow.trajectory

import dawn.flow._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import spire.algebra.Field
import breeze.linalg.{norm, normalize, cross, DenseMatrix, DenseVector, eig, eigSym, argmax}

case class AugmentedComplementaryFilter(rawSource1: Source[(Acceleration, Omega)],
                                        rawSource2: Source[(Position, Attitude)],
                                        initQ: Quat,
                                        alpha: Real)
    extends Block2[(Acceleration, Omega), (Position, Attitude), (Position, Attitude)] {

  def name = "ACF"

  case class State(v: Velocity, p: Position, q: Attitude, viconP: Timestamped[Position]) {
    def move(a: Acceleration, dt: Time) = {

      val nv = v + (a * dt)
      val np = p + (v * dt)

      copy(
        v = nv,
        p = np
      )

    }

  }

  def imu   = source1
  def vicon = source2

  def update(x: (Timestamped[Either[(Acceleration, Omega), (Position, Attitude)]], Timestamped[State])): State = {
    val (Timestamped(t1, e, _), Timestamped(t2, s, _)) = x

    val dt = t1 - t2
    //if dt is too small (< epsilon) then dividing by dt to get velocity make no sense
    val epsilon = 0.0001
    e match {

      case Left((acc, om)) =>
        val localQuat = Quat.localAngleToLocalQuat(om * dt)
        val nq        = localQuat.rotateBy(s.q)
        val accFixed  = nq.rotate(acc)
        s.copy(q = nq)
          .move(accFixed, dt)

      case Right((pos, att)) =>
        val velocity =
          if (dt < epsilon)
            s.v
          else {
            //CF between interpolation of vicon position and velocity from deack reckoning
            s.v * alpha + ((pos - s.viconP.v) / (t1 - s.viconP.t)) * (1 - alpha)
          }

        s.copy(
          v = velocity,
          p = pos,
          q = att,
          viconP = Timestamped(t1, pos)
        )
    }
  }

  lazy val upd =
    imu
      .merge(vicon)
      .zipLastT(state)
      .map(update)

  lazy val state: Source[State] =
    Buffer(upd, State(Vec3(), Vec3(), initQ, Timestamped(Vec3())), source1)
  lazy val out = upd.map(x => (x.p, x.q))

}
