package dawn.flow

import io.circe.generic.semiauto._
import io.circe._
import spire.math.{Real => _, _ => _}
import spire.implicits._
import breeze.linalg.DenseVector

package object trajectory {

  implicit class DenseVectorOps(v: VectorR) {
    def toQuaternion = Quaternion(v(0), v(1), v(2), v(3))
  }

  implicit class QuaternionOps(q: Quaternion[Real]) {
    def normalized = {
      val normQ = sqrt(q.r ** 2 + q.i ** 2 + q.j ** 2 + q.k ** 2)
      q / normQ
    }

    //https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation
    def rotate(v: Vec3) = {
      val qga = Quaternion(0.0, v.x, v.y, v.z)
      val rq = q * qga * q.reciprocal
      Vec3(rq.i, rq.j, rq.k)
    }

    def rotateBy(q2: Quaternion[Real]) =
      q2 * q

    def toDenseVector =
      DenseVector(q.r, q.i, q.j, q.k)

  }

}
