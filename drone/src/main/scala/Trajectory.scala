package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._

object TrajFactory {

  def generate() = {
    val init = Init(Vec3.zero, Vec3.zero, Vec3.zero)
    var keypoints = List[Keypoint]()
    var tfs = List[Time]()

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
    traj
  }

}
