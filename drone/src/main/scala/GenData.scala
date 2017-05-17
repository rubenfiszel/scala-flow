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
  val clock1 = TrajectoryClock(dt)
  val trajPP = clock1.map(TrajectoryPointPulse)
  val points = trajPP //.cache()

  //******* Filter ********
  //Rate at which sensor geenerate data

  val divider = 1
  //The NormalVector through the ComplimentaryFilter

  //filter parameter
  val cov   = DenseMatrix.eye[Real](3) * 0.000001
  val alpha = 0.05

  val clock2     = clock1 //.divider(divider)
  val clockNoise = ClockVar(clock2, 0.001)
  val imu        = clock2.map(IMU(Accelerometer(cov), Gyroscope(cov, dt * divider)))
  val cf =
    imu.map(OrientationComplimentaryFilter(alpha, dt * divider)).latency(0.0)

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
    //The actual normal vector
    val nvs = points.mapT(NamedFunction1((x: TrajectoryPoint) => x.q, "toNV"))
    //    This is to buff data every 0.3 sec
//    def red[A](x: Timestamped[A], y: Timestamped[A]) = y

    //Those are the "true values", get the normal vector from the trajectory point
    val r = nvs //.buffer(Clock(dt)).reduceF(red[Vec3] _)

    //The two plots
    val plot = Plot2(r, cf)

    sinks ++= Seq(plot)
  }

  figure()
//  awt()
//  printJson()

  val sim = Simulation(traj, sinks, SimpleScheduler)
  sim.run()

//  println(Sourcable.graph(sinks))
}
