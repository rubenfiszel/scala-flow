package dawn.flow

trait Data[A] {
  def toValues(x: A): Seq[Real]
}
