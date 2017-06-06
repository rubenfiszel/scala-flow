package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._
import spire.math.Quaternion

object DroneParticle extends FlowApp[Trajectory] {

  //****** Model ******

  val dt = 0.005

  val covScale = 1.0
  //filter parameter
  val covAcc    = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covGyro   = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covViconP = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covViconQ = DenseMatrix.eye[Real](4) * (1.0 * covScale)

  val stdCIThrust = 0.1 * covScale
  val covCIOmega  = DenseMatrix.eye[Real](3) * (0.1 * covScale)

  val numberParticles = 10

  val initQ = Quaternion(1.0, 0, 0, 0)

  val clock  = TrajectoryClock(dt)
  val clock2 = TrajectoryClock(dt + 0.001)
  val points = clock.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)), "toPoints")

  val accelerometer = clock2.map(Accelerometer(covAcc)) //.latency(dt / 2)
  val gyroscope     = clock.map(Gyroscope(covGyro, dt))
  val controlInput  = clock.map(ControlInput(stdCIThrust, covCIOmega, dt))
  val vicon         = clock.map(Vicon(covViconP, covViconQ))

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
                   numberParticles,
                   covAcc,
                   covGyro,
                   covViconP,
                   covViconQ,
                   stdCIThrust,
                   covCIOmega)

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

  val traj = TrajFactory.generate()

  figure()
  testTS()
  awt()
//  filter.println


  run(traj)
  drawExpandedGraph()
  //System.exit(0)

}
