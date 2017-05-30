package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._
/*
object DroneComplementary extends App {

  //****** Model ******
  val dt = Config.dt

  val traj = TrajFactory.generate()

  implicit val mc = ModelCB[Trajectory]
  //We cache it to showcase Cache that avoids recomputing points each time
  val clock = TrajectoryClock(dt)
  val points = clock.map(LambdaWithModel((t: Time, traj: Trajectory) =>
                           Timestamped(t, traj.getPoint(t))),
                         "toPoints")

  //******* Filter ********
  //Rate at which sensor geenerate data

  //The NormalVector through the ComplimentaryFilter

  //filter parameter
  val cov   = Config.cov
  val covQ  = Config.covQ
  val initQ = Config.initQ

  val alpha = 0.95

  val accelerometer = clock.map(Accelerometer(cov))
  val gyroscope     = clock.map(Gyroscope(cov, dt))
  val controlInput  = clock.map(ControlInput(1, cov, dt))

  lazy val filter: SourceT[Quat] =
    OrientationComplementaryFilter(accelerometer,
                                   gyroscope,
                                   controlInput,
                                   initQ,
                                   alpha,
                                   dt)

  val lpf = LowPassFilter(filter, Timestamped(initQ), 0.2)

  val qs =
    points.mapT((x: TrajectoryPoint) => x.q, "toQ") //.cache()

  var sinks: Seq[Sink] = Seq()

  def awt() = {
    val vis = new Jzy3dVisualisation(points, KeypointSource())
    sinks ++= Seq(vis)
  }

  def printJson() = {
//    val jsonFilter  = JsonExport(lpf)
//    val printFilter = PrintSink(jsonFilter)
//    sinks ++= Seq(printFilter)
  }

  def figure() = {
    val plot = Plot2(lpf, qs)
    sinks ++= Seq(plot)
  }

  def testTS() = {
    val test = TestTS(lpf, qs, 1000)
    sinks ++= Seq(test)
  }

  testTS()
  figure()

  //    awt()
  //  printJson()

  Sourcable.drawGraph(sinks)

  val sim = Simulation(sinks, SimpleScheduler)
  sim.run(traj)

}
 */
 */
 */
