package spatial.fusion.gen

import breeze.linalg._
import quad._

object GenData extends App {

  val dt = 0.005
  val init = Init(Vec3.zero, Vec3.zero, Vec3.zero)
  var keypoints = List[Keypoint]()
  var tfs = List[Time]()

  keypoints ::= Keypoint(Some(Vec3(0, 0.5, 0.5)), Some(Vec3.one), Some(Vec3.zero))
  tfs ::= 1.0

  keypoints ::= Keypoint(Some(Vec3.one), Some(Vec3.zero), None)
  tfs ::= 1.0  

  
  keypoints = keypoints.reverse
  tfs = tfs.reverse

  val traj = QuadTrajectory(init, keypoints, tfs)

  traj.warn()
 
  val viconCov = DenseMatrix.eye[Real](3) * 0.1
  val vicon = Vicon(viconCov)
  val sim = Simulation(traj, Seq((0.1, vicon)))
  val (keypointsS, points) = sim.simulate(dt, 12345)

  def awt() = {
    val vis = new AWTVisualisation(points, keypointsS)
    vis.start()
  }

  def json() = {
    val json = JsonExport(points)
    val print = PrintSink(json)
    print.consumeAll()
  }

  awt()
  //
//  json()

}
