package dawn.flow.trajectory

import dawn.flow._

object DroneParticle extends App {

  //****** Model ******
  val dt = Config.dt

  val traj = TrajFactory.generate()

  implicit val modelHook = ModelHook[Trajectory]

  val clock = TrajectoryClock(dt)
  val points = clock.map(
    LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)),
    "toPoints")

  //filter parameter
  val cov = Config.cov
  val covQ = Config.covQ
  val initQ = Config.initQ

  val accelerometer = clock.map(Accelerometer(cov)) //.latency(dt / 2)
  val gyroscope = clock.map(Gyroscope(cov, dt))
  val controlInput = clock.map(ControlInput(1, cov, dt))
  val vicon = clock.map(Vicon(cov, covQ))

//  /* batch example
  val batch = Batch(accelerometer, (x: ListT[Acceleration]) => x.map(_*2), "*2")


  Plot2(batch, accelerometer)
//   */

  val filter =
    ParticleFilter(accelerometer,
                   gyroscope,
                   controlInput,
                   vicon,
                   initQ,
                   dt,
                   10,
                   cov)

  val qs =
    points.map(_.q, "toQ")

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

//  testTS()
  figure()

//  awt()
//  printJson()

  modelHook(traj)

  PrimaryNodeHook.drawGraph()
  PrimaryScheduler.run()

  PrimaryScheduler.reset()
  PrimaryNodeHook.reset()  
  PrimaryScheduler.run()    

}
