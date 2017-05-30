package dawn.flow

class NamedFunction[A, B](f: A => B, name: String, rqModel: Boolean = false)
    extends (A => B) {
  override def toString = name
  def apply(x: A)       = f(x)
  def requireModel      = rqModel
}
