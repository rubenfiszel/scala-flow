package dawn.flow

trait EmitterStream[A] extends Source[A] with Source0 {
  def stream(): Stream[(Time, A)]
  var iterator: Iterator[(Time, A)] = _

  def registerNext(): Unit = {
    if (iterator.hasNext && !closed) {
      val (t, a) = iterator.next
      scheduler.registerEvent(broadcast(Timestamped(t, a)), t)
      scheduler.registerEvent(registerNext(), t)
    }
  }

  override def reset() = {
    super.reset()
    iterator = stream().toIterator
  }
  
  override def setup() = {
    super.setup()
    registerNext()
  }
}
