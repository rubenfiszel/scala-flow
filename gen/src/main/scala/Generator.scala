package spatial.fusion.gen

trait Data {
  def timestamp: Long
  def values: Seq[Float]
  override def toString =
    s"""$getClass.getName $timestamp ${values.mkString(" ")}"""
}

trait Generator {
  def generate(u: Universe, t: Time, dt: Timestep): Seq[Data]
}
