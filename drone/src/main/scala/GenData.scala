package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._


object GenData extends App {

  //****** Model ******
  val dt = 0.005

  val init      = Init(Vec3.zero, Vec3.zero, Vec3.zero)
  var keypoints = List[Keypoint]()
  var tfs       = List[Time]()

  //Keypoint 1
  keypoints ::= Keypoint(Some(Vec3(0, 0.75, 1.0)),
                         Some(Vec3.one),
                         Some(Vec3.zero))
  tfs ::= 1.0

  //Keypoint 2
  keypoints ::= Keypoint(Some(Vec3(1, 1, 0.8)), None, None)
  tfs ::= 1.0

  //Keypoint 3
  keypoints ::= Keypoint(Some(Vec3(-1, -1, 1.0)), Some(Vec3.zero), None)
  tfs ::= 2.0

  //Reverse them to get the right order
  keypoints = keypoints.reverse
  tfs = tfs.reverse

  val traj = QuadTrajectory(init, keypoints, tfs)

  //Warn you if trajectory is impossible
  traj.warn()

  val keypointsS = KeypointSource
  //We cache it to showcase Cache that avoids recomputing points each time
  val clock1 = TrajectoryClock(dt).cache()
  val trajPP = clock1.map(TrajectoryPointPulse)
  val points = trajPP

  //******* Filter ********
  //Rate at which sensor geenerate data

  val divider = 1
  //The NormalVector through the ComplimentaryFilter

  //filter parameter
  val cov   = DenseMatrix.eye[Real](3) * 0.000001
  val alpha = 0.05

  val clock2     = clock1//.divider(divider)
  val clockNoise = clock2//.latencyVariance(0.01)
  val imu        = clockNoise.map(IMU(Accelerometer(cov), Gyroscope(cov, dt * divider)))//.latency(0.05)

  lazy val cf: Source[Timestamped[Quaternion[Real]], Trajectory] = OrientationComplimentaryFilterBuffered(imu, buffered, alpha, dt * divider).cache()//.latency(0.0)
  lazy val buffered = Buffer(() => cf, Timestamped(0.0, Quaternion(1.0, 0, 0, 0)))
//    OrientationComplimentaryFilter(imu, alpha, dt * divider).cache()//.latency(0.0)


  val qs = points.mapT(NamedFunction1((x: TrajectoryPoint) => x.q, "toQ")).cache()

//    val datas = sensors.map { case (ts, s) => s.generate(traj, ts, tf, seed) }
//(keypoints, points, datas)

  var sinks: Seq[Sink[Trajectory]] = Seq()

  def awt() = {
    val vis = new Jzy3dVisualisation(points, keypointsS)
    sinks ++= Seq(vis)
  }

  def printJson() = {
    //Map to json
//    val jsonP  = JsonExport(points)
    val jsonCF = JsonExport(cf)
    //Print json
//    val printP  = PrintSink(jsonP)
    val printCF = PrintSink(jsonCF)

    sinks ++= Seq(printCF)
  }

  def figure() = {

    val plot = Plot2(cf, qs)

    sinks ++= Seq(plot)
  }

  def testTS() = {
    val test = TestTS(cf, qs, 1000)
    sinks ++= Seq(test)
  }

  testTS()  
  figure()

//  awt()
//  printJson()

  Sourcable.drawGraph(sinks)

  val sim = Simulation(traj, sinks, SimpleScheduler)
  sim.run()


}
