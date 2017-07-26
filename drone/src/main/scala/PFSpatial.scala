package dawn.flow.spatial

import breeze.linalg.{DenseMatrix, inv, det}
import spatial.dsl._
import dawn.flow.spatialf._
import dawn.flow.trajectory._
import dawn.flow._
import org.virtualized._

case class PFSpatial(s1: Source[(Acceleration, Omega)], s2: Source[(Position, Quat)])
    extends SpatialBatchRaw2[(Acceleration, Omega), (Position, Quat), (Position, Quat), SIMU, SVicon, SPOSE](s1, s2) {

  def spatial() = ???
}
