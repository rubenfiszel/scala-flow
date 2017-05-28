package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._


object DroneParticle extends App {

  //****** Model ******
  val dt = Config.dt

  val traj = TrajFactory.generate()

  implicit val setModel = ModelCB[Trajectory]

  val clock = TrajectoryClock(dt)
  val points = clock.map(LambdaWithModel((t: Time, traj: Trajectory) =>
                           Timestamped(t, traj.getPoint(t))),
                         "toPoints")

  //filter parameter
  val cov   = Config.cov
  val covQ  = Config.covQ
  val initQ = Config.initQ

  
  val accelerometer = clock.map(Accelerometer(cov)).latency(dt/2)
  val gyroscope     = clock.map(Gyroscope(cov, dt))
  val controlInput  = clock.map(ControlInput(1, cov, dt))
  val vicon         = clock.map(Vicon(cov, covQ))

  /* batch example
  val batch = new Batch[Acceleration, Acceleration] {
    def name = "id batch"
    def source1 = accelerometer
    def f(x: ListT[Acceleration]) = x
  }


  
  val replay = Replay(accelerometer, batch.sh)
  Plot2(batch, replay)
  */

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


  def awt() = {
    new Jzy3dVisualisation(points, traj.getKeypoints)
  }

  def printJson() = {
//    val jsonFilter  = JsonExport(filter)
    PrintSink(filter)
  }

  def figure() = {
    Plot2(filter, qs)
  }

  def testTS() = {
    TestTS(filter, qs, 1000)
  }

  testTS()
  figure()

//  awt()
//  printJson()

  setModel(traj)

  PrimarySourcableHook.drawGraph()
  PrimaryScheduler.run()



}



