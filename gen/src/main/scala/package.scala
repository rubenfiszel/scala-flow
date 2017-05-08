package spatial.fusion

import breeze.stats.distributions._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

package object gen {

  type Real      = Double
  type State     = Seq[Real]
  type Seed      = Int
  type Timestep  = Real
  type Timeframe = Real
  type Time      = Real
  type Goal      = Keypoint
  type Rate      = Long
  def toReal(x: Timestep) = x.toDouble

  type NormalVector = Vec3
  type Jerk         = Vec3
  type Velocity     = Vec3
  type Position     = Vec3
  type Acceleration = Vec3
  type Thrust       = Real
  type Omega        = Real
  type BodyRates    = Vec3

  //variance in time prediction (simulate imperfection of reality)
  def genTimes(dt: Timestep,
               tf: Timeframe,
               variance: Double,
               seed: Seed): Stream[Time] = {
    val u = Gaussian(0, variance * dt)(RandBasis.withSeed(seed))
    genPerfectTimes(dt, tf).map(_ + u.draw())
  }

  def genPerfectTimes(dt: Timestep, tf: Timeframe): Stream[Time] = {
    def genTime(i: Long): Stream[Time] = (dt * i) #:: genTime(i + 1)
    genTime(0).takeWhile(_ < tf)
  }

  def fromRate(i: Long): Timestep = 1.0 / i

  //JSON
  implicit val encodeData: Encoder[Data] = new Encoder[Data] {
    final def apply(d: Data): Json = d.toValues.asJson
  }

  implicit val encodeTimestampedData: Encoder[Timestamped[Data]] =
    deriveEncoder
  implicit val encodeTimestampedTrajPoint
    : Encoder[Timestamped[TrajectoryPoint]] = deriveEncoder
  implicit val encodeTimestampedKeypoint
    : Encoder[Timestamped[Keypoint]] = deriveEncoder

}
