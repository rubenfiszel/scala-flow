package dawn.flow

import breeze.plot._
import breeze.linalg._


object Plot {
  var i = 0
}

case class Plot[A: Data](rawSource1: Source[A], rawSourcesIn: Source[A]*)
    extends SinkBatchN[A] with Source1[A] {

  override val rawSources = rawSource1 :: rawSourcesIn.toList

  def listen1(x: Timestamped[A]) = ()

  def consumeAll(ar: Array[ListT[A]]) = {

    val data = implicitly[Data[A]]

    val xs = ar.map(_.map(_.time))
    val ys = ar.map(_.map(x => data.toVector(x.v)))

//    println(ar.map(_.length).toList)
//    println(ys.toList)

    //Array[List[List[Data]]]


    val yss: IndexedSeq[Array[(List[Double], List[Real])]] =
      ((0 until ys(0)(0).length)).map( i =>
        (xs.zip(ys).map{ case (xsi, ysi) =>
          (xsi, ysi.map( ysii =>
            ysii(i)
          ))
        }
        )
      )

    val f = new Figure("fig ", yss.length, 1)
    f.width = 1920
    f.height = (2160/7.0*yss.length).toInt
    f.visible = false
    val titles = yss.length match {
      case 1 =>
        Seq("distance")
      case 2 =>
        Seq("p", "q")
      case 3 =>
        Seq("x", "y", "z")
      case 4 =>
        Seq("r", "i", "j", "k")
      case 7 =>
        Seq("x", "y", "z", "r", "i", "j", "k")
    }
  
    yss.zip(titles).zipWithIndex.foreach {
      case (((vs), t), i: Int) => {
        val p = f.subplot(i)
        vs.foreach { case (x, y) => 
          p += plot(x, y)
        }

        p.xlabel = "time"
        p.ylabel = t
        p.legend = true
      }
    }

    Plot.i += 1
    println(s"Save plot as plot-${Plot.i}.pdf")
    f.saveas(s"plot-${Plot.i}.pdf", 50)
  }
}
