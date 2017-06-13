package dawn.flow.trajectory

import dawn.flow._
import io.circe.generic.JsonCodec
import breeze.linalg.{max => _, min => _, _ => _}
import spire.math.{Real => _, _ => _}
import spire.implicits._

trait Trajectory extends Model {

  def tf: Timeframe
  def keypoints: List[(Keypoint, Timeframe)]
  def gravity: Vec3

  def getKeypoints = {
    var t = 0.0
    keypoints.map(x => { t += x._2; Timestamped(t, x._1) })
  }

  def getPosition(t: Time): Position

  def getVelocity(t: Time): Velocity

  def getAcceleration(t: Time): Acceleration

  //Acceleration + G
//  def getFullAcceleration(t: Time): Acceleration =
//    getAcceleration(t) + gravity

  //TODO: getAcceleration in Local coordinate you retard
  //Acceleration in the local referential of the drone
  def getLocalAcceleration(t: Time) = {
    getOrientationQuaternion(t).reciprocal
      .rotate(getAcceleration(t))

  }

  def getJerk(t: Time): Jerk

  //NormalVector is oriented toward acceleration - G
  //because it is assumed that acceleration direction is the goal
  //and you need to retrieve it after applying G
  def getNormalVector(t: Time): NormalVector

  def getThrust(t: Time): Thrust

  //The quaternion is not unique but we choose the "shortest arc".
  def getOrientationQuaternion(t: Time) = {
    val rt =
      if (t > tf) {
        println("[Warning] time over tf")
        tf
      } else
        t

    Quat.getQuaternion(
      Vec3(0, 0, 1),
      getNormalVector(rt)
    )
  }

  //avoid end of trajectory sampling annoyance
  //by "replaying" the last possible sample
  def lastPossibleDelta(t: Time, dt: Timestep) =
    if ((t + dt) >= tf)
      tf - 1.1 * dt
    else
      t

  def getQuaternionRotation(t: Time, dt: Timestep) = {
    val rt = lastPossibleDelta(t, dt)
    getOrientationQuaternion(rt + dt)
      .rotateBy(getOrientationQuaternion(rt).reciprocal)
  }

  def getOmega(t: Time, dt: Timestep): Vec3 = {
    val rt = lastPossibleDelta(t, dt)
    Quat.quatToAxisAngle(getOrientationQuaternion(rt),
                                getOrientationQuaternion(rt + dt)) / dt
  }

  def getPoint(t: Time, dt: Timestep = 1e-3): TrajectoryPoint =
    TrajectoryPoint(getPosition(t),
                    getVelocity(t),
                    getLocalAcceleration(t),
                    getJerk(t),
                    getNormalVector(t),
                    getOrientationQuaternion(t),
                    getThrust(t),
                    getOmega(t, dt))

}

class TrajectoryClock(dt: Timestep)(
    implicit val modelHook: ModelHook[Trajectory],
    val schedulerHook: SchedulerHook,
    val nodeHook: NodeHook
) extends Block0[Time]
    with RequireModel[Trajectory] {
  def name = "TrajectoryClock " + dt
  lazy val out = (new Clock(dt)).takeWhile(_ < model.get.tf, "< tf")
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
  def one = Keypoint(Some(Vec3.one), Some(Vec3.one), Some(Vec3.one))
}

case class SingleAxisInit(p: Real, v: Real, a: Real)
case class SingleAxisGoal(p: Option[Real], v: Option[Real], a: Option[Real])
