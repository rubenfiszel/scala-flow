package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._
import spire.math.Quaternion

object DroneParticle extends FlowApp[Trajectory] {

  //****** Model ******

  val dt = 0.005

  //filter parameter
  val cov = DenseMatrix.eye[Real](3) * 0.1
  val covQ = DenseMatrix.eye[Real](4) * 1.0
  val initQ = Quaternion(1.0, 0, 0, 0)


  val clock = TrajectoryClock(dt)
  val clock2 = TrajectoryClock(dt + 0.001)  
  val points = clock.map(
    LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)),
    "toPoints")
  
  val accelerometer = clock2.map(Accelerometer(cov)) //.latency(dt / 2)
  val gyroscope = clock.map(Gyroscope(cov, dt))
  val controlInput = clock.map(ControlInput(1, cov, dt))
  val vicon = clock.map(Vicon(cov, covQ))

  /* batch example
  val batch = Batch(accelerometer, (x: ListT[Acceleration]) => x.map(_*2), "*2")
  Plot2(batch, accelerometer)
   */

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

  def figure() = {
    Plot2(filter, qs)
  }

  def testTS() = {
    TestTS(filter, qs, 1000)
  }

  figure()
  testTS()
//  awt()
//  filter.println

  val traj = TrajFactory.generate()

  run(traj)
  drawExpandedGraph()
  System.exit(0)

}
