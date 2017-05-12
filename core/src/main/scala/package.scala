package dawn

import breeze.stats.distributions._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
//import cats.free.Free

package object flow {

  val Random = RandBasis.withSeed(12345)

  type Real      = Double
  type Timestep  = Real
  type Timeframe = Real
  type Time      = Real



  //variance in time prediction (simulate imperfection of reality)
  def genTimes(dt: Timestep,
               variance: Double): Stream[Time] = {
    val u = Gaussian(0, variance * dt)(Random)
    genPerfectTimes(dt).map(_ + u.draw())
  }

  def genPerfectTimes(dt: Timestep): Stream[Time] = {
    def genTime(i: Long): Stream[Time] = (dt * i) #:: genTime(i + 1)
    genTime(0)
  }

/*  //JSON
  implicit val encodeData: Encoder[Data] = new Encoder[Data] {
    final def apply(d: Data): Json = d.toValues.asJson
  }
 */
    
  type SourceT[A, B] = Source[Timestamped[A], B]
  type StreamT[A] = Stream[Timestamped[A]]
  type Stream[A] = scala.Stream[A]
  type MapT[A,B, C] = Map[Timestamped[A], Timestamped[B], C]

  implicit def fromNothing[A, B](s: Source[A, Null]): Source[A, B] = new Source[A,B] {
    def stream(p: B) = s.stream(null)
  }

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


  def fromRate(i: Long): Timestep = 1.0 / i

  
  implicit object Vec3Data extends Data[Vec3]{
    def toValues(x: Vec3) = x.toArray.toSeq
  }
  

}