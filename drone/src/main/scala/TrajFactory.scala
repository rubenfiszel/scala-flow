package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions.{Rand => _, _ => _}
import spire.math.{Real => _, _ => _}
import spire.implicits._

object TrajFactory {

  def generate(n: Int): Stream[Trajectory] =
    if (n <= 0)
      Stream.empty
    else
      generateOne() #:: generate(n - 1)

  def genKeypoints(): (Init, List[Keypoint], List[Time]) = {
    val init      = Init(Vec3.zero, Vec3.zero, Vec3.zero)
    var keypoints = List[Keypoint]()
    var tfs       = List[Time]()

    val covA = eye(3) * 0.1
    val covV = eye(3) * 0.2
    val covP = eye(3) * 0.6

    for (i <- 1 to 5) {
      //Keypoint 1
      keypoints ::= Keypoint(Some(Rand.gaussian(Vec3(0, 0.75, 1.0), covP)),
                             Some(Rand.gaussian(Vec3.one, covV)),
                             Some(Rand.gaussian(Vec3.zero, covA)))
      tfs ::= 1.0

      //Keypoint 2
      keypoints ::= Keypoint(Some(Rand.gaussian(Vec3(1, 1, 0.8), covP)), None, None)
      tfs ::= 1.0

      //Keypoint 3
      keypoints ::= Keypoint(Some(Rand.gaussian(Vec3(-1, -1, 1.0), covP)), Some(Rand.gaussian(Vec3.zero, covV)), None)
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
      val traj                   = QuadTrajectory(init, keypoints, tfs)
      traj
    }

    var r: Option[Trajectory] = None
    var attempts              = 0
    while (r.isEmpty || !r.get.isFeasible) {
      r = Some(loop())
      if (!r.get.isFeasible)
        attempts += 1
    }
    println(attempts + " attempts were required to generate a valid random trajectory!")
    r.get

  }

}
