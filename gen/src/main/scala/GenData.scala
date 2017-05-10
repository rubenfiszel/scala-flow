package spatial.fusion.gen

import breeze.linalg._
import quad._
import breeze.stats.distributions._

object GenData extends App {

  val dt = 0.005

  val init = Init(Vec3.zero, Vec3.zero, Vec3.zero)
  var keypoints = List[Keypoint]()
  var tfs = List[Time]()

  keypoints ::= Keypoint(Some(Vec3(0, 0.75, 1.0)), Some(Vec3.one), Some(Vec3.zero))
  tfs ::= 1.0

  keypoints ::= Keypoint(Some(Vec3(1, 1, 0.8)), None, None)
  tfs ::= 1.0  

  keypoints ::= Keypoint(Some(Vec3(-1, -1, 1.0)), Some(Vec3.zero), None)
  tfs ::= 2.0
  
  
  keypoints = keypoints.reverse
  tfs = tfs.reverse

  val traj = QuadTrajectory(init, keypoints, tfs)

  traj.warn()
 
//  val viconCov = DenseMatrix.eye[Real](3) * 0.1
//  val vicon = Vicon(viconCov)

  val sim = Simulation(traj, Seq())//Seq((0.1, vicon)))
  val (keypointsS, points) = sim.simulate(dt)

  def awt() = {
    val vis = new AWTVisualisation(points, keypointsS)
    vis.start()
  }

  def json() = {
    val json = JsonExport(points)
    val print = PrintSink(json)
    print.consumeAll()
  }

  def figure() {

    val dt = 0.3

    //The NormalVector through the ComplimentaryFilter
    val cov = DenseMatrix.eye[Real](3)
    val imu = SensorPulse(ClockStop(Clock(dt), traj.tf), traj, IMU(Accelerometer(cov), Gyroscope(cov, dt)))
    val cf = ComplimentaryFilter(imu, 0.9, dt)


    //The actual normal vector
    val functorNV = TimestampFunctor(points, (x: TrajectoryPoint) => x.nv)    
//    def reduce[A](x: Timestamped[A], y: Timestamped[A]) = y
//    val r = Reduce(Buffer(Clock(dt), functorNV), reduce[Vec3])
    val r = functorNV

    //The two plots
    PlotData.createFigure(cf)     
    PlotData.createFigure(r)

  }

  figure()
  awt()
  json()

}
