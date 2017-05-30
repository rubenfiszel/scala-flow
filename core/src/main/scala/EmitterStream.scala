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

  scheduler.executeBeforeStart({ iterator = stream().toIterator })
  scheduler.executeAtStart(registerNext())

  override def reset() = {
    super.reset()
    iterator = stream.toIterator
  }
}
