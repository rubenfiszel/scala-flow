package dawn

//import scala.{specialized => sp}
import breeze.stats.distributions._
import breeze.linalg.DenseVector
import scala.language.implicitConversions
import spire.math.Quaternion
import spire.implicits._

package object flow {

  val Random = RandBasis.withSeed(12345)

  type Real      = Double
  type Timestep  = Real
  type Timeframe = Real
  type Time      = Real


  /*  //JSON
  implicit val encodeData: Encoder[Data] = new Encoder[Data] {
    final def apply(d: Data): Json = d.toValues.asJson
  }
   */

  type SourceT[A, B] = Source[Timestamped[A], B]
  type StreamT[A]    = Stream[Timestamped[A]]

  type Op1T[A, B, C] = Op1[Timestamped[A], B, Timestamped[C]]
  type Op2T[A, B, C, D] =
    Op2[Timestamped[A], B, Timestamped[C], Timestamped[D]]

  type Block1T[A, B, C] = Block1[Timestamped[A], B, Timestamped[C]]
  type Block2T[A, B, C, D] =
    Block2[Timestamped[A], B, Timestamped[C], Timestamped[D]]

  type Stream[A] = scala.Stream[A]

  implicit def fromNothing[A, B](s: Source[A, Null]) = new Source[A, B] {
    override def toString = s.toString
    def sources           = s.sources
    def genStream(p: B)      = s.stream(null)
  }

  type Rate = Long
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

  implicit object RealData extends Data[Real] {
    def toValues(x: Real) = Seq(x)
  }
  
  implicit object Vec3Data extends Data[Vec3] {
    def toValues(x: Vec3) = x.toArray.toSeq
  }

  implicit def toVec3(x: DenseVector[Real]): Vec3 = Vec3(x)

  def debug[A](x: A) = {
    println(x)
    x
  }

  implicit def toTimestampedSource[A, B](
      s: Source[Timestamped[A], B]): TimestampedSource[A, B] =
    new TimestampedSource(s)

  implicit def toTimeSource[B](s: Source[Time, B]): TimeSource[B] =
    new TimeSource(s)

  implicit def toStreamSource[A, B](
      s: Source[Stream[A], B]): StreamSource[A, B] =
    new StreamSource(s)

  implicit def toStdLibSource[A, B](s: Source[A, B]): StdLibSource[A, B] =
    new StdLibSource(s)

  //spire/breeze integration
  //Let's hope they will be a better integration in the future
  //For now it will be enough
  //TODO: Make it generic instead of specific to Double
  

  implicit val doubleVec = new Vec[Double] {
    def scale(x: Double, y: Double) = x * y
    def plus(x: Double, y: Double) = x + y
    def minus(x: Double, y: Double) = x - y    
  }

  implicit val DVdoubleVec = new Vec[DenseVector[Double]] {
    def scale(x: DenseVector[Double], y: Double) = x * y
    def plus(x: DenseVector[Double], y: DenseVector[Double]) = x + y
    def minus(x: DenseVector[Double], y: DenseVector[Double]) = x - y    
  }

  implicit val QuatdoubleVec = new Vec[Quaternion[Double]] {
    def scale(x: Quaternion[Double], y: Double) = x * y
    def plus(x: Quaternion[Double], y: Quaternion[Double]) = x + y
    def minus(x: Quaternion[Double], y: Quaternion[Double]) = x - y    
  }

  implicit class VecOps[A: Vec](x: A) {
    val v = implicitly[Vec[A]]
    def *(y: Double) = v.scale(x, y)
    def +(y: A) = v.plus(x, y)
    def -(y: A) = v.minus(x, y)    
  }

  implicit def tsVecToVec[A: Vec] = new Vec[Timestamped[A]] {
    def scale(x: Timestamped[A], y: Double) = x.copy(v = x.v * y)
    def plus(x: Timestamped[A], y: Timestamped[A]) = x.copy(v = x.v + y.v)
    def minus(x: Timestamped[A], y: Timestamped[A]) = x.copy(v = x.v - y.v)
  }
  
  def getStrOrElse(str: String, default: String) =
    if (str.isEmpty)
      default
    else
      str


}
