package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._
import spire.math.Quaternion

object DroneParticle extends FlowApp[Trajectory] {

  //****** Model ******

  val dtIMU   = 0.001
  val dtCI    = dtIMU
  val dtVicon = (dtIMU * 500)

  val covScale = 10.0
  //filter parameter
  val covAcc    = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covGyro   = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covViconP = DenseMatrix.eye[Real](3) * (0.000001 * covScale)
  val covViconQ = DenseMatrix.eye[Real](3) * (0.000001 * covScale)

  val stdCIThrust = 0.1 * covScale
  val covCIOmega  = DenseMatrix.eye[Real](3) * (0.1 * covScale)

  val numberParticles = 10

  val initQ = Quaternion(1.0, 0, 0, 0)

  val clockIMU   = new TrajectoryClock(dtIMU)
  val clockCI    = new TrajectoryClock(dtCI).latency(dtIMU/100.0)
  val clockVicon = new TrajectoryClock(dtVicon)
 

  val points = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)), "toPoints")

  val imu      = IMU(clockIMU, covAcc, covGyro, dtIMU)
  val controlInputThrust = clockCI.map(ControlInputThrust(stdCIThrust, dtCI))
  val controlInputOmega  = clockCI.map(ControlInputOmega(covCIOmega, dtCI))
  val vicon              = clockVicon.map(Vicon(covViconP, covViconQ))

  /* batch example
  val batch = Batch(accelerometer, (x: ListT[Acceleration]) => x.map(_*2), "*2")
  Plot2(batch, accelerometer)
   */

//  /*
  val filter =
    AugmentedComplementaryFilter(
      imu,
      vicon,
      initQ,
      0.95
    )
//   */

 /*  
  val filter =
    ParticleFilter(accelerometer,
                   gyroscope,
                   vicon,
                   controlInputThrust,
                   controlInputOmega,
                   initQ,
                   numberParticles,
                   covAcc,
                   covGyro,
                   covViconP,
                   covViconQ,
                   stdCIThrust,
                   covCIOmega)
*/   
  val pqs =
    points
      .map(x => (x.p, x.q), "toPandQ")

  def awt() = {
    new Jzy3dVisualisation(points, traj.getKeypoints)
  }

  def figure() = {
    Plot2(filter, pqs)
  }

  def testTS() = {
    TestTS(filter, pqs, 1000)
  }

  val traj = TrajFactory.generate()

  figure()
  testTS()
//  awt()
//  filter.println

  run(traj)
  //  drawExpandedGraph()
  System.exit(0)

}
