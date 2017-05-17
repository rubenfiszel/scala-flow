package dawn.flow.trajectory

import dawn.flow._
import io.circe.generic.JsonCodec
import breeze.linalg.{max => _, min => _, _ => _}
import spire.math.{Real => _, _ => _}
import spire.implicits._

object Trajectory {

  def getQuaternion(v1: Vec3, v2: Vec3) = {
    //http://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
    val a     = Vec3(cross(v1, v2))
    val w     = sqrt((norm(v1) ** 2) * (norm(v2) ** 2)) + v1.dot(v2)
    Quaternion(w, a.x, a.y, a.z).normalized
  }

  //http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle/index.htm
  def quatToBodyRate(qstart: Quaternion[Real], qend: Quaternion[Real]) = {
    val q     = qstart.reciprocal * qend
    val v     = Vec3(q.i, q.j, q.k)
    val angle = 2 * acos(q.r)
    Vec3(normalize(v)*angle)
  }

  //https://www.astro.rug.nl/software/kapteyn/_downloads/attitude.pdf page 19
  def bodyRateToQuat(q: Quaternion[Real], omega: Vec3): Quaternion[Real] = {
    val omegaQ = Quaternion(0.0, omega.x, omega.y, omega.z) * (1.0 / 2)
    q * omegaQ
  }

}
trait Trajectory {

  def tf: Timeframe
  def keypoints: List[(Keypoint, Timeframe)]
  def gravity: Vec3

  def getPosition(t: Time): Position

  def getVelocity(t: Time): Velocity

  def getAcceleration(t: Time): Acceleration

  //Acceleration + G
  def getFullAcceleration(t: Time): Acceleration =
    Vec3(gravity)
//    Vec3(getAcceleration(t) + gravity)

  //Acceleration in the local referential of the drone
  def getFullLocalAcceleration(t: Time) = {
    getOrientationQuaternion(t).reciprocal
      .rotate(getFullAcceleration(t))

  }

  def getJerk(t: Time): Jerk

  //NormalVector is oriented toward acceleration - G
  //because it is assumed that acceleration direction is the goal
  //and you need to retrieve it after applying G
  def getNormalVector(t: Time): NormalVector

  def getThrust(t: Time): Thrust

  //The quaternion is not unique but we choose the "shortest arc".
  def getOrientationQuaternion(t: Time) = {
    require(t < tf)
    Trajectory.getQuaternion(
      Vec3(0, 0, 1),
      getNormalVector(t)
    )
  }

  //avoid end of trajectory sampling annoyance
  //by "replaying" the last possible sample
  def lastPossibleDelta(t: Time, dt: Timestep) =
      if ((t + 1.1*dt) > tf)
        tf - 1.1*dt
      else
        t

  def getQuaternionRotation(t: Time, dt: Timestep) = {
    val rt = lastPossibleDelta(t, dt)
    getOrientationQuaternion(rt + dt)
      .rotateBy(getOrientationQuaternion(rt).reciprocal)
  }

  def getBodyRates(t: Time, dt: Timestep): Vec3 = {
    val rt = lastPossibleDelta(t, dt)
    Trajectory.quatToBodyRate(getOrientationQuaternion(rt),
                              getOrientationQuaternion(rt + dt)) / dt
  }

  def getPoint(t: Time, dt: Timestep = 1e-3): TrajectoryPoint =
    TrajectoryPoint(getPosition(t),
                    getVelocity(t),
                    getFullLocalAcceleration(t),
                    getJerk(t),
                    getNormalVector(t),
                    getOrientationQuaternion(t),
                    getThrust(t),
                    getBodyRates(t, dt))

}

object TrajectoryPointPulse
    extends ((Trajectory, Time) => Timestamped[TrajectoryPoint]) {
  override def toString                = getClass.getSimpleName
  def apply(traj: Trajectory, t: Time) = Timestamped(t, traj.getPoint(t))
}

case object KeypointSource extends Source[Timestamped[Keypoint], Trajectory] {
  def sources = List()
  def stream(traj: Trajectory) = {
    var ts = 0.0
    traj.keypoints.map { case (kp, tf) => { ts += tf; Timestamped(ts, kp) } }.toStream
  }
}

case class TrajectoryClock(dt: Timestep) extends Block[Time, Trajectory, Time] {
  val source                = Clock(dt)
  def stream(p: Trajectory) = source.takeWhile(_ < p.tf).stream(null)
}


@JsonCodec
case class TrajectoryPoint(p: Vec3,
                           v: Vec3,
                           a: Vec3,
                           j: Vec3,
                           nv: Vec3,
                           q: Quaternion[Real],
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
  def one                      = Keypoint(Some(Vec3.one), Some(Vec3.one), Some(Vec3.one))
}

case class SingleAxisInit(p: Real, v: Real, a: Real)
case class SingleAxisGoal(p: Option[Real], v: Option[Real], a: Option[Real])
