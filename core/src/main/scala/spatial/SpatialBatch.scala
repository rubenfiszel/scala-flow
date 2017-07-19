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
  type SpatialR <: MetaAny[_]

  implicit def typeSR: Type[SpatialR]
  implicit def bitsSR: Bits[SpatialR]

  def convertOutput(x: Seq[(java.lang.String, Exp[_])]): Timestamped[R]

  @struct case class TSR(t: Double, v: SpatialR)

  override def stagingArgs = scala.Array[java.lang.String]("--interpreter", "-q")

  type BatchInput

  val zipIns: Source[BatchInput]

  def convertInputs(x: ListT[BatchInput]): List[List[MetaAny[_]]]

  def setStreams(x: ListT[BatchInput]) = {
    val inputs                                = List[Bus](In1, In2, In3, In4, In5).zip(convertInputs(x))
    val inputsMap: Map[Bus, List[MetaAny[_]]] = inputs.toMap
    val outs: List[Bus]                       = List(Out1)

    inputsMap.foreach {
      case (bus, content) =>
        Streams.addStreamIn(bus)
        content.foreach(x => Streams.streamsIn(bus).put(x.s.asInstanceOf[Const[_]].c))
    }

    Streams.addStreamOut(Out1)
  }

  def getAndCleanStreams(): List[Timestamped[R]] = {

    val r = Streams.streamsOut(Out1)

    Streams.streamsOut = Map()
    Streams.streamsIn = Map()

    r.asScala.toList
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

abstract class SpatialBatch1[A, R, SA <: MetaAny[_], SR <: MetaAny[_]]
  (val rawSource1: Source[A])
  (implicit
    val sa: Spatialable[A] { type Spatial = SA },
    val sr: Spatialable[R] { type Spatial = SR}
  )
    extends SpatialBatch[R]
    with Block1[A, R] {

  type SpatialA = sa.Spatial
  type SpatialR = sr.Spatial

  implicit def bitsSA = sa.bitsI
  implicit def typeSA = sa.typeI

  implicit def bitsSR: Bits[SpatialR] = sr.bitsI
  implicit def typeSR                 = sr.typeI

  @struct case class TSA(t: Double, v: SA)

  type BatchInput = A
  lazy val zipIns = source1

  def spatial(tsa: TSA): SR

  def convertInputs(x: ListT[BatchInput]): List[List[MetaAny[_]]] = {
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
    implicit def typeSR                 = sr.typeI

    lazy val rawSource1 = zipIns

    def name = "Spatial Batch Inner 1"

    def runI(prog: () => Unit) {
      initConfig(stagingArgs)
      compileProgram(() => prog())
    }

    def f(lA: ListT[BatchInput]): ListT[R] = {

      setStreams(lA)

      def prog() = {
        Accel {
          val in1 = StreamIn[TSA](In1)
          val out = StreamOut[TSR](Out1)
          
          Stream(*) { x =>
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

abstract class SpatialBatch2[A, B, R, SA <: MetaAny[_], SB <: MetaAny[_], SR <: MetaAny[_]](val rawSource1: Source[A], val rawSource2: Source[B])(implicit val sa: Spatialable[A] {type Spatial = SA}, val sb: Spatialable[B] {type Spatial = SB}, val sr: Spatialable[R] { type Spatial = SR}) extends SpatialBatch[R] with Block2[A, B, R] {

  type SpatialA = sa.Spatial
  type SpatialB = sb.Spatial
  type SpatialR = sr.Spatial

  implicit def bitsSA = sa.bitsI
  implicit def typeSA = sa.typeI

  implicit def bitsSB = sb.bitsI
  implicit def typeSB = sb.typeI

  implicit def bitsSR: Bits[SpatialR] = sr.bitsI
  implicit def typeSR = sr.typeI

  @struct case class TSA(t: Double, v: SA)
  @struct case class TSB(t: Double, v: SB)

  type BatchInput = Either[A, B]
  lazy val zipIns = source1.merge(source2)

  def spatial(ts: Either[TSA, TSB]): SR

  def convertInputs(x: ListT[BatchInput]): List[List[MetaAny[_]]]  = {
    val la = x.filter(_.v.isLeft).map(y => TSA(y.t, sa.to(y.v.left.get)))
    val lb = x.filter(_.v.isRight).map(y => TSB(y.t, sb.to(y.v.right.get)))    
    List(la, lb)
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

      @virtualize def prog() = {
        Accel {
          val in1 = StreamIn[TSA](In1)
          val in2 = StreamIn[TSB](In2)
          val fifo1 = FIFO[TSA](1000)
          val fifo2 = FIFO[TSB](1000)
          val out = StreamOut[TSR](Out1)
          
          Stream(*) {x =>
            fifo1.enq(in1)
          }
          Stream(*) {x =>
            fifo2.enq(in2)
          }
          FSM[Boolean, Boolean](true){x => x}{x =>
            if ((fifo2.empty && !fifo1.empty) || (!fifo1.empty && !fifo2.empty && fifo1.peek.t < fifo2.peek.t)) {
              val tsa = fifo1.deq()
              out := TSR(tsa.t, spatial(Left(tsa)))
            }
            else if (!fifo2.empty) {
              val tsb = fifo2.deq()
              out := TSR(tsb.t, spatial(Right(tsb)))
            } 
          }{x => !fifo1.empty || !fifo2.empty}
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
