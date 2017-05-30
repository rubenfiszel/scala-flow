package dawn.flow

import breeze.plot._
import breeze.linalg._

case class Plot[A: Data](rawSource1: Source[A])(implicit val nodeHook: NodeHook)
    extends SinkBatch1[A] {

  def consumeAll(st: ListT[A]) = {
    val data = implicitly[Data[A]]
    val x    = st.map(_.time)
    val y    = st.map(x => data.toValues(x.v))
    val ys   = (0 until y(0).length).map(x => y.map(z => z(x)))

    val f = new Figure("fig", ys.length, 1)
    ys.zipWithIndex.foreach {
      case (v, i) => {
        val p = f.subplot(i)
        p += plot(x, v)
        p.xlabel = "time"
        p.ylabel = "value"
        p.ylim = (-1, 1)        
      }
    }
  }
}

case class Plot2[A: Data](rawSource1: Source[A],
                             rawSource2: Source[A])(implicit val nodeHook: NodeHook)
    extends SinkBatch2[A, A] {

  def consumeAll(st: ListT[A], st2: ListT[A]) = {    
    val data = implicitly[Data[A]]

    val x  = st.map(_.time)
    val x2 = st2.map(_.time)
    val y  = st.map(x => data.toValues(x.v))
    val y2 = st2.map(x => data.toValues(x.v))

    val ys  = (0 until y(0).length).map(x => y.map(z => z(x)))
    val ys2 = (0 until y2(0).length).map(x => y2.map(z => z(x)))

    val f = new Figure("fig", ys.length, 1)
    val titles =
      if (ys.length == 3)
        Seq("x", "y", "z")
      else
        Seq("r", "i", "j", "k")
    ys.zip(ys2).zip(titles).zipWithIndex.foreach {
      case (((v1, v2), t), i: Int) => {
        val p = f.subplot(i)
        p += plot(x, v1)
        p += plot(x2, v2)
        p.xlabel = "time"
        p.ylabel = t
        p.legend = true
//        p.ylim = (-1, 1)
      }
    }
  }
}
