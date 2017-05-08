package spatial.fusion.gen

object GenData extends App {

  val traj = Trajectory(Init(Vec3.zero, Vec3(4, -2, 3), Vec3.zero), tf = 1.0)
  val sim = Simulation(traj, Seq())
  val vis = new AWTVisualisation(sim)
  vis.start()

}
