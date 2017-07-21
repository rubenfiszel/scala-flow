package dawn.flow.spatial

import breeze.linalg.{DenseMatrix, inv, det}
import spatial.dsl._
import dawn.flow.spatialf._
import dawn.flow._
import org.virtualized._

case class PFSpatial(s1: Source[(Acceleration, Omega)], s2: Source[(Position, Quat)])
    extends SpatialBatch2[(Acceleration, Omega), (Position, Quat), (Position, Quat), SIMU, SVicon, SPOSE](s1, s2) {

  val N: scala.Int = 10

  @struct case class Particle(w: SReal, q: SQuat)

  override def initMems() = {
    Map(
      ("particles", SRAM[Particle](N))
    )
  }

  def spatial(ts: Either[TSA, TSB]) = {

    val particles = mems[SRAM1[Particle]]("particles")

    particles(0) = Particle(math.log(1.0/N), SQuat(1, 0, 0, 0))

    SVicon(SVec3(0, 0, 0), particles(0).q)

  }

}
