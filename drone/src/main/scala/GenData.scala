package dawn.flow.drone

import dawn.flow._
import breeze.linalg._
import breeze.stats.distributions._

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
  val points = Cache(TrajectoryPointPulse(TrajectoryClock(dt)))

  //******* Filter ********
  //Rate at which sensor geenerate data
  val fdt = 0.3

  //The NormalVector through the ComplimentaryFilter

  //filter parameter
  val cov   = DenseMatrix.eye[Real](3)
  val alpha = 0.9

  val imu = SensorPulse(TrajectoryClock(fdt),
                        IMU(Accelerometer(cov), Gyroscope(cov, fdt)))
  val cf = FilterApply(imu, ComplimentaryFilter(alpha, fdt))

//    val datas = sensors.map { case (ts, s) => s.generate(traj, ts, tf, seed) }
//(keypoints, points, datas)

  var sinks: Seq[Sink[Trajectory]] = Seq()

  def awt() = {
    val vis = new Jzy3dTrajectoryVisualisation(points, keypointsS)
    sinks ++= Seq(vis)
  }

  def json() = {
    //Map to json
    val jsonP  = JsonExport(points)
    val jsonCF = JsonExport(cf)
    //Print json
    val printP  = PrintSink(jsonP)
    val printCF = PrintSink(jsonCF)

    sinks ++= Seq(printP, printCF)
  }

  def figure() = {
    //The actual normal vector
    val functorNV = TimestampFunctor(points, (x: TrajectoryPoint) => x.nv)
    //    This is to buff data every 0.3 sec
    //    def reduce[A](x: Timestamped[A], y: Timestamped[A]) = y
    //    val r = Reduce(Buffer(Clock(dt), functorNV), reduce[Vec3])

    //Those are the "true values", get the normal vector from the trajectory point
    val r = functorNV

    //The two plots
    val f1 = Plot(cf)
    val f2 = Plot(r)

    sinks ++= Seq(f1, f2)
  }

  figure()
  awt()
  json()

  val sim = Simulation(traj, sinks, SimpleScheduler)
  sim.run()
}
