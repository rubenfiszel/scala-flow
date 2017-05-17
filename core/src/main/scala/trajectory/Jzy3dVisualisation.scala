package dawn.flow.trajectory

import dawn.flow._

import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.maths.{Coord3d, Scale}
import org.jzy3d.plot3d.primitives._
import org.jzy3d.plot3d.rendering.canvas.Quality

class Jzy3dVisualisation(val source1: SourceT[TrajectoryPoint, Trajectory], val source2: SourceT[Keypoint, Trajectory]) extends Sink[Trajectory] with Source2[Timestamped[TrajectoryPoint], Timestamped[Keypoint], Trajectory]{

  def consumeAll(traj: Trajectory) = {

    val pts   = source1.stream(traj)

    val chart = AWTChartComponentFactory.chart(Quality.Advanced)    
    val line  = new LineStrip()

    for (pt <- pts) {
      val p = pt.v.p
      //    println(t + " " + p)
      line.add(new Point(new Coord3d(p.x, p.y, p.z)))
    }


    val kps = source2.stream(traj)

    for (kp <- kps.map(_.v.p).filter(_.isDefined).map(_.get)) {
      val sph = new Sphere(new Coord3d(kp.x, kp.y, kp.z), 0.02f, 15, Color.random())
      sph.setWireframeDisplayed(false)
      chart.getScene().getGraph().add(sph)
    }

    val p     = pts(0).v.p
    val point = new Sphere(new Coord3d(p.x, p.y, p.z), 0.02f, 20, Color.BLUE)
    point.setWireframeDisplayed(false)
    line.setWireframeColor(Color.RED)

    chart.getScene().getGraph().add(line)
    chart.getScene().getGraph().add(point)
    //  chart.getScene().getGraph().add(p1)
//    chart.getView().setScale(new Scale(0, 1))
    chart.getView().setSquared(false)
    chart.addMouseCameraController()
    chart.open("trajectory", 600, 600)

    val pt = new Thread(new Runnable {
      def run() = {
        while (!Thread.currentThread().isInterrupted()) {
          for (pt <- pts) {
            Thread.sleep(10)
            val p = pt.v.p
//            print(t + " " + p)
            //        println(p)
            point.setPosition(new Coord3d(p.x, p.y, p.z))
            point.setVolume(pt.v.t.toFloat / 1000)

            //    chart.getScene.clear()
          }
        }
      }
    })
    pt.start()
//    chart.getScene().getGraph().addWindowListener(???)
  }

}
