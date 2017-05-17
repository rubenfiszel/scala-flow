package dawn.flow

import io.circe.generic.semiauto._
import io.circe._
import spire.math.{Real => _, _ => _}
import spire.implicits._

package object trajectory {
  implicit val encodeQuaternion: Encoder[Quaternion[Real]] = deriveEncoder
  implicit val decodeQuaternion: Decoder[Quaternion[Real]] = deriveDecoder

  implicit object QuaternionData extends Data[Quaternion[Real]]{
    def toValues(x: Quaternion[Real]) = Seq(x.r, x.i, x.j, x.k)
  }

  implicit class QuaternionOps(q: Quaternion[Real]) {
    def normalized = {
      val normQ = sqrt(q.r ** 2 + q.i ** 2 + q.j ** 2 + q.k ** 2)
      q/normQ
    }

    //https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation
    def rotate(v: Vec3) = {
      val qga = Quaternion(0.0, v.x, v.y, v.z)
      val rq  = q * qga * q.conjugate
      Vec3(rq.i, rq.j, rq.k)
    }

    def rotateBy(q2: Quaternion[Real]) =
      q2*q
    
  }
  
}
