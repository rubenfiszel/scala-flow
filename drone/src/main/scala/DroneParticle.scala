package dawn.flow.trajectory

import dawn.flow._
import breeze.linalg._
import spire.math.Quaternion

object DroneParticle extends FlowApp[Trajectory] {

  //****** Model ******

  val dtIMU   = 0.1
  val dtCI    = dtIMU
  val dtVicon = dtIMU * 5

  val covScale = 0.1
  //filter parameter
  val covAcc    = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covGyro   = DenseMatrix.eye[Real](3) * (0.1 * covScale)
  val covViconP = DenseMatrix.eye[Real](3) * (0.01 * covScale)
  val covViconQ = DenseMatrix.eye[Real](3) * (0.01 * covScale)

  val stdCIThrust = 0.1 * covScale
  val covCIOmega  = DenseMatrix.eye[Real](3) * (0.1 * covScale)

  val numberParticles = 10

  val initQ = Quaternion(1.0, 0, 0, 0)

  val clockIMU   = new TrajectoryClock(dtIMU)
  val clockCI    = new TrajectoryClock(dtCI)
  val clockVicon = new TrajectoryClock(dtVicon)
 

  val points = clockIMU.map(LambdaWithModel((t: Time, traj: Trajectory) => traj.getPoint(t)), "toPoints")

  val accelerometer      = clockIMU.map(Accelerometer(covAcc)) //.latency(dt / 2)
  val gyroscope          = clockIMU.map(Gyroscope(covGyro, dtIMU))
  val controlInputThrust = clockCI.map(ControlInputThrust(stdCIThrust, dtCI))
  val controlInputOmega  = clockCI.map(ControlInputOmega(covCIOmega, dtCI))
  val vicon              = clockVicon.map(Vicon(covViconP, covViconQ))

  /* batch example
  val batch = Batch(accelerometer, (x: ListT[Acceleration]) => x.map(_*2), "*2")
  Plot2(batch, accelerometer)
   */

  val cfilter =
    AugmentedComplementaryFilter(
      accelerometer,
      gyroscope,
      vicon,
      controlInputThrust,
      controlInputOmega,
      initQ,
      0.98
    )



  /*
  val pfilter =
    ParticleFilter(accelerometer,
                   gyroscope,
                   vicon,
                   controlInputThrust,
                   controlInputOmega,
                   initQ,
                   dt,
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
    Plot2(cfilter, pqs)
  }

  def testTS() = {
    TestTS(cfilter, pqs, 1000)
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
