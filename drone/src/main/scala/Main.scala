package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._

object Main extends App {

  def drone() = {

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

    //We cache it to showcase Cache that avoids recomputing points each time
    val clock  = TrajectoryClock(dt) //.cache()
    val points = clock.map(TrajectoryPointPulse)

    //******* Filter ********
    //Rate at which sensor geenerate data

    //The NormalVector through the ComplimentaryFilter

    //filter parameter
    val cov   = DenseMatrix.eye[Real](3) * 1.0
    val alpha = 0.95
    val initQ = Quaternion(1.0, 0, 0, 0)

//    val clock2     = clock.divider(divider)
//    val clockNoise = clock.latencyVariance(0.01)

//  val imu = clock.map(
//    IMU(Accelerometer(cov), Gyroscope(cov, dt)))
    val accelerometer = clock.map(Accelerometer(cov))
    val gyroscope     = clock.map(Gyroscope(cov, dt))
    val controlInput  = clock.map(ControlInput(1))

    lazy val cf: Source[Timestamped[Quaternion[Real]], Trajectory] =
      OrientationComplementaryFilter(accelerometer,
                                     gyroscope,
                                     controlInput,
                                     initQ,
                                     alpha,
                                     dt)

    val lpf = LowPassFilter(cf, Timestamped(initQ), 0.2)

    val qs =
      points.mapT((x: TrajectoryPoint) => x.q, "toQ") //.cache()

    var sinks: Seq[Sink[Trajectory]] = Seq()

    def awt() = {
      val vis = new Jzy3dVisualisation(points, KeypointSource)
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

      val plot = Plot2(lpf, qs)

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

    val sim = Simulation(sinks, SimpleScheduler)
    sim.run(traj)
  }

  def filter() = {

    val alpha = 0.5

    val clock = Clock(0.1).stop(1)
    val ts    = clock.map(Timestamp(1000))
    val sinus = ts.mapT(x => Quaternion(1.0, 0, 0, 0), "Sinus")
    val filt =
      LowPassFilter(sinus, Timestamped(0, Quaternion(1.0, 0, 0, 0), 0), alpha)

    val plot = Plot(filt)

    val sinks = Seq(plot)

    val sim = Simulation(sinks)
    sim.run(null)

    Sourcable.drawGraph(sinks)

  }

  def trans() = {

    val clock = Clock(0.1).stop(1)
    val ts    = clock.map(Timestamp(1000))
    val quat = ts.mapT(
      x => Quaternion(sin(x), cos(x), cos(x), sin(x)).normalized,
      "Sinus")
    val q = Quaternion(1.0, 0, 0, 0)
    val retrieved = quat.mapT(
      x =>
        TQuaternion
          .localAngleToLocalQuat(q, TQuaternion.quatToAxisAngle(q, x))
          .rotateBy(q),
      "Retrieve")

    val plot = Plot2(quat, retrieved)

//    val json = JsonExport(filt)
//    val print = PrintSink(json)

    val sinks = Seq(plot) //, print)

    val sim = Simulation(sinks)
    sim.run(null)

    Sourcable.drawGraph(sinks)

  }

  //  filter()
  // trans()
  drone()

}
