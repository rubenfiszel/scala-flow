package spatial.fusion.gen

import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.maths.{Coord3d, Scale}
import org.jzy3d.plot3d.primitives._
import org.jzy3d.plot3d.rendering.canvas.Quality

class AWTVisualisation(sim: Simulation,
                       dt: Timestep = 0.002,
                       seed: Seed = 12345) {

  def start() = {

    val datas = sim.simulate(dt, seed)
    val pts   = datas._1.toIndexedSeq
    val line  = new LineStrip()
    for (pt <- pts) {
      val p = pt.v.p
      //    println(t + " " + p)
      line.add(new Point(new Coord3d(p.x, p.y, p.z)))
    }

    val p     = pts(0).v.p
    val point = new Sphere(new Coord3d(p.x, p.y, p.z), 0.02f, 8, Color.BLUE)
    /*  val p1 = new Polygon()
     p1.add(new Point(new Coord3d(0, 0, 0)))
     p1.add(new Point(new Coord3d(0, 0.5, 0)))
     p1.add(new Point(new Coord3d(0, 0.5, 0.1)))
     p1.setColor(Color.GREEN)
     */
    point.setWireframeDisplayed(false)
    line.setWireframeColor(Color.RED)

    val chart = AWTChartComponentFactory.chart(Quality.Advanced)
    chart.getScene().getGraph().add(line)
    chart.getScene().getGraph().add(point)
    //  chart.getScene().getGraph().add(p1)
    chart.getView().setScale(new Scale(0, 1))
    chart.addMouseCameraController()
    chart.open("trajectory", 600, 600)

    val pt = new Thread(new Runnable {
      def run() = {
        while (true) {
          for (pt <- pts) {
            Thread.sleep(10)
            val p = pt.v.p
            //        println(p)
            point.setPosition(new Coord3d(p.x, p.y, p.z))
            point.setVolume(pt.v.t.toFloat / 1000)
            //    chart.getScene.clear()
          }
        }
      }
    })
    pt.start()
  }

}
