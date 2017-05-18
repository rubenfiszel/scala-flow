package dawn

import breeze.stats.distributions._
import breeze.linalg._

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
  type Op1T[A, B, C] = Op1[Timestamped[A], B, Timestamped[C]]
  type Op2T[A, B, C, D] = Op2[Timestamped[A], B, Timestamped[C], Timestamped[D]]  
  type Stream[A] = scala.Stream[A]

  implicit def fromNothing[A, B](s: Source[A, Null]) = new Source[A, B] {
    override def toString = s.toString
    def sources = s.sources
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


  implicit def toVec3(x: DenseVector[Real]): Vec3 = Vec3(x)

  def debug[A](x: A) = {
    println(x)
    x
  }

  implicit def toTimestampedSource[A,B](s: Source[Timestamped[A],B]): TimestampedSource[A, B] =
    new TimestampedSource(s)

  implicit def toTimeSource[B](s: Source[Time, B]): TimeSource[B] =
    new TimeSource(s)
  
  implicit def toStreamSource[A,B](s: Source[Stream[A],B]): StreamSource[A, B] =
    new StreamSource(s)

  implicit def toStdLibSource[A,B](s: Source[A,B]): StdLibSource[A, B] =
    new StdLibSource(s)
  
}
