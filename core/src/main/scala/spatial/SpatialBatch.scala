package dawn.flow.spatialf

import spatial._
import spatial.interpreter._
import spatial.dsl._
import org.virtualized._
import dawn.flow._
import scala.collection.JavaConverters._
import argon.core.Const

trait SpatialBatch[R] extends SpatialStream {

  def name = "Spatial Batch"
  type SpatialR <:  MetaAny[_]

  implicit def typeSR: Type[SpatialR]
  implicit def bitsSR: Bits[SpatialR] 

  def convertOutput(x: Seq[(java.lang.String, Exp[_])]): Timestamped[R]

  @struct case class TSR(t: Double, v: SpatialR)    

  override def stagingArgs = scala.Array[java.lang.String]("--interpreter", "-q")

  type BatchInput 

  val zipIns: Source[BatchInput]
  

  def convertInputs(x: ListT[BatchInput]): List[List[MetaAny[_]]]

  def setStreams(x: ListT[BatchInput]) = {
    val inputs = List[Bus](In1, In2, In3, In4, In5).zip(convertInputs(x))
    val inputsMap: Map[Bus, List[MetaAny[_]]] = inputs.toMap
    val outs: List[Bus] = List(Out1)

    inputsMap.foreach { case (bus, content) =>
      Streams.addStreamIn(bus)
      content.foreach(x => Streams.streamsIn(bus).put(x.s.asInstanceOf[Const[_]].c))
    }
    
    Streams.addStreamOut(Out1)
  }

  def getAndCleanStreams(): List[Timestamped[R]] = {

    val r = Streams.streamsOut(Out1)

    Streams.streamsOut = Map()
    Streams.streamsIn = Map()

    r
      .asScala
      .toList
      .asInstanceOf[List[Seq[(java.lang.String, Exp[_])]]]
      .map(convertOutput)

  }

}

trait Spatialable[A] {
  type Spatial <: MetaAny[_]
  type Internal
  implicit def bitsI: Bits[Spatial]
  implicit def typeI: Type[Spatial]
  def from(x: Internal): A
  def to(x: A): Spatial
}

abstract class SpatialBatch1[A, R, SA, SR <: MetaAny[_]](val rawSource1: Source[A])(implicit val sa: Spatialable[A] {type Spatial = SA}, val sr: Spatialable[R] { type Spatial = SR}) extends SpatialBatch[R] with Block1[A, R] {

  type SpatialA = sa.Spatial
  type SpatialR = sr.Spatial

  implicit def bitsSA = sa.bitsI
  implicit def typeSA = sa.typeI

  implicit def bitsSR: Bits[SpatialR] = sr.bitsI
  implicit def typeSR = sr.typeI

  @struct case class TSA(t: Double, v: SA)      

  type BatchInput = A
  lazy val zipIns = source1

  def spatial(tsa: TSA): SR
  
  def convertInputs(x: ListT[BatchInput]): List[List[MetaAny[_]]]  = {
    List(x.map(y => TSA(y.t, sa.to(y.v))))
  }

  def convertOutput(x: Seq[(java.lang.String, Exp[_])]) = {
    var m: Map[java.lang.String, Exp[_]] = Map()
    x.foreach(y => m += y)
    Timestamped(
      m("t").asInstanceOf[Const[_]].c.asInstanceOf[BigDecimal].toDouble,
      sr.from(m("v").asInstanceOf[Const[_]].c.asInstanceOf[sr.Internal])
    )
  }
  
  lazy val out = new Batch[BatchInput, R] {

    implicit def bitsSR: Bits[SpatialR] = sr.bitsI
    implicit def typeSR = sr.typeI

    lazy val rawSource1 = zipIns

    def name = "Spatial Batch Inner 1"

    def runI(prog: () => Unit) {
      initConfig(stagingArgs)
      compileProgram(() => prog())
    }
    
    def f(lA: ListT[BatchInput]): ListT[R] = {

      setStreams(lA)

      def prog() = {
        val in1 = StreamIn[TSA](In1)
        val out = StreamOut[TSR](Out1)
        Accel {
          Stream(*) {x =>
            val tsa = in1.value
            out := TSR(tsa.t, spatial(tsa))
          }
        }
      }

      runI(prog)
      getAndCleanStreams()      

    }
  }
  
  
}

/*
trait SpatialBatch2[A, B, R] extends SpatialBatch[R] with Block2[A, B, R] {

  type BatchInput = Either[A, B]
  val zipIns = source1.merge(source2)

  type SpatialA <: MetaAny[_]
  type SpatialB <: MetaAny[_]  

  var in1: StreamIn[SpatialA] = _
  var in2: StreamIn[SpatialA] = _  

  implicit def typeA: Type[SpatialA]
  implicit def bitsA: Bits[SpatialA]  

  implicit def typeB: Type[SpatialB]
  implicit def bitsB: Bits[SpatialB]

  def convertInputs(x: ListT[BatchInput]): List[List[MetaAny[_]]]  = {
    ???//List(x.map(convertA))
  }
  
  def convertA(x: A): SpatialA
  def convertB(x: A): SpatialB
  
  lazy val out = new Batch[BatchInput, R] {

    def rawSource1 = zipIns

    def name = "Spatial Batch Inner"

    def runI(prog: () => Unit) {
      initConfig(stagingArgs)
      compileProgram(() => prog())
    }
    
    def f(lA: ListT[BatchInput]): ListT[R] = {

      setStreams(lA)

      def prog() = {
        val hiddenIn1 = StreamIn[SpatialA](In1)
        val hiddenIn2 = StreamIn[SpatialB](In2)        
        in1 = hiddenIn1
        in2 = hiddenIn1        
        val out = StreamOut[SpatialR](Out1)
        Accel {
          out := spatial()
        }
      }

      runI(prog)
      getAndCleanStreams()
      

    }
  }
  
  
}
 */
//object SpatialBatch {
//  def apply[A, B, R](s1: Source[A], s2: Source[B]) = new Spatial {
//    new Source1
//  }
//}
