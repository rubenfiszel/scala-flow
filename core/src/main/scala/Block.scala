package dawn.flow

sealed trait Block[A] extends Source[A] { parent =>

  override def scheduler = out.scheduler
  
  def out: Source[A]

  val trans = new Op1[A, A] {
    def rawSource1 = out
    def listen1(x: Timestamped[A]) =
      parent.broadcast(x)
    def name = "Block out"
  }
}

trait Block0[A] extends Source0 with Block[A]

trait Block1[A, B] extends Source1[A] with Block[B] {
  def listen1(x: Timestamped[A]) = ()
}

trait Block2[A, B, C] extends Source2[A, B] with Block[C] {
  def listen1(x: Timestamped[A]) = ()
  def listen2(x: Timestamped[B]) = ()
}

trait Block3[A, B, C, D] extends Source3[A, B, C] with Block[D] {
  def listen1(x: Timestamped[A]) = ()
  def listen2(x: Timestamped[B]) = ()
  def listen3(x: Timestamped[C]) = ()
}

trait Block4[A, B, C, D, E] extends Source4[A, B, C, D] with Block[E] {
  def listen1(x: Timestamped[A]) = ()
  def listen2(x: Timestamped[B]) = ()
  def listen3(x: Timestamped[C]) = ()
  def listen4(x: Timestamped[D]) = ()
}

trait Block5[A, B, C, D, E, F] extends Source5[A, B, C, D, E] with Block[F] {
  def listen1(x: Timestamped[A]) = ()
  def listen2(x: Timestamped[B]) = ()
  def listen3(x: Timestamped[C]) = ()
  def listen4(x: Timestamped[D]) = ()
  def listen5(x: Timestamped[E]) = ()  
}

trait Block6[A, B, C, D, E, F, G] extends Source6[A, B, C, D, E, F] with Block[G] {
  def listen1(x: Timestamped[A]) = ()
  def listen2(x: Timestamped[B]) = ()
  def listen3(x: Timestamped[C]) = ()
  def listen4(x: Timestamped[D]) = ()
  def listen5(x: Timestamped[E]) = ()
  def listen6(x: Timestamped[F]) = ()    
}
