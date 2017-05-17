package dawn.flow

import breeze.plot._
import breeze.linalg._


case class Plot[A: Data, B](source: Source[Timestamped[A], B]) extends Sink[B] with Source1[Timestamped[A], B]{

  def consumeAll(p: B) = {
    val data = implicitly[Data[A]]
    val st = source.stream(p)
    val x = st.map(_.t)
    val y = st.map(x => data.toValues(x.v))
    val ys = (0 until y(0).length).map(x => y.map(z => z(x)))

    val f = new Figure("fig", ys.length, 1)
    ys.zipWithIndex.foreach { case (v, i) => {
      val p = f.subplot(i)
      p += plot(x, v)
      p.xlabel = "time"
      p.ylabel = "value"      
    }}
  }
}

case class Plot2[A: Data, B](source1: Source[Timestamped[A], B], source2: Source[Timestamped[A], B]) extends Sink[B] with Source2[Timestamped[A], Timestamped[A], B]{

  def consumeAll(p: B) = {
    val data = implicitly[Data[A]]    

    val st = source1.stream(p)
    val st2 = source2.stream(p)

    val x = st.map(_.time)
    val x2 = st2.map(_.time)    
    val y = st.map(x => data.toValues(x.v))
    val y2 = st2.map(x => data.toValues(x.v))

    val ys = (0 until y(0).length).map(x => y.map(z => z(x)))
    val ys2 = (0 until y2(0).length).map(x => y2.map(z => z(x)))    

    val f = new Figure("fig", ys.length, 1)
    val titles =
      if (ys.length == 3)
        Seq("x", "y", "z")
      else
        Seq("r", "i", "j", "k")        
    ys.zip(ys2).zip(titles).zipWithIndex.foreach { case (((v1, v2), t), i: Int) => {
      val p = f.subplot(i)
      p += plot(x, v1)
      p += plot(x2, v2)      
      p.xlabel = "time"
      p.ylabel = t
      p.legend = true
    }}
  }
}



