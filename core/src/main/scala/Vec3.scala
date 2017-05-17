package dawn.flow

import io.circe.generic.JsonCodec
import scala.collection.GenSeq
import breeze.linalg._

@JsonCodec
case class Vec3(x: Real, y: Real, z: Real)
    extends DenseVector[Real](Array(x, y, z))

object Vec3 {
  def zero = Vec3(0, 0, 0)
  def one  = Vec3(1, 1, 1)

  def apply(gs: Vector[Real]): Vec3 =
    apply(gs.toArray)

  def apply(gs: GenSeq[Real]): Vec3 = {
    require(gs.length == 3)
    Vec3(gs(0), gs(1), gs(2))
  }
}

