package spatial.fusion.gen


import org.jzy3d.chart._
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.maths.{ Coord3d, Scale }
import org.jzy3d.plot3d.primitives.{ LineStrip, Point, Polygon, Sphere }
import org.jzy3d.plot3d.rendering.canvas.Quality

object GenData extends App {

  val traj = RapidTrajectory(Init(Vec3.zero, Vec3(4, -2, 3), Vec3.zero))

  val dt = 0.01
  val ts = (1 / dt).toInt

  val line = new LineStrip()
  for (i <- 0 to ts) {
    val t = i * dt
    val p = traj.getPosition(t)
//    println(t + " " + p)
    line.add(new Point(new Coord3d(p.x, p.y, p.z)))
  }

  val p     = traj.getPosition(0)
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

  var t  = 0
  val tf = 1000
  val pt = new Thread(new Runnable {
    def run() = {
      while (true) {
        t = t % tf
        Thread.sleep(10)
        val time = t / tf.toDouble
        val p = traj.getPosition(time)
//        println(p)
        point.setPosition(new Coord3d(p.x, p.y, p.z))
        println(traj.getThrust(time).toFloat)
        point.setVolume(traj.getThrust(time).toFloat/1000)
        //    chart.getScene.clear()
        t += 1
      }
    }
  })
  pt.start()

}
