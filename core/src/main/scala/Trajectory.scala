package dawn.flow

import io.circe.generic.JsonCodec
import scala.collection.GenSeq
import breeze.linalg.{max => _, min => _, _ => _}


trait Trajectory {

  def tf: Timeframe
  def keypoints: List[(Keypoint, Timeframe)]

  def getPosition(t: Time): Position

  def getVelocity(t: Time): Velocity 

  def getAcceleration(t: Time): Acceleration 

  def getJerk(t: Time): Jerk 

  def getNormalVector(t: Time): NormalVector 

  def getThrust(t: Time): Thrust

  def getBodyRates(t: Time, dt: Timestep): BodyRates
  def getBodyRates(t: Time): BodyRates =
    getBodyRates(t, 1e-3)  

  def getPoint(t: Time, dt: Timestep): TrajectoryPoint
  def getPoint(t: Time): TrajectoryPoint =
    getPoint(t, 1e-3)

}

object TrajectoryPointPulse extends ((Trajectory, Time) => Timestamped[TrajectoryPoint]) {
  override def toString = getClass.getSimpleName
  def apply(traj: Trajectory, t: Time) = Timestamped(t, traj.getPoint(t))
}

case object KeypointSource extends Source[Timestamped[Keypoint], Trajectory] {
  def sources = List()
  def stream(traj: Trajectory) = {
    var ts = 0.0
    traj.keypoints.map { case (kp, tf) => {ts += tf; Timestamped(ts, kp) } }.toStream
  }
}

case class TrajectoryClock(dt: Timestep) extends Op1[Time, Trajectory, Time]{
  val source = Clock(dt)
  def stream(p: Trajectory) = source.takeWhile(_ < p.tf).stream(null)  
}


@JsonCodec
case class Vec3(x: Real, y: Real, z: Real)
    extends DenseVector[Real](Array(x, y, z))

object Vec3 {
  def zero = Vec3(0, 0, 0)
  def one  = Vec3(1, 1, 1)

  def apply(gs: GenSeq[Real]): Vec3 =
    Vec3(gs(0), gs(1), gs(2))
}

@JsonCodec
case class TrajectoryPoint(p: Vec3,
                           v: Vec3,
                           a: Vec3,
                           j: Vec3,
                           nv: Vec3,
                           t: Thrust,
                           br: Vec3)


case class Init(p: Vec3, v: Vec3, a: Vec3) {
  def apply(i: Int) = SingleAxisInit(p(i), v(i), a(i))
}

object Init {
  def zero = Init(Vec3.zero, Vec3.zero, Vec3.zero)
}

@JsonCodec
case class Keypoint(p: Option[Vec3], v: Option[Vec3], a: Option[Vec3]) {
  def apply(i: Int) = SingleAxisGoal(p.map(_(i)), v.map(_(i)), a.map(_(i)))
}

object Keypoint {
  def apply(p: Vec3): Keypoint = Keypoint(Some(p), None, None)
  def one = Keypoint(Some(Vec3.one), Some(Vec3.one), Some(Vec3.one))
}

case class SingleAxisInit(p: Real, v: Real, a: Real)
case class SingleAxisGoal(p: Option[Real], v: Option[Real], a: Option[Real])
