package dawn

//import scala.{specialized => sp}
import breeze.stats.distributions._
import breeze.linalg.{DenseVector, DenseMatrix}
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

  type VectorR = DenseVector[Real]
  type MatrixR = DenseMatrix[Real]
  type Quat = Quaternion[Real]
  type TS[A] = Timestamped[A]
  def fromRate(i: Long): Timestep = 1.0 / i
  

  //Less Timestamped boilerplate
  type SourceT[A] = Source[Timestamped[A]]
  type ListT[A] = List[Timestamped[A]]

  type Source1T[A] = Source1[Timestamped[A]]
  type Source2T[A, B] = Source2[Timestamped[A], Timestamped[B]]
  type Source3T[A, B, C] = Source3[Timestamped[A], Timestamped[B], Timestamped[C]]      
  type StreamT[A]    = Stream[Timestamped[A]]

  type Op1T[A, B] = Op1[Timestamped[A], Timestamped[B]]
  type Op2T[A, B, C] =
    Op2[Timestamped[A], Timestamped[B], Timestamped[C]]
  type Op3T[A, B, C, D] =
    Op3[Timestamped[A], Timestamped[B], Timestamped[C], Timestamped[D]]    
  type Op4T[A, B, C, D, E] =
    Op4[Timestamped[A], Timestamped[B], Timestamped[C], Timestamped[D], Timestamped[E]]    

  type Block1T[A, B] = Block1[Timestamped[A], Timestamped[B]]
  type Block2T[A, B, C] =
    Block2[Timestamped[A], Timestamped[B], Timestamped[C]]
  type Block3T[A, B, C, D] =
    Block3[Timestamped[A], Timestamped[B], Timestamped[C], Timestamped[D]]    
  type Block4T[A, B, C, D, E] =
    Block4[Timestamped[A], Timestamped[B], Timestamped[C], Timestamped[D], Timestamped[E]]    

  //******* Data (as in transformable in array of Real **********

  implicit object RealData extends Data[Real] {
    def toValues(x: Real) = Seq(x)
  }

  implicit object denseVectorRealData extends Data[DenseVector[Real]] {
    def toValues(x: DenseVector[Real]) = x.toArray.toSeq
  }


  //****** Source to specific Ops Source

  implicit def toTimestampedSource[A](
      s: Source[Timestamped[A]]): TimestampedSource[A] =
    new TimestampedSource(s)

  implicit def toTimeSource[B](s: Source[Time]): TimeSource =
    new TimeSource(s)

  implicit def toStreamSource[A](
      s: Source[Stream[A]]): StreamSource[A] =
    new StreamSource(s)


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

  implicit val QuatdoubleVec = new Vec[Quat] {
    def scale(x: Quat, y: Double) = x * y
    def plus(x: Quat, y: Quat) = x + y
    def minus(x: Quat, y: Quat) = x - y    
  }

  implicit class VecOps[A: Vec](x: A) {
    val v = implicitly[Vec[A]]
    def *(y: Double) = v.scale(x, y)
    def +(y: A) = v.plus(x, y)
    def -(y: A) = v.minus(x, y)    
  }

  implicit def tsVecToVec[A: Vec] = new Vec[Timestamped[A]] {
    val aVec = implicitly[Vec[A]]
    def scale(x: Timestamped[A], y: Double) = x.map(z => aVec.scale(z, y))
    def plus(x: Timestamped[A], y: Timestamped[A]) = x.map(z => aVec.plus(z, y.v))
    def minus(x: Timestamped[A], y: Timestamped[A]) = x.map(z => aVec.minus(z, y.v))
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
