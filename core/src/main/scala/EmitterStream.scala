package dawn.flow

trait EmitterStream[A] extends Source[A] with Source0 {
  def stream(): Stream[(Time, A)]
  var iterator: Iterator[(Time, A)] = _

  def registerNext(): Unit = {
    if (iterator.hasNext && !closed) {
      val (t, a) = iterator.next
      broadcast(Timestamped(t, a))
      scheduler.registerEvent(registerNext(), t)
    }
  }


  override def setup() = {
    super.setup()
    iterator = stream().toIterator
    scheduler.executeAtStart(registerNext())
  }
}
