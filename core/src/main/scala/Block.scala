package dawn.flow

trait Block[A] { parent: Source[A] =>
  def out: Source[A]

  val trans = new Op1[A, A] {
    def rawSource1 = out
    def listen1(x: Timestamped[A]) =
      parent.broadcast(x)
    def name = "Op Block"
  }
}

trait Block1[A, B] extends Source[B] with Source1[A] with Block[B] {
  def listen1(x: Timestamped[A]) = ()
}
trait Block2[A, B, C] extends Source[C] with Source2[A, B] with Block[C] {
  def listen1(x: Timestamped[A]) = ()
  def listen2(x: Timestamped[B]) = ()
}
trait Block3[A, B, C, D]
    extends Source[D]
    with Source3[A, B, C]
    with Block[D] {
  def listen1(x: Timestamped[A]) = ()
  def listen2(x: Timestamped[B]) = ()
  def listen3(x: Timestamped[C]) = ()
}
trait Block4[A, B, C, D, E]
    extends Source[E]
    with Source4[A, B, C, D]
    with Block[E] {
  def listen1(x: Timestamped[A]) = ()
  def listen2(x: Timestamped[B]) = ()
  def listen3(x: Timestamped[C]) = ()
  def listen4(x: Timestamped[D]) = ()
}
