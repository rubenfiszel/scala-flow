package dawn.flow

trait Vec[A] {
  def scale(x: A, y: Double): A
  def plus(x: A, y: A): A
  def minus(x: A, y: A): A
}
