package spatial.fusion.gen

object GenData extends App {

  val init = Init(Vec3.zero, Vec3.zero, Vec3.zero)
  var keypoints = List[Keypoint]()
  var tfs = List[Time]()

  keypoints ::= Keypoint(Some(Vec3(0, 0.5, 0.5)), Some(Vec3.one), Some(Vec3.zero))
  tfs ::= 1.0

  keypoints ::= Keypoint(Some(Vec3.one), Some(Vec3.zero), None)
  tfs ::= 1.0  

  
  keypoints = keypoints.reverse
  tfs = tfs.reverse

  val traj = Trajectory(init, keypoints, tfs)

  traj.warn()

  val sim = Simulation(traj, Seq((0.1, Accelerometer(null))))

  def awt() = {
    val vis = new AWTVisualisation(sim)
    vis.start()
  }
  def json() = {
    val json = JsonExport.export(sim)
    println(json)
  }

  awt()
//  json()

}
