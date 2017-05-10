package spatial.fusion.gen

import breeze.plot._
import breeze.linalg._

object PlotData {

  def createFigure[A: Data](s: Source[Timestamped[A]]) = {

    val data = implicitly[Data[A]]
    val st = s.stream()
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
