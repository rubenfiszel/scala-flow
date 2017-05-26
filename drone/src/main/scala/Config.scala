package dawn.flow.trajectory

import dawn.flow._

import breeze.linalg._
import breeze.stats.distributions._
import spire.math.{Real => _, _ => _}
import spire.implicits._

object Config {

  val dt = 0.005

  val cov   = DenseMatrix.eye[Real](3) * 1.0
  val covQ = DenseMatrix.eye[Real](4) * 1.0
  val initQ = Quaternion(1.0, 0, 0, 0)

}
