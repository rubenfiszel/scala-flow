package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._

object TrajFactory {

  def generate(n: Int): Stream[Trajectory] =
    if (n <= 0)
      Stream.empty
    else 
      generateOne() #:: generate(n-1)

  def genKeypoints(): (Init, List[Keypoint], List[Time]) = {
    val init = Init(Vec3.zero, Vec3.zero, Vec3.zero)
    var keypoints = List[Keypoint]()
    var tfs = List[Time]()

    for (i <- 1 to 5) {
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
    }

    //Reverse them to get the right order
    keypoints = keypoints.reverse
    tfs = tfs.reverse
    
    (init, keypoints, tfs)
  }

  def generateOne() = {

    def loop() = {
      val (init, keypoints, tfs) = genKeypoints()
      val traj = QuadTrajectory(init, keypoints, tfs)
      traj
    }

    var r: Option[Trajectory] = None
    while (r.isEmpty || !r.get.isFeasible) {
      println("loop")
      r = Some(loop())
    }
    r.get

  }

}
