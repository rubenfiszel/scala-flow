package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._

/*
object DroneParticle extends App {

  //****** Model ******
  val dt = Config.dt

  val traj = TrajFactory.generate()

  implicit val mc = ModelCB[Trajectory]

  val clock = TrajectoryClock(dt)
  val points = clock.map(LambdaWithModel((t: Time, traj: Trajectory) =>
                           Timestamped(t, traj.getPoint(t))),
                         "toPoints")

  //filter parameter
  val cov   = Config.cov
  val covQ  = Config.covQ
  val initQ = Config.initQ

  val accelerometer = clock.map(Accelerometer(cov)).latency(dt / 2)
  val gyroscope     = clock.map(Gyroscope(cov, dt))
  val controlInput  = clock.map(ControlInput(1, cov, dt))
  val vicon         = clock.map(Vicon(cov, covQ))

  lazy val filter: SourceT[Quat] =
    ParticleFilter(accelerometer,
                   gyroscope,
                   controlInput,
                   vicon,
                   initQ,
                   dt,
                   400,
                   cov)

  val qs =
    points.mapT((x: TrajectoryPoint) => x.q, "toQ") //.cache()

  var sinks: Seq[Sink] = Seq()

  def awt() = {
    val vis = new Jzy3dVisualisation(points, KeypointSource())
    sinks ++= Seq(vis)
  }

  def printJson() = {
//    val jsonFilter  = JsonExport(filter)
//    val printFilter = PrintSink(jsonFilter)
//    sinks ++= Seq(printFilter)
  }

  def figure() = {
    val plot = Plot2(filter, qs)
    sinks ++= Seq(plot)
  }

  def testTS() = {
    val test = TestTS(filter, qs, 1000)
    sinks ++= Seq(test)
  }

  testTS()
  figure()

//  awt()
  printJson()

  Sourcable.drawGraph(sinks)

  val sim = Simulation(sinks, SimpleScheduler)
  sim.run(traj)

}

*/
*/
