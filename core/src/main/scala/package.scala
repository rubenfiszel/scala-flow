package dawn

//import scala.{specialized => sp}
import breeze.stats.distributions._
import breeze.linalg.DenseVector
import scala.language.implicitConversions
import spire.math.Quaternion
import spire.implicits._
import io.circe.generic.JsonCodec
import scala.collection.GenSeq
import io.circe._

package object flow {

  //Common seed 
  val Random = RandBasis.withSeed(12345)


  //Common type aliases
  type Real      = Double
  type Timestep  = Real
  type Timeframe = Real
  type Time      = Real

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
  

  //Less Timestamped boilerplate
  type SourceT[A, B] = Source[Timestamped[A], B]
  type StreamT[A]    = Stream[Timestamped[A]]

  type Op1T[A, B, C] = Op1[Timestamped[A], B, Timestamped[C]]
  type Op2T[A, B, C, D] =
    Op2[Timestamped[A], B, Timestamped[C], Timestamped[D]]
  type Op3T[A, B, C, D, E] =
    Op3[Timestamped[A], B, Timestamped[C], Timestamped[D], Timestamped[E]]    



  //A Stream doesn't HAVE TO be a scala stream, it just happen to be
  type Stream[A] = scala.Stream[A]

  //Useful to cast from non-model dependant time source to model dependant
  implicit def fromNothing[A, B](s: Source[A, Null]) = new Source[A, B] {
    override def toString = s.toString
    def sources           = s.sources
    def genStream(p: B)      = s.stream(null)
  }


  //******* Data (as in transformable in array of Real **********

  implicit object RealData extends Data[Real] {
    def toValues(x: Real) = Seq(x)
  }

  implicit object denseVectorRealData extends Data[DenseVector[Real]] {
    def toValues(x: DenseVector[Real]) = x.toArray.toSeq
  }


  //****** Source to specific Ops Source

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


  //***** Vec (as in some Ops common in "vectors". Scalars are 1D vectors)

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

  //********* Utils *************

  def getStrOrElse(str: String, default: String) =
    if (str.isEmpty)
      default
    else
      str

  def debug[A](x: A) = {
    println(x)
    x
  }

  //********* Vec3 (just a thin wrapper around DenseVector) *************

  type Vec3 = DenseVector[Real]

  implicit class Vec3Ops(dv: DenseVector[Real]) {
    def x = dv(0)
    def y = dv(1)
    def z = dv(2)
  }


  object Vec3 {
    def zero = Vec3(0, 0, 0)
    def one  = Vec3(1, 1, 1)

    def apply(x: Real = 0, y: Real = 0, z: Real = 0): Vec3 = 
      DenseVector(x, y, z)

//    def apply(gs: Vector[Real]): Vec3 =
//      apply(gs.toArray)

    def apply(gs: GenSeq[Real]): Vec3 = {
      require(gs.length == 3)
      DenseVector(gs(0), gs(1), gs(2))
    }
  }

  implicit val encodeDV: Encoder[DenseVector[Real]] = new Encoder[DenseVector[Real]] {
    def apply(x: DenseVector[Real]) =
      Json.fromValues(x.map(y => Json.fromDouble(y).get).toArray)
  }

  implicit val decodeDV: Decoder[DenseVector[Real]] = new Decoder[DenseVector[Real]] {
    def apply(x: HCursor) =
      for {
        v <- x.downArray.as[Array[Double]]
      }
      yield
        Vec3(v)
  }
  
}
