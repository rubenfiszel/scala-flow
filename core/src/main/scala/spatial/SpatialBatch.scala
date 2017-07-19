package dawn.flow.spatial

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

  implicit def typeR: Type[SpatialR]
  implicit def bitsR: Bits[SpatialR] 

  def spatial(): SpatialR

  def convertOutput(x: scala.Any): Timestamped[R]

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
      .asInstanceOf[List[scala.Any]]
      .map(convertOutput)

  }

}

abstract class SpatialBatch1[A, R, SpatialA <: MetaAny[_] : Type : Bits, SR <: MetaAny[_]](val rawSource1: Source[A])(implicit val typeR: Type[SR], val bitsR: Bits[SR]) extends SpatialBatch[R] with Block1[A, R] {

  type SpatialR = SR  
  type BatchInput = A
  lazy val zipIns = source1
  
  var in1: StreamIn[SpatialA] = _
  
  def convertInputs(x: ListT[BatchInput]): List[List[MetaAny[_]]]  = {
    List(x.map(convertA))
  }

  def convertA(x: Timestamped[A]): SpatialA      
  
  lazy val out = new Batch[BatchInput, R] {

    lazy val rawSource1 = zipIns

    def name = "Spatial Batch Inner 1"

    def runI(prog: () => Unit) {
      initConfig(stagingArgs)
      compileProgram(() => prog())
    }
    
    def f(lA: ListT[BatchInput]): ListT[R] = {

      setStreams(lA)

      def prog() = {
        val hiddenIn1 = StreamIn[SpatialA](In1)
        in1 = hiddenIn1
        val out = StreamOut[SpatialR](Out1)
        Accel {
          Stream(*) {x =>
            out := spatial()
          }
        }
      }

      runI(prog)
      getAndCleanStreams()      

    }
  }
  
  
}


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

//object SpatialBatch {
//  def apply[A, B, R](s1: Source[A], s2: Source[B]) = new Spatial {
//    new Source1
//  }
//}
